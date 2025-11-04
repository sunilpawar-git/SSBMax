#!/usr/bin/env node

/**
 * Fix Missing Question Numbers in batch_002
 * Questions oir_q_0151 through oir_q_0200 are missing questionNumber field
 */

const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixMissingQuestionNumbers() {
  console.log('🔧 Fixing missing questionNumber fields in batch_002\n');
  
  try {
    const batchRef = db.collection('test_content/oir/question_batches').doc('batch_002');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.log('❌ batch_002 not found');
      return;
    }
    
    const data = batchDoc.data();
    const questions = data.questions || [];
    
    console.log(`📊 Total questions: ${questions.length}`);
    
    let fixedCount = 0;
    
    // Fix each question
    const updatedQuestions = questions.map((q, index) => {
      // If questionNumber is missing or invalid
      if (!q.questionNumber || q.questionNumber <= 0) {
        // Extract number from ID (e.g., "oir_q_0153" -> 153)
        const match = q.id.match(/oir_q_(\d+)/) || q.id.match(/oir_(\d+)/);
        if (match) {
          const extractedNumber = parseInt(match[1], 10);
          console.log(`✓ Fixed ${q.id}: questionNumber = ${extractedNumber}`);
          fixedCount++;
          return {
            ...q,
            questionNumber: extractedNumber
          };
        } else {
          // Fallback: use index + 1
          console.log(`⚠️  ${q.id}: Using index-based number ${index + 1}`);
          fixedCount++;
          return {
            ...q,
            questionNumber: index + 1
          };
        }
      }
      return q;
    });
    
    console.log(`\n✅ Fixed ${fixedCount} questions`);
    
    // Upload back to Firestore
    console.log('\n📤 Uploading corrected batch to Firestore...');
    await batchRef.update({
      questions: updatedQuestions,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      version: '1.4'
    });
    
    console.log('✅ Upload complete!');
    console.log(`\n📊 Batch Summary:`);
    console.log(`   Total questions: ${updatedQuestions.length}`);
    console.log(`   Fixed questions: ${fixedCount}`);
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
  
  process.exit(0);
}

fixMissingQuestionNumbers();


