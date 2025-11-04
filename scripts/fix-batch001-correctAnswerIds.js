#!/usr/bin/env node
/**
 * Fix batch_001 questions with incorrect correctAnswerId format
 * Change "opt_103_b" to "opt_b", etc.
 */

const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixBatch001CorrectAnswers() {
  console.log('🔧 Fixing batch_001 correctAnswerId format...\n');
  
  try {
    const docRef = db.collection('test_content/oir/question_batches').doc('batch_001');
    const doc = await docRef.get();
    
    if (!doc.exists) {
      console.error('❌ batch_001 not found!');
      return;
    }
    
    const questions = doc.data().questions || [];
    console.log(`📄 Found ${questions.length} questions in batch_001\n`);
    
    // Find and fix corrupted correctAnswerId fields
    let fixedCount = 0;
    const fixedQuestions = questions.map(q => {
      const fixed = { ...q };
      
      // Check if correctAnswerId has incorrect format (e.g., "opt_103_b" instead of "opt_b")
      if (fixed.correctAnswerId && fixed.correctAnswerId.includes('_') && fixed.correctAnswerId.split('_').length > 2) {
        const parts = fixed.correctAnswerId.split('_');
        const letter = parts[parts.length - 1]; // Get last part (the letter)
        const correctFormat = `opt_${letter}`;
        
        console.log(`Fixing ${q.id}:`);
        console.log(`  Before: ${fixed.correctAnswerId}`);
        console.log(`  After:  ${correctFormat}`);
        
        fixed.correctAnswerId = correctFormat;
        fixedCount++;
      }
      
      return fixed;
    });
    
    if (fixedCount === 0) {
      console.log('✅ No corrections needed - all correctAnswerId fields are valid!');
      return;
    }
    
    console.log(`\n📊 Fixed ${fixedCount} questions\n`);
    
    // Upload corrected batch
    console.log('📤 Uploading corrected batch_001 to Firestore...\n');
    
    await docRef.update({
      questions: fixedQuestions,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      version: '1.1',
      notes: 'Fixed correctAnswerId format (removed question number from IDs)'
    });
    
    console.log('✅ Successfully uploaded corrected batch_001!');
    console.log('\n🎉 All correctAnswerId fields are now in correct format!');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

fixBatch001CorrectAnswers();
