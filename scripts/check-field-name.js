#!/usr/bin/env node
const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkFieldName() {
  const docRef = db.collection('test_content/oir/question_batches').doc('batch_002');
  const doc = await docRef.get();
  const questions = doc.data().questions || [];
  
  // Check oir_q_0153 specifically
  const q153 = questions.find(q => q.id === 'oir_q_0153');
  
  console.log('Question oir_q_0153 fields:');
  console.log('  questionText:', q153.questionText || 'MISSING');
  console.log('  question:', q153.question || 'MISSING');
  console.log('\nAll fields:', Object.keys(q153));
  
  await admin.app().delete();
}

checkFieldName();
