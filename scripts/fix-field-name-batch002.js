#!/usr/bin/env node
/**
 * CRITICAL FIX: Rename 'question' field to 'questionText' for questions 151-200
 */

const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixFieldNames() {
  console.log('🔧 CRITICAL FIX: Renaming "question" field to "questionText"...\n');
  
  try {
    const docRef = db.collection('test_content/oir/question_batches').doc('batch_002');
    const doc = await docRef.get();
    const questions = doc.data().questions || [];
    
    let fixedCount = 0;
    const fixedQuestions = questions.map(q => {
      const fixed = { ...q };
      
      // If question field exists but questionText doesn't, rename it
      if (fixed.question && !fixed.questionText) {
        fixed.questionText = fixed.question;
        delete fixed.question;
        fixedCount++;
      }
      
      return fixed;
    });
    
    console.log(`✅ Fixed ${fixedCount} questions (renamed "question" → "questionText")\n`);
    
    // Upload
    console.log('📤 Uploading to Firestore...');
    await docRef.update({
      questions: fixedQuestions,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      version: '1.3',
      notes: 'Fixed field name: renamed "question" to "questionText" for questions 151-200'
    });
    
    console.log('✅ Upload complete!');
    console.log('\n🎉 All questions now use standard "questionText" field!');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

fixFieldNames();
