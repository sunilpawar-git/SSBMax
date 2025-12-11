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

// ═══════════════════════════════════════════════════════════════════════════════
// OLQ DEFINITIONS - Complete definitions for all 15 Officer-Like Qualities
// ═══════════════════════════════════════════════════════════════════════════════

const OLQ_DEFINITIONS = `
OFFICER-LIKE QUALITIES (OLQs) - Definitions & Behavioral Indicators:

INTELLECTUAL QUALITIES (Factor-I):
1. EFFECTIVE_INTELLIGENCE
   Definition: Practical wisdom, ability to grasp situations quickly and take appropriate action
   Indicators: Common sense solutions, quick understanding, practical approach to problems
   Questions to reveal: Problem-solving scenarios, real-life challenges faced

2. REASONING_ABILITY
   Definition: Logical thinking, ability to analyze cause-effect relationships
   Indicators: Structured thinking, analytical approach, connecting dots
   Questions to reveal: Why questions, asking to explain decisions, hypothetical dilemmas

3. ORGANIZING_ABILITY
   Definition: Systematic planning, resource management, methodical approach
   Indicators: Planning before action, prioritization, delegation awareness
   Questions to reveal: How they planned events, managed projects, handled multiple tasks

4. POWER_OF_EXPRESSION
   Definition: Clear articulation, effective communication, vocabulary usage
   Indicators: Coherent speech, appropriate vocabulary, confident delivery
   Questions to reveal: Descriptive questions, asking to explain complex topics simply

SOCIAL QUALITIES (Factor-II):
5. SOCIAL_ADJUSTMENT
   Definition: Adaptability to different social situations, mixing with diverse people
   Indicators: Comfort in varied settings, respect for others, flexibility
   Questions to reveal: Experiences with different groups, handling cultural differences

6. COOPERATION
   Definition: Team player, willingness to help, putting group before self
   Indicators: Collaborative spirit, helping others, accepting others' ideas
   Questions to reveal: Team experiences, times they helped others, conflict resolution

7. INFLUENCE_GROUP
   Definition: Natural leadership, ability to convince and motivate others
   Indicators: Others follow their suggestions, can persuade without authority
   Questions to reveal: Leadership experiences, times they changed group opinion

DYNAMIC QUALITIES (Factor-III):
8. INITIATIVE
   Definition: Self-starter, proactive action without waiting for instructions
   Indicators: Volunteering, starting things independently, not waiting to be told
   Questions to reveal: Times they started something new, proactive problem-solving

9. SELF_CONFIDENCE
   Definition: Belief in own abilities, composure under pressure
   Indicators: Calm demeanor, positive self-talk, handling criticism well
   Questions to reveal: Challenging situations, how they handled failures

10. SPEED_OF_DECISION
    Definition: Quick decision-making without over-analysis
    Indicators: Timely decisions, comfortable with uncertainty, action-oriented
    Questions to reveal: Time-pressure situations, snap decisions made

11. DETERMINATION
    Definition: Persistence, goal-oriented, doesn't give up easily
    Indicators: Long-term goal pursuit, overcoming obstacles, bouncing back
    Questions to reveal: Long-term goals, times they persisted despite difficulties

12. COURAGE
    Definition: Physical and moral courage, standing up for beliefs
    Indicators: Speaking truth, facing fears, defending principles
    Questions to reveal: Times they stood up for something, faced fears

CHARACTER & PHYSICAL QUALITIES (Factor-IV):
13. SENSE_OF_RESPONSIBILITY
    Definition: Accountability, reliability, duty-consciousness
    Indicators: Owning mistakes, completing commitments, dependability
    Questions to reveal: Responsibilities held, times they owned up to mistakes

14. STAMINA
    Definition: Physical and mental endurance, resilience
    Indicators: Sustained effort, handling stress, physical fitness
    Questions to reveal: Endurance challenges, stressful periods handled

15. LIVELINESS
    Definition: Energy, enthusiasm, positive outlook
    Indicators: Active participation, optimistic attitude, energetic demeanor
    Questions to reveal: Hobbies, how they energize others, daily routine
`;

// ═══════════════════════════════════════════════════════════════════════════════
// PIQ TO OLQ MAPPING - Guidance for connecting PIQ data to OLQ assessment
// ═══════════════════════════════════════════════════════════════════════════════

