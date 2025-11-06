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

async function verifyCorruptedQuestions() {
  console.log('🔍 Verifying the 4 corrupted questions in Firestore...\n');
  
  const corruptedIds = [
    'oir_q_0161',
    'oir_q_0186', 
    'oir_q_0176',
    'oir_q_0193'
  ];
  
  for (const questionId of corruptedIds) {
    console.log(`\n━━━ ${questionId} ━━━`);
    
    // Check batch_001
    const batch001Ref = db.collection('oir_questions')
      .doc('batch_001')
      .collection('questions')
      .doc(questionId);
    
    const batch001Doc = await batch001Ref.get();
    
    if (batch001Doc.exists) {
      const data = batch001Doc.data();
      const correctAnswerId = data.correctAnswerId || '';
      const options = data.options ? data.options.map(o => o.id) : [];
      
      if (correctAnswerId === '' || !options.includes(correctAnswerId)) {
        console.log(`❌ FOUND IN batch_001 - CORRUPTED!`);
        console.log(`   correctAnswerId: "${correctAnswerId}"`);
        console.log(`   Options: ${options.join(', ')}`);
      } else {
        console.log(`✅ Found in batch_001 - CORRECT`);
        console.log(`   correctAnswerId: "${correctAnswerId}"`);
        console.log(`   Options: ${options.join(', ')}`);
      }
    }
    
    // Check batch_002
    const batch002Ref = db.collection('oir_questions')
      .doc('batch_002')
      .collection('questions')
      .doc(questionId);
    
    const batch002Doc = await batch002Ref.get();
    
    if (batch002Doc.exists) {
      const data = batch002Doc.data();
      const correctAnswerId = data.correctAnswerId || '';
      const options = data.options ? data.options.map(o => o.id) : [];
      
      if (correctAnswerId === '' || !options.includes(correctAnswerId)) {
        console.log(`❌ FOUND IN batch_002 - CORRUPTED!`);
        console.log(`   correctAnswerId: "${correctAnswerId}"`);
        console.log(`   Options: ${options.join(', ')}`);
      } else {
        console.log(`✅ Found in batch_002 - CORRECT`);
        console.log(`   correctAnswerId: "${correctAnswerId}"`);
        console.log(`   Options: ${options.join(', ')}`);
      }
    }
    
    if (!batch001Doc.exists && !batch002Doc.exists) {
      console.log(`❌ NOT FOUND in either batch!`);
    }
  }
  
  process.exit(0);
}

verifyCorruptedQuestions().catch(console.error);



