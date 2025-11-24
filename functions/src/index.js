/**
 * Firebase Cloud Functions for SSBMax Interview Assessment
 *
 * This function securely analyzes interview responses using Gemini API
 * without exposing the API key to the client app.
 */

// Load environment variables from .env file (for local development)
// In production, Firebase automatically loads environment variables
require('dotenv').config();

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { GoogleGenerativeAI } = require('@google/generative-ai');

// Initialize Firebase Admin
admin.initializeApp();
const db = admin.firestore();

// Initialize Gemini AI with API key from environment variables
// Set via .env file: GEMINI_API_KEY=your_key_here
const getGenAI = () => {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey === 'your_gemini_api_key_here') {
    throw new Error(
      'Gemini API key not configured.\n' +
      'For local development: Create functions/.env file with GEMINI_API_KEY=your_key\n' +
      'For production: Set in Firebase Console under Functions > Environment Variables'
    );
  }
  return new GoogleGenerativeAI(apiKey);
};

/**
 * Analyze Interview Response
 *
 * Callable function that:
 * 1. Verifies user authentication
 * 2. Fetches interview response from Firestore
 * 3. Calls Gemini API for OLQ assessment
 * 4. Returns analysis results
 *
 * @param {Object} data - Request data
 * @param {string} data.responseId - ID of the interview response to analyze
 * @param {string} data.sessionId - ID of the interview session
 * @param {Object} context - Function context with auth info
 * @returns {Object} Analysis results with OLQ scores
 */
exports.analyzeInterviewResponse = functions.https.onCall(async (data, context) => {
  // Security: Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to analyze responses'
    );
  }

  const userId = context.auth.uid;
  const { responseId, sessionId } = data;

  // Validate input
  if (!responseId || !sessionId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'responseId and sessionId are required'
    );
  }

  try {
    // Fetch interview response from Firestore
    const responseDoc = await db
      .collection('interview_responses')
      .doc(responseId)
      .get();

    if (!responseDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        `Interview response ${responseId} not found`
      );
    }

    const responseData = responseDoc.data();

    // Fetch session to verify ownership
    const sessionDoc = await db
      .collection('interview_sessions')
      .doc(sessionId)
      .get();

    if (!sessionDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        `Interview session ${sessionId} not found`
      );
    }

    const sessionData = sessionDoc.data();

    // Security: Verify the session belongs to the authenticated user
    if (sessionData.userId !== userId) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'You do not have permission to access this session'
      );
    }

    // Fetch the question for context
    const questionDoc = await db
      .collection('interview_questions')
      .doc(responseData.questionId)
      .get();

    const questionData = questionDoc.exists ? questionDoc.data() : null;
    const questionText = questionData?.questionText || responseData.questionText || 'Unknown question';
    const expectedOLQs = questionData?.expectedOLQs || responseData.expectedOLQs || [];

    // Call Gemini API for analysis
    const genAI = getGenAI();
    const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });

    const prompt = buildAnalysisPrompt(
      questionText,
      responseData.responseText,
      expectedOLQs,
      responseData.responseMode || 'text'
    );

    console.log(`Analyzing response ${responseId} for user ${userId}`);
    const result = await model.generateContent(prompt);
    const responseText = result.response.text();

    // Parse JSON response
    const analysis = parseAnalysisResponse(responseText);

    // Store analysis back to Firestore
    await responseDoc.ref.update({
      olqScores: analysis.olqScores,
      overallConfidence: analysis.overallConfidence,
      keyInsights: analysis.keyInsights,
      suggestedFollowUp: analysis.suggestedFollowUp || null,
      analyzedAt: admin.firestore.FieldValue.serverTimestamp(),
      analyzedBy: 'gemini-2.5-flash'
    });

    console.log(`Successfully analyzed response ${responseId}`);

    return {
      success: true,
      responseId: responseId,
      analysis: analysis
    };

  } catch (error) {
    console.error('Error analyzing response:', error);

    // Re-throw HttpsErrors as-is
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    // Wrap other errors
    throw new functions.https.HttpsError(
      'internal',
      `Failed to analyze response: ${error.message}`
    );
  }
});