const PIQ_TO_OLQ_MAPPING = `
PIQ SECTIONS → OLQ ASSESSMENT MAPPING:

PERSONAL BACKGROUND:
- Rural/Small town → SOCIAL_ADJUSTMENT (adaptability), STAMINA (hardships faced)
- Urban/Metro → POWER_OF_EXPRESSION (exposure), INFLUENCE_GROUP (diverse experiences)
- Relocated for studies/work → INITIATIVE, DETERMINATION

FAMILY ENVIRONMENT:
- Defense family → SENSE_OF_RESPONSIBILITY, COURAGE (values absorbed)
- Single parent → DETERMINATION, STAMINA (challenges overcome)
- Business family → INITIATIVE, ORGANIZING_ABILITY
- First-generation graduate → DETERMINATION, SELF_CONFIDENCE

EDUCATION JOURNEY:
- Boarding school → SOCIAL_ADJUSTMENT, COOPERATION
- High academic performance → EFFECTIVE_INTELLIGENCE, DETERMINATION
- Stream change → INITIATIVE, SPEED_OF_DECISION
- Co-curricular achievements → Varies based on activity

CAREER & WORK:
- Work experience → SENSE_OF_RESPONSIBILITY, ORGANIZING_ABILITY
- Entrepreneurship → INITIATIVE, COURAGE, SELF_CONFIDENCE
- Multiple jobs → SOCIAL_ADJUSTMENT or concerns about DETERMINATION

ACTIVITIES & INTERESTS:
- Team sports → COOPERATION, SOCIAL_ADJUSTMENT, INFLUENCE_GROUP
- Individual sports → SELF_CONFIDENCE, DETERMINATION, STAMINA
- Creative hobbies → POWER_OF_EXPRESSION, LIVELINESS
- Adventure activities → COURAGE, INITIATIVE

LEADERSHIP EXPOSURE:
- NCC training → SENSE_OF_RESPONSIBILITY, ORGANIZING_ABILITY, COURAGE
- Student body positions → INFLUENCE_GROUP, INITIATIVE
- Event organization → ORGANIZING_ABILITY, EFFECTIVE_INTELLIGENCE

SSB JOURNEY:
- First attempt → Assess baseline OLQs
- Repeat attempt → DETERMINATION (why trying again), learning shown
- Multiple attempts → Strong DETERMINATION, but probe SELF_CONFIDENCE

SELF-ASSESSMENT:
- Strengths stated → Verify through behavioral questions
- Weaknesses acknowledged → EFFECTIVE_INTELLIGENCE (self-awareness)
- Why defense → SENSE_OF_RESPONSIBILITY, COURAGE, DETERMINATION
`;

/**
 * Build comprehensive PIQ context from raw PIQ data
 * Extracts and organizes all relevant fields for AI consumption
 */
