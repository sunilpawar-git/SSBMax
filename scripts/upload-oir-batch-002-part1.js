#!/usr/bin/env node

/**
 * Upload OIR batch_002 part1 (50 questions: 101-150) to Firestore
 * 
 * Usage: node upload-oir-batch-002-part1.js
 * 
 * Prerequisites:
 * 1. Firebase Admin SDK initialized
 * 2. Service account key configured
 * 3. oir-batch-002-part1.json file in same directory
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin SDK
const serviceAccountPath = path.join(__dirname, '../.firebase/service-account.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error('❌ Error: Firebase service account key not found');
  console.error(`   Expected at: ${serviceAccountPath}`);
  process.exit(1);
}

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('✅ Firebase Admin SDK initialized');
} catch (error) {
  console.error('❌ Error initializing Firebase:', error.message);
  process.exit(1);
}

const db = admin.firestore();

/**
 * Upload OIR batch_002 part1 to Firestore
 */
async function uploadOIRBatch() {
  try {
    console.log('\n📚 Loading OIR batch_002 part1 from file...');
    
    // Read batch file
    const batchPath = path.join(__dirname, 'oir-batch-002-part1.json');
    if (!fs.existsSync(batchPath)) {
      throw new Error('oir-batch-002-part1.json not found in scripts directory');
    }
    
    const batchData = JSON.parse(fs.readFileSync(batchPath, 'utf8'));
    console.log(`   Loaded: ${batchData.questions.length} questions`);
    console.log(`   Part: ${batchData.metadata.part}`);
    console.log(`   Question range: ${batchData.questions[0].questionNumber}-${batchData.questions[batchData.questions.length-1].questionNumber}`);
    
    // Upload batch to Firestore as batch_002
    console.log('\n📦 Uploading OIR batch_002 (part 1 of 2)...');
    const batchRef = db.collection('test_content')
      .doc('oir')
      .collection('question_batches')
      .doc('batch_002');
    
    // Check if batch_002 already exists
    const existingBatch = await batchRef.get();
    let finalQuestions = batchData.questions;
    
    if (existingBatch.exists) {
      console.log('   ℹ️  batch_002 already exists, merging questions...');
      const existingData = existingBatch.data();
      // Merge questions (avoid duplicates by ID)
      const existingQuestionIds = new Set(existingData.questions.map(q => q.id));
      const newQuestions = batchData.questions.filter(q => !existingQuestionIds.has(q.id));
      finalQuestions = [...existingData.questions, ...newQuestions];
      console.log(`   Added ${newQuestions.length} new questions to existing ${existingData.questions.length} questions`);
    }
    
    await batchRef.set({
      batch_id: 'batch_002',
      version: batchData.metadata.version,
      question_count: finalQuestions.length,
      questions: finalQuestions,
      uploaded_at: admin.firestore.FieldValue.serverTimestamp(),
      part_info: existingBatch.exists ? 'Parts 1 & 2 merged' : 'Part 1 of 2'
    });
    
    console.log(`   ✅ batch_002: ${finalQuestions.length} questions uploaded`);
    
    // Update metadata (aggregate)
    console.log('\n📝 Updating metadata...');
    const metaRef = db.collection('test_content').doc('oir').collection('meta').doc('config');
    
    // Get existing metadata
    const metaDoc = await metaRef.get();
    const existingMeta = metaDoc.exists ? metaDoc.data() : {};
    
    // Calculate total questions across all batches
    const batchesSnapshot = await db.collection('test_content')
      .doc('oir')
      .collection('question_batches')
      .get();
    
    let totalQuestions = 0;
    let totalBatches = 0;
    batchesSnapshot.forEach(doc => {
      const data = doc.data();
      totalQuestions += data.question_count || 0;
      totalBatches++;
    });
    
    const updatedMeta = {
      total_questions: totalQuestions,
      version: batchData.metadata.version,
      last_updated: admin.firestore.FieldValue.serverTimestamp(),
      batches: totalBatches,
      distribution: batchData.metadata.distribution_part1 || existingMeta.distribution,
      description: `OIR question repository - ${totalBatches} batches with ${totalQuestions} questions`
    };
    
    await metaRef.set(updatedMeta, { merge: true });
    console.log('   ✅ Metadata updated');
    
    // Verify upload
    console.log('\n🔍 Verifying upload...');
    const uploadedBatch = await batchRef.get();
    
    if (!uploadedBatch.exists) {
      throw new Error('Batch verification failed - document not found');
    }
    
    const verifiedData = uploadedBatch.data();
    console.log(`   Batch ID: ${verifiedData.batch_id}`);
    console.log(`   Question count: ${verifiedData.question_count}`);
    console.log(`   Questions verified: ${verifiedData.questions.length}`);
    
    if (verifiedData.questions.length === finalQuestions.length) {
      console.log('\n✅ SUCCESS! All questions uploaded and verified');
    } else {
      console.warn('\n⚠️  Warning: Question count mismatch');
      console.warn(`   Expected: ${finalQuestions.length}`);
      console.warn(`   Found: ${verifiedData.questions.length}`);
    }
    
    // Display sample questions
    console.log('\n📄 Sample questions from Firestore:');
    const sampleQuestions = verifiedData.questions.slice(0, 3);
    sampleQuestions.forEach(question => {
      console.log(`\n   ID: ${question.id} (#${question.questionNumber})`);
      console.log(`   Type: ${question.type} - ${question.subtype}`);
      console.log(`   Question: ${question.questionText.substring(0, 60)}...`);
      console.log(`   Difficulty: ${question.difficulty}`);
    });
    
    console.log('\n🎉 Upload complete!');
    console.log(`\n📊 Summary:`);
    console.log(`   Questions in this upload: ${batchData.questions.length}`);
    console.log(`   Total questions in batch_002: ${finalQuestions.length}`);
    console.log(`   Total questions across all batches: ${totalQuestions}`);
    console.log(`   Firestore path: test_content/oir/question_batches/batch_002`);
    console.log(`\n📝 Note: Upload part2 to complete batch_002 (target: 100 questions)`);
    
  } catch (error) {
    console.error('\n❌ Error uploading OIR batch:', error);
    throw error;
  }
}

// Main execution
(async () => {
  try {
    await uploadOIRBatch();
    process.exit(0);
  } catch (error) {
    console.error('\n💥 Fatal error:', error);
    process.exit(1);
  }
})();

