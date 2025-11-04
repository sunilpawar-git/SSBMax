#!/usr/bin/env node
const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function findCorrupted() {
  console.log('🔍 Searching for questions with corrupted correctAnswerId...\n');
  
  const batches = ['batch_001', 'batch_002'];
  
  for (const batchId of batches) {
    const docRef = db.collection('test_content/oir/question_batches').doc(batchId);
    const doc = await docRef.get();
    
    if (!doc.exists) continue;
    
    const questions = doc.data().questions || [];
    
    console.log(`\n📦 Checking ${batchId} (${questions.length} questions):`);
    console.log('─'.repeat(70));
    
    const corrupted = questions.filter(q => {
      // Check if correctAnswerId doesn't match any option ID
      const optionIds = q.options.map(o => o.id);
      return q.correctAnswerId && !optionIds.includes(q.correctAnswerId);
    });
    
    if (corrupted.length > 0) {
      console.log(`❌ Found ${corrupted.length} questions with mismatched correctAnswerId:\n`);
      
      corrupted.forEach(q => {
        console.log(`Question: ${q.id}`);
        console.log(`  correctAnswerId: "${q.correctAnswerId}"`);
        console.log(`  Available options: ${q.options.map(o => o.id).join(', ')}`);
        console.log('');
      });
    } else {
      console.log('✅ All questions have valid correctAnswerId');
    }
  }
  
  await admin.app().delete();
}

findCorrupted();