function buildComprehensivePIQContext(piqData) {
  // Extract nested data if present
  const data = piqData.data || piqData;

  // Helper functions
  const getString = (key) => (data[key] || '').toString().trim();
  const getMap = (key) => data[key] || {};
  const getList = (key) => data[key] || [];

  // Derive residence type
  const population = getString('maximumResidencePopulation');
  let residenceType = 'Unknown background';
  if (population.toLowerCase().includes('metro')) residenceType = 'Metropolitan city';
  else if (population.toLowerCase().includes('lakh')) residenceType = 'Large city';
  else if (parseInt(population) > 100000) residenceType = 'City';
  else if (parseInt(population) > 50000) residenceType = 'Town';
  else if (parseInt(population) > 10000) residenceType = 'Small town';
  else if (population) residenceType = 'Rural/Village background';

  // Build family context
  const familyContexts = [];
  const fatherOcc = getString('fatherOccupation').toLowerCase();
  if (['army', 'navy', 'air force', 'forces', 'military'].some(k => fatherOcc.includes(k))) {
    familyContexts.push('Defense family background');
  }
  const parentsAlive = getString('parentsAlive');
  if (parentsAlive.includes('Only') || getString('ageAtFatherDeath') || getString('ageAtMotherDeath')) {
    familyContexts.push('Single parent/guardian upbringing');
  }
  const motherOcc = getString('motherOccupation').toLowerCase();
  if (motherOcc && !motherOcc.includes('housewife') && !motherOcc.includes('homemaker')) {
    familyContexts.push('Working mother');
  }
  if (['govt', 'government', 'ias', 'ips', 'psu'].some(k => fatherOcc.includes(k))) {
    familyContexts.push('Government service family');
  }
  const familyContext = familyContexts.join(', ') || 'Standard family environment';

  // Build siblings summary
  const siblings = getList('siblings');
  const siblingSummary = siblings.length > 0 
    ? siblings.map(s => `${s.name || 'Sibling'} (${s.age || '?'}, ${s.occupation || 'unknown'})`).join('; ')
    : 'Only child / No siblings listed';

  // Build education summary
  const formatEducation = (edu, level) => {
    if (!edu || !edu.institution) return `${level}: Not provided`;
    const parts = [`Institution: ${edu.institution}`];
    if (edu.board) parts.push(`Board: ${edu.board}`);
    if (edu.stream) parts.push(`Stream: ${edu.stream}`);
    if (edu.percentage) parts.push(`Score: ${edu.percentage}%`);
    if (edu.cgpa) parts.push(`CGPA: ${edu.cgpa}`);
    if (edu.outstandingAchievement) parts.push(`Achievement: ${edu.outstandingAchievement}`);
    return `${level}:\n    ${parts.join('\n    ')}`;
  };

  // Build sports summary
  const sports = getList('sportsParticipation');
  const sportsSummary = sports.length > 0
    ? sports.map(s => {
        let str = s.sport || '';
        if (s.representedInstitution) str += ` (Represented: ${s.representedInstitution})`;
        if (s.outstandingAchievement) str += ` - ${s.outstandingAchievement}`;
        return str;
      }).filter(s => s).join('; ')
    : getString('sports') || 'Not specified';

  // Build work experience summary
  const workExp = getList('workExperience');
  const workSummary = workExp.length > 0
    ? workExp.map(w => `${w.role || 'Role'} at ${w.company || 'Company'} (${w.duration || 'duration unknown'})`).join('; ')
    : 'No prior work experience';

  // Build NCC details
  const ncc = getMap('nccTraining');
  const nccDetails = ncc.hasTraining
    ? `Yes - ${ncc.wing || ''} Wing, ${ncc.certificateObtained || 'Certificate pending'}`
    : 'No NCC training';

  // Build previous interviews summary
  const prevInterviews = getList('previousInterviews');
  const interviewSummary = prevInterviews.length > 0
    ? prevInterviews.map((pi, i) => `${i+1}. ${pi.typeOfEntry || 'Entry'} at ${pi.ssbPlace || 'SSB'} (${pi.date || 'date unknown'})`).join('\n  ')
    : 'First attempt (Freshie)';
  
  const attemptContext = prevInterviews.length === 0 
    ? 'Fresh candidate - no prior SSB experience'
    : prevInterviews.length === 1 
      ? 'Repeater (1 previous attempt) - has SSB exposure'
      : `Multiple attempts (${prevInterviews.length}) - highly determined`;

  // Build personalization notes
  const notes = [];
  if (ncc.hasTraining) notes.push(`→ Has NCC background (${ncc.wing} Wing) - explore leadership experiences`);
  if (prevInterviews.length > 0) notes.push('→ Repeater candidate - ask about learning from previous attempt(s)');
  if (['army', 'navy', 'air force', 'forces'].some(k => fatherOcc.includes(k))) {
    notes.push('→ Defense family background - explore influence and expectations');
  }
  if (sports.some(s => s.outstandingAchievement || s.representedInstitution)) {
    notes.push('→ Sports achievements present - ask about teamwork and competition');
  }
  if (workExp.length > 0) notes.push('→ Has work experience - explore professional challenges and growth');
  if (getString('positionsOfResponsibility')) notes.push('→ Has held leadership positions - probe leadership style');
  const personalizationNotes = notes.length > 0 ? notes.join('\n') : '→ Standard profile - use general SSB questioning approach';

  return `
CANDIDATE PROFILE
=================

PERSONAL BACKGROUND:
- Name: ${getString('fullName') || 'Not provided'}
- Age: ${getString('age') || 'Not provided'}
- Gender: ${getString('gender') || 'Not provided'}
- From: ${getString('district') || 'Unknown'}, ${getString('state') || 'Unknown'}
- Background: ${residenceType}
- Marital Status: ${getString('maritalStatus') || 'Not provided'}
- Religion: ${getString('religion') || 'Not provided'}
- Mother Tongue: ${getString('motherTongue') || 'Not provided'}

FAMILY ENVIRONMENT:
- Father: ${getString('fatherName') || 'Not provided'}
  • Occupation: ${getString('fatherOccupation') || 'Not provided'}
  • Education: ${getString('fatherEducation') || 'Not provided'}
- Mother: ${getString('motherName') || 'Not provided'}
  • Occupation: ${getString('motherOccupation') || 'Not provided'}
  • Education: ${getString('motherEducation') || 'Not provided'}
- Parents Status: ${parentsAlive || 'Both alive (assumed)'}
- Siblings: ${siblingSummary}
- Family Context: ${familyContext}

EDUCATION JOURNEY:
- ${formatEducation(getMap('education10th'), '10th Standard')}
- ${formatEducation(getMap('education12th'), '12th Standard')}
- ${formatEducation(getMap('educationGraduation'), 'Graduation')}
${getMap('educationPostGraduation').institution ? '- ' + formatEducation(getMap('educationPostGraduation'), 'Post-Graduation') : ''}

CAREER & WORK:
- Current Occupation: ${getString('presentOccupation') || 'Not specified (likely student/fresher)'}
- Work Experience: ${workSummary}

ACTIVITIES & INTERESTS:
- Hobbies: ${getString('hobbies') || 'Not specified'}
- Sports: ${sportsSummary}
- Extra-Curricular: ${getList('extraCurricularActivities').map(e => e.activityName).filter(a => a).join(', ') || 'None listed'}

LEADERSHIP EXPOSURE:
- NCC Training: ${nccDetails}
- Positions of Responsibility: ${getString('positionsOfResponsibility') || 'None mentioned'}

SSB JOURNEY:
- Choice of Service: ${getString('choiceOfService') || 'Not specified'}
- Nature of Commission: ${getString('natureOfCommission') || 'Not specified'}
- Previous SSB Attempts:
  ${interviewSummary}
- Candidate Type: ${attemptContext}

SELF-ASSESSMENT:
- Why Defense Forces:
  ${getString('whyDefenseForces') || 'Not provided'}
- Stated Strengths:
  ${getString('strengths') || 'Not provided'}
- Acknowledged Weaknesses:
  ${getString('weaknesses') || 'Not provided'}

PERSONALIZATION NOTES:
${personalizationNotes}
  `.trim();
}

