#!/usr/bin/env node
/**
 * Fix remaining 20 corrupted questions in batch_002
 * Questions oir_101 through oir_120 have correctAnswerId like "opt_103_b" instead of "opt_b"
 */

const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixRemaining() {
  console.log('🔧 Fixing remaining 20 corrupted questions in batch_002...\n');
  
  try {
    const docRef = db.collection('test_content/oir/question_batches').doc('batch_002');
    const doc = await docRef.get();
    
    const questions = doc.data().questions || [];
    
    let fixedCount = 0;
    const fixedQuestions = questions.map(q => {
      const fixed = { ...q };
      const optionIds = q.options.map(o => o.id);
      
      // Check if correctAnswerId doesn't match any option
      if (fixed.correctAnswerId && !optionIds.includes(fixed.correctAnswerId)) {
        // Extract the letter from correctAnswerId (e.g., "opt_103_b" -> "b")
        const match = fixed.correctAnswerId.match(/_([a-d])$/);
        if (match) {
          const letter = match[1];
          const correctFormat = `opt_${letter}`;
          
          console.log(`Fixing ${q.id}:`);
          console.log(`  Before: ${fixed.correctAnswerId}`);
          console.log(`  After:  ${correctFormat}`);
          
          fixed.correctAnswerId = correctFormat;
          fixedCount++;
        }
      }
      
      return fixed;
    });
    
    console.log(`\n✅ Fixed ${fixedCount} questions\n`);
    
    // Upload
    console.log('📤 Uploading to Firestore...');
    await docRef.update({
      questions: fixedQuestions,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      version: '1.2',
      notes: 'Fixed correctAnswerId format for questions 101-120'
    });
    
    console.log('✅ Upload complete!');
    console.log('\n🎉 All batch_002 questions are now fully corrected!');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

fixRemaining();
