#!/usr/bin/env node
const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkQuestions() {
  console.log('🔍 Checking specific questions...\n');
  
  const problemIds = ['oir_103', 'oir_110', 'oir_113', 'oir_120', 'oir_114', 'oir_104'];
  
  const docRef = db.collection('test_content/oir/question_batches').doc('batch_001');
  const doc = await docRef.get();
  
  const questions = doc.data().questions || [];
  
  for (const qId of problemIds) {
    const q = questions.find(x => x.id === qId);
    if (q) {
      console.log(`\n📋 Question: ${q.id}`);
      console.log(`   correctAnswerId: "${q.correctAnswerId}"`);
      console.log(`   Options:`);
      q.options.forEach(opt => {
        console.log(`     - ${opt.id}: ${opt.text?.substring(0, 50) || 'N/A'}`);
      });
    } else {
      console.log(`\n❌ Question ${qId} not found in batch_001`);
    }
  }
  
  await admin.app().delete();
}

checkQuestions();