/**
 * Get difficulty level description
 */
function getDifficultyDescription(difficulty) {
  const descriptions = {
    1: 'Icebreaker - Warm, easy opening questions to build rapport',
    2: 'Basic Probing - Simple background exploration, direct questions',
    3: 'Moderate Challenge - Situational questions, "what would you do" scenarios',
    4: 'Deep Probing - Challenging assumptions, value-based dilemmas',
    5: 'Stress Testing - Rapid fire, contradiction exploration, pressure scenarios'
  };
  return descriptions[difficulty] || 'Standard difficulty';
}

/**
 * Build prompt for response analysis
 */
function buildAnalysisPrompt(question, response, expectedOLQs, responseMode) {
  const olqList = expectedOLQs.join(', ') || 'all 15 Officer-Like Qualities';

  return `
You are an SSB PSYCHOLOGIST analyzing a candidate's interview response.

═══════════════════════════════════════════════════════════════════════════════
INTERVIEW CONTEXT:
═══════════════════════════════════════════════════════════════════════════════

QUESTION ASKED: ${question}

TARGET OLQs FOR THIS QUESTION: ${olqList}

RESPONSE MODE: ${responseMode}

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE'S RESPONSE:
═══════════════════════════════════════════════════════════════════════════════

${response}

═══════════════════════════════════════════════════════════════════════════════
OLQ SCORING REFERENCE:
═══════════════════════════════════════════════════════════════════════════════

${OLQ_DEFINITIONS}

═══════════════════════════════════════════════════════════════════════════════
SCORING SCALE (SSB Convention - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

1-2 = Exceptional (Outstanding, rare excellence)
3   = Excellent (Top tier performance)
4   = Very Good (Above expectations)
5   = Good (Solid, meets standards)
6   = Average (Acceptable, typical)
7   = Below Average (Needs improvement)
8   = Poor (Significant concerns)
9-10 = Very Poor (Major deficiency)

DISTRIBUTION GUIDELINE (Bell Curve):
- Most candidates score 5-7 (70% of scores)
- Exceptional (1-3) and Poor (8-10) are rare (15% each)
- Use decimal scores for precision (e.g., 5.5, 6.2)

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Analyze the response and provide OLQ assessment.

FOR EACH TARGET OLQ, ASSESS:
1. Score (1-10 with decimals, lower is better)
2. Specific reasoning based on what they said
3. Direct evidence (exact phrases/behaviors from response)

ALSO PROVIDE:
- Overall confidence in your assessment (0-100%)
- Key insights about the candidate
- Suggested follow-up question if needed

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Return ONLY valid JSON):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": [
    {
      "olq": "OLQ_NAME",
      "score": 5.5,
      "reasoning": "Why this score - specific to their response",
      "evidence": ["Exact phrases from response that support this score"]
    }
  ],
  "overallConfidence": 75,
  "keyInsights": [
    "Notable observations about the candidate based on this response"
  ],
  "suggestedFollowUp": "A follow-up question to probe further if needed"
}

IMPORTANT:
- Score ONLY the OLQs listed in TARGET OLQs
- Lower scores mean BETTER performance
- Be specific with evidence - quote from their response
- If response doesn't provide enough data for an OLQ, note this
  `.trim();
}