/**
 * Analyze Interview Response (Inline)
 *
 * Callable function for real-time analysis during interview session.
 * Accepts question and response data inline without fetching from Firestore.
 *
 * @param {Object} data - Request data
 * @param {string} data.questionText - The interview question
 * @param {string} data.responseText - User's response
 * @param {Array<string>} data.expectedOLQs - Target OLQs for this question
 * @param {string} data.responseMode - Response mode (text/voice)
 * @param {Object} context - Function context with auth info
 * @returns {Object} Analysis results with OLQ scores
 */
exports.analyzeResponseInline = functions.https.onCall(async (data, context) => {
  // Security: Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to analyze responses'
    );
  }

  const userId = context.auth.uid;
  const { questionText, responseText, expectedOLQs, responseMode } = data;

  // Validate input
  if (!questionText || !responseText) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'questionText and responseText are required'
    );
  }

  try {
    // Call Gemini API for analysis
    const genAI = getGenAI();
    const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });

    const prompt = buildAnalysisPrompt(
      questionText,
      responseText,
      expectedOLQs || [],
      responseMode || 'text'
    );

    console.log(`Analyzing inline response for user ${userId}`);
    const result = await model.generateContent(prompt);
    const analysisText = result.response.text();

    // Parse JSON response
    const analysis = parseAnalysisResponse(analysisText);

    console.log(`Successfully analyzed inline response`);

    return {
      success: true,
      analysis: analysis
    };

  } catch (error) {
    console.error('Error analyzing inline response:', error);

    // Re-throw HttpsErrors as-is
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    // Wrap other errors
    throw new functions.https.HttpsError(
      'internal',
      `Failed to analyze response: ${error.message}`
    );
  }
});

/**
 * Generate Questions for Interview Session
 *
 * Callable function that generates personalized interview questions
 * based on user's PIQ submission
 *
 * @param {Object} data - Request data
 * @param {string} data.piqSubmissionId - ID of the PIQ submission
 * @param {number} data.questionCount - Number of questions to generate
 * @param {Object} context - Function context with auth info
 * @returns {Object} Generated questions
 */
exports.generateInterviewQuestions = functions.https.onCall(async (data, context) => {
  // Security: Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to generate questions'
    );
  }

  const userId = context.auth.uid;
  const { piqSubmissionId, questionCount = 4 } = data;

  if (!piqSubmissionId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'piqSubmissionId is required'
    );
  }

  try {
    // Fetch PIQ submission
    const piqDoc = await db
      .collection('piq_submissions')
      .doc(piqSubmissionId)
      .get();

    if (!piqDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        `PIQ submission ${piqSubmissionId} not found`
      );
    }

    const piqData = piqDoc.data();

    // Security: Verify ownership
    if (piqData.userId !== userId) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'You do not have permission to access this PIQ submission'
      );
    }

    // Call Gemini API for question generation
    const genAI = getGenAI();
    const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });

    const prompt = buildQuestionGenerationPrompt(piqData, questionCount);

    console.log(`Generating ${questionCount} questions for user ${userId}`);
    const result = await model.generateContent(prompt);
    const responseText = result.response.text();

    // Parse JSON response
    const questions = parseQuestionResponse(responseText);

    console.log(`Successfully generated ${questions.length} questions`);

    return {
      success: true,
      questions: questions,
      generatedAt: new Date().toISOString()
    };

  } catch (error) {
    console.error('Error generating questions:', error);

    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    throw new functions.https.HttpsError(
      'internal',
      `Failed to generate questions: ${error.message}`
    );
  }
});

/**
 * Build prompt for response analysis
 */
function buildAnalysisPrompt(question, response, expectedOLQs, responseMode) {
  const olqList = expectedOLQs.join(', ') || 'all 15 Officer-Like Qualities';

  return `
You are an SSB (Services Selection Board) psychologist analyzing a candidate's interview response.

**QUESTION**: ${question}
**TARGET OLQs**: ${olqList}
**RESPONSE MODE**: ${responseMode}

**CANDIDATE'S RESPONSE**:
${response}

**TASK**: Analyze the response and assess demonstrated Officer-Like Qualities (OLQs).

**EVALUATION CRITERIA**:
- Clarity and coherence of thought
- Depth of self-awareness
- Leadership potential indicators
- Problem-solving approach
- Emotional intelligence
- Confidence and communication style

**OUTPUT FORMAT** (JSON):
{
  "olqScores": [
    {
      "olq": "EFFECTIVE_INTELLIGENCE",
      "score": 5.5,
      "reasoning": "Why this score (lower is better in SSB scale)",
      "evidence": ["Specific phrases from response demonstrating this quality"]
    }
  ],
  "overallConfidence": 85,
  "keyInsights": ["Notable observations about the response"],
  "suggestedFollowUp": "Optional follow-up question to probe deeper"
}

**SCORING SCALE (SSB Convention - LOWER is BETTER)**:
1-3 = Exceptional (rare, outstanding performance)
4 = Excellent (top tier)
5 = Very Good (best common score - aim for this)
6 = Good (above average)
7 = Average (typical performance)
8 = Below Average (lowest acceptable)
9-10 = Poor (usually rejected)

**IMPORTANT**: Use decimal scores (e.g., 5.5) for precision. Most scores should fall in the 5-7 range (bell curve distribution).

Provide analysis as a valid JSON object.
  `.trim();
}

