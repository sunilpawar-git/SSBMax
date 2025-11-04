const admin = require('firebase-admin');
const path = require('path');

// Initialize Firebase Admin
const serviceAccountPath = path.join(__dirname, '../.firebase/service-account.json');
const serviceAccount = require(serviceAccountPath);

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function verifyQuestions() {
  const corruptedIds = ['oir_q_0166', 'oir_q_0161', 'oir_q_0156', 'oir_q_0171'];
  
  console.log('🔍 Verifying previously corrupted questions...\n');
  
  for (const questionId of corruptedIds) {
    const docRef = db.collection('oir_questions')
      .doc('batch_002')
      .collection('questions')
      .doc(questionId);
    
    const doc = await docRef.get();
    if (doc.exists) {
      const data = doc.data();
      const hasCorrectId = data.correctAnswerId && data.correctAnswerId !== '';
      const status = hasCorrectId ? '✅' : '❌';
      
      console.log(`${status} ${questionId}`);
      console.log(`   correctAnswerId: "${data.correctAnswerId}"`);
      console.log(`   Options: ${data.options.map(o => o.id).join(', ')}`);
      console.log('');
    } else {
      console.log(`❌ ${questionId} - NOT FOUND`);
    }
  }
  
  process.exit(0);
}

verifyQuestions();


