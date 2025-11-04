#!/usr/bin/env node
/**
 * Fix corrupted OIR questions in Firestore batch_002
 * 
 * Issue: 5 questions have incorrect option IDs ("a", "b", "c", "d" instead of "opt_a", "opt_b", etc.)
 * Questions to fix: oir_q_0153, oir_q_0156, oir_q_0161, oir_q_0166, oir_q_0194
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixCorruptedQuestions() {
  console.log('🔧 Fixing corrupted OIR questions in batch_002...\n');
  
  try {
    // Read the corrected batch file
    const batchPath = path.join(__dirname, 'oir-batch-002.json');
    const batchData = JSON.parse(fs.readFileSync(batchPath, 'utf8'));
    
    const corruptedQuestionIds = [
      'oir_q_0153',
      'oir_q_0156',
      'oir_q_0161',
      'oir_q_0166',
      'oir_q_0194'
    ];
    
    // Find the corrected questions in the batch
    const correctedQuestions = batchData.questions.filter(q =>
      corruptedQuestionIds.includes(q.id)
    );
    
    console.log(`Found ${correctedQuestions.length} questions to fix:\n`);
    correctedQuestions.forEach(q => {
      console.log(`  - ${q.id}: ${q.question || q.questionText}`);
      console.log(`    Options: ${q.options.map(opt => opt.id).join(', ')}`);
      console.log(`    CorrectAnswer: ${q.correctAnswerId}\n`);
    });
    
    // Update each question in Firestore
    console.log('📤 Uploading corrected questions to Firestore...\n');
    
    for (const question of correctedQuestions) {
      const docRef = db.collection('test_content/oir/question_batches')
        .doc('batch_002');
      
      // Get current batch
      const batchDoc = await docRef.get();
      if (!batchDoc.exists) {
        console.error('❌ batch_002 document not found!');
        return;
      }
      
      const batchQuestions = batchDoc.data().questions || [];
      
      // Find and replace the corrupted question
      const index = batchQuestions.findIndex(q => q.id === question.id);
      if (index === -1) {
        console.error(`❌ Question ${question.id} not found in batch!`);
        continue;
      }
      
      batchQuestions[index] = question;
      
      // Update Firestore
      await docRef.update({ questions: batchQuestions });
      console.log(`✅ Fixed ${question.id}`);
    }
    
    console.log('\n✅ All corrupted questions fixed!');
    console.log('\n🎉 OIR test should now work correctly!');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

fixCorruptedQuestions();