/**
 * Build prompt for question generation
 */
function buildQuestionGenerationPrompt(piqData, count) {
  const piqJson = JSON.stringify(piqData, null, 2);

  return `
You are an expert SSB (Services Selection Board) interviewer for the Indian Armed Forces.

Based on the candidate's Personal Information Questionnaire (PIQ) below, generate exactly ${count} personalized interview questions that assess Officer-Like Qualities (OLQs).

**PIQ DATA**:
${piqJson}

**AVAILABLE OLQs (15 total)**:
- Intellectual: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY, POWER_OF_EXPRESSION
- Social: SOCIAL_ADJUSTMENT, COOPERATION, INFLUENCE_GROUP
- Dynamic: INITIATIVE, SELF_CONFIDENCE, SPEED_OF_DECISION, DETERMINATION, COURAGE
- Character & Physical: STAMINA, LIVELINESS, SENSE_OF_RESPONSIBILITY

**REQUIREMENTS**:
1. Questions must be relevant to the candidate's background, interests, or experiences mentioned in PIQ
2. Each question should target 2-3 OLQs
3. Questions should be open-ended and require thoughtful responses
4. Use professional, respectful tone appropriate for SSB interview
5. Provide context explaining why this question is relevant

**OUTPUT FORMAT** (JSON array):
[
  {
    "id": "unique-id",
    "questionText": "The interview question",
    "expectedOLQs": ["INITIATIVE", "ORGANIZING_ABILITY"],
    "context": "Why this question is relevant to the candidate"
  }
]

Generate exactly ${count} questions as a valid JSON array.
  `.trim();
}

/**
 * Parse analysis response from Gemini
 */
function parseAnalysisResponse(responseText) {
  // Remove markdown code blocks if present
  const cleanJson = responseText
    .replace(/```json\n?/g, '')
    .replace(/```\n?/g, '')
    .trim();

  try {
    const parsed = JSON.parse(cleanJson);

    // Validate structure
    if (!parsed.olqScores || !Array.isArray(parsed.olqScores)) {
      throw new Error('Invalid response: missing olqScores array');
    }

    if (typeof parsed.overallConfidence !== 'number') {
      throw new Error('Invalid response: missing or invalid overallConfidence');
    }

    // Clamp scores and confidence to valid ranges
    parsed.olqScores.forEach(score => {
      if (score.score < 1) score.score = 1;
      if (score.score > 10) score.score = 10;
    });

    if (parsed.overallConfidence < 0) parsed.overallConfidence = 0;
    if (parsed.overallConfidence > 100) parsed.overallConfidence = 100;

    return parsed;
  } catch (error) {
    console.error('Failed to parse analysis response:', responseText);
    throw new Error(`Invalid JSON response from Gemini: ${error.message}`);
  }
}

/**
 * Parse question generation response from Gemini
 */
function parseQuestionResponse(responseText) {
  // Remove markdown code blocks if present
  const cleanJson = responseText
    .replace(/```json\n?/g, '')
    .replace(/```\n?/g, '')
    .trim();

  try {
    const parsed = JSON.parse(cleanJson);

    if (!Array.isArray(parsed)) {
      throw new Error('Invalid response: expected array of questions');
    }

    // Validate each question
    parsed.forEach((q, index) => {
      if (!q.questionText || !q.expectedOLQs) {
        throw new Error(`Invalid question at index ${index}: missing required fields`);
      }
    });

    return parsed;
  } catch (error) {
    console.error('Failed to parse question response:', responseText);
    throw new Error(`Invalid JSON response from Gemini: ${error.message}`);
  }
}
