#!/usr/bin/env node

/**
 * Firebase Admin Script: Upload Generic Interview Questions
 *
 * Purpose:
 * - Uploads curated generic SSB interview questions to Firestore
 * - Creates 'generic_questions' collection with permanent questions
 * - Used for 25% of interview question pool (70/25/5 distribution)
 *
 * Usage:
 *   node upload_generic_questions.js
 *
 * Prerequisites:
 *   1. Firebase Admin SDK initialized
 *   2. Service account key JSON file (see README for setup)
 *   3. generic_interview_questions.json in project root
 *
 * Environment:
 *   - GOOGLE_APPLICATION_CREDENTIALS: Path to service account key
 *   - Or use default application credentials
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin SDK
// Uses GOOGLE_APPLICATION_CREDENTIALS environment variable or default credentials
try {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId: 'ssbmax-app' // Replace with your Firebase project ID
  });
  console.log('✅ Firebase Admin SDK initialized');
} catch (error) {
  console.error('❌ Failed to initialize Firebase Admin SDK');
  console.error('Make sure GOOGLE_APPLICATION_CREDENTIALS is set or use:');
  console.error('export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"');
  process.exit(1);
}

const db = admin.firestore();

/**
 * Upload generic questions to Firestore
 */
async function uploadGenericQuestions() {
  try {
    // Read JSON file
    const jsonPath = path.join(__dirname, 'generic_interview_questions.json');
    console.log(`📂 Reading questions from: ${jsonPath}`);

    if (!fs.existsSync(jsonPath)) {
      throw new Error(`File not found: ${jsonPath}`);
    }

    const jsonData = fs.readFileSync(jsonPath, 'utf8');
    const data = JSON.parse(jsonData);

    console.log(`📋 Found ${data.questions.length} questions to upload`);
    console.log(`📊 Metadata:`, data.metadata);

    // Batch write for efficiency
    const batchSize = 500; // Firestore batch limit
    let batch = db.batch();
    let operationCount = 0;
    let totalUploaded = 0;

    for (const question of data.questions) {
      // Validate required fields
      if (!question.id || !question.text || !question.targetOLQs) {
        console.warn(`⚠️ Skipping invalid question: ${JSON.stringify(question)}`);
        continue;
      }

      // Create document reference
      const docRef = db.collection('generic_questions').doc(question.id);

      // Prepare document data
      const docData = {
        id: question.id,
        text: question.text,
        category: question.category || 'General',
        targetOLQs: question.targetOLQs,
        difficulty: question.difficulty || 3,
        expectedDuration: question.expectedDuration || 150,
        source: 'GENERIC_POOL',
        isPermanent: true,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        usageCount: 0, // Track how many times this question is used
        lastUsed: null,
        isActive: true // Can be set to false to temporarily disable a question
      };

      // Add to batch
      batch.set(docRef, docData);
      operationCount++;

      // Commit batch when it reaches the limit
      if (operationCount >= batchSize) {
        await batch.commit();
        totalUploaded += operationCount;
        console.log(`✅ Uploaded batch of ${operationCount} questions (Total: ${totalUploaded})`);
        batch = db.batch();
        operationCount = 0;
      }
    }

    // Commit remaining documents
    if (operationCount > 0) {
      await batch.commit();
      totalUploaded += operationCount;
      console.log(`✅ Uploaded final batch of ${operationCount} questions`);
    }

    console.log('');
    console.log('════════════════════════════════════════');
    console.log('✅ SUCCESS: Generic questions uploaded!');
    console.log('════════════════════════════════════════');
    console.log(`📦 Total questions uploaded: ${totalUploaded}`);
    console.log(`🗂️ Collection: generic_questions`);
    console.log(`🌐 Project: ${admin.app().options.projectId}`);
    console.log('');
    console.log('Next steps:');
    console.log('1. Verify questions in Firebase Console');
    console.log('2. Create Firestore index: generic_questions (difficulty ASC, usageCount ASC)');
    console.log('3. Test question retrieval in the app');
    console.log('');

  } catch (error) {
    console.error('');
    console.error('════════════════════════════════════════');
    console.error('❌ ERROR: Failed to upload questions');
    console.error('════════════════════════════════════════');
    console.error(error.message);
    console.error(error.stack);
    throw error;
  }
}

/**
 * Main execution
 */
async function main() {
  try {
    console.log('');
    console.log('════════════════════════════════════════');
    console.log('🚀 Starting Generic Questions Upload');
    console.log('════════════════════════════════════════');
    console.log('');

    await uploadGenericQuestions();

    console.log('✅ Script completed successfully!');
    process.exit(0);
  } catch (error) {
    console.error('❌ Script failed!');
    process.exit(1);
  }
}

// Run the script
main();