/**
 * Build prompt for question generation with comprehensive PIQ context
 */
function buildQuestionGenerationPrompt(piqData, count, difficulty = 3) {
  // Build comprehensive PIQ context from raw data
  const piqContext = buildComprehensivePIQContext(piqData);
  const difficultyDesc = getDifficultyDescription(difficulty);

  return `
You are a SENIOR SSB PSYCHOLOGIST with 20+ years of experience at Services Selection Board.
Your expertise: Identifying Officer-Like Qualities through strategic, personalized questioning.

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE'S PERSONAL INFORMATION QUESTIONNAIRE (PIQ):
═══════════════════════════════════════════════════════════════════════════════

${piqContext}

═══════════════════════════════════════════════════════════════════════════════
OLQ REFERENCE GUIDE:
═══════════════════════════════════════════════════════════════════════════════

${OLQ_DEFINITIONS}

═══════════════════════════════════════════════════════════════════════════════
PIQ TO OLQ MAPPING (use this to connect candidate's background to OLQs):
═══════════════════════════════════════════════════════════════════════════════

${PIQ_TO_OLQ_MAPPING}

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Generate exactly ${count} PERSONALIZED interview questions for this candidate.

DIFFICULTY LEVEL: ${difficulty}/5 (${difficultyDesc})

OLQ TARGETING: Generate a balanced mix covering all OLQ clusters (Intellectual, Social, Dynamic, Character)

═══════════════════════════════════════════════════════════════════════════════
QUESTION REQUIREMENTS (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. PERSONALIZATION IS CRITICAL:
   ✓ MUST reference specific details from the candidate's PIQ
   ✓ Name their hobby, mention their father's profession, cite their college
   ✓ Reference their stated strengths/weaknesses
   ✗ NO generic questions that could apply to anyone

2. OLQ TARGETING:
   ✓ Each question should target 2-3 specific OLQs
   ✓ Explain in reasoning how the answer would reveal those OLQs
   ✓ Balance across OLQ clusters unless specific targeting requested

3. QUESTION TYPES TO INCLUDE (mix these):
   - Background Exploration: "You mentioned [specific PIQ detail]... tell me more about..."
   - Situational: "Given your experience in [PIQ detail], what would you do if..."
   - Value-Based: "Your [family/background] has [characteristic]... how has that shaped..."
   - Achievement Deep-Dive: "You achieved [specific thing]... walk me through the challenges..."
   - Hypothetical Dilemma: Based on their background, present a relevant dilemma

4. QUESTION QUALITY STANDARDS:
   ✓ Open-ended requiring 2-3 minute responses
   ✓ Create natural follow-up opportunities
   ✓ Reveal character through stories, not just opinions
   ✗ NO yes/no questions
   ✗ NO questions answerable in one sentence
   ✗ NO clichéd questions like "Tell me about yourself"
   ✗ NO questions about future plans (too easy to rehearse)

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Return ONLY valid JSON array):
═══════════════════════════════════════════════════════════════════════════════

[
  {
    "id": "q1",
    "questionText": "Your personalized question here?",
    "expectedOLQs": ["OLQ_NAME_1", "OLQ_NAME_2"],
    "context": "Why this question for THIS candidate specifically, what OLQ indicators it reveals",
    "piqTouchpoint": "Which PIQ field/detail this references"
  }
]

IMPORTANT: 
- Return ONLY the JSON array, no markdown formatting
- Use exact OLQ names from the list (e.g., EFFECTIVE_INTELLIGENCE, not "Effective Intelligence")
- Generate exactly ${count} questions
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
