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

async function checkFirestore() {
  console.log('🔍 Checking ACTUAL Firestore data for corrupted questions...\n');
  
  const corruptedIds = ['oir_q_0166', 'oir_q_0171', 'oir_q_0176'];
  
  for (const questionId of corruptedIds) {
    // Check batch_002
    const docRef = db.collection('oir_questions')
      .doc('batch_002')
      .collection('questions')
      .doc(questionId);
    
    const doc = await docRef.get();
    if (doc.exists) {
      const data = doc.data();
      const status = (data.correctAnswerId && data.correctAnswerId !== '') ? '✅' : '❌';
      
      console.log(`${status} ${questionId} (batch_002)`);
      console.log(`   correctAnswerId: "${data.correctAnswerId}"`);
      console.log(`   Options: ${data.options ? data.options.map(o => o.id).join(', ') : 'N/A'}`);
      console.log('');
    } else {
      console.log(`⚠️  ${questionId} NOT FOUND in batch_002`);
      
      // Check if it's in batch_001
      const batch001Ref = db.collection('oir_questions')
        .doc('batch_001')
        .collection('questions')
        .doc(questionId);
      
      const batch001Doc = await batch001Ref.get();
      if (batch001Doc.exists) {
        const data = batch001Doc.data();
        const status = (data.correctAnswerId && data.correctAnswerId !== '') ? '✅' : '❌';
        console.log(`   Found in batch_001: ${status}`);
        console.log(`   correctAnswerId: "${data.correctAnswerId}"`);
      } else {
        console.log(`   Not in batch_001 either`);
      }
      console.log('');
    }
  }
  
  // Count total questions in batch_002
  const batch002Snapshot = await db.collection('oir_questions')
    .doc('batch_002')
    .collection('questions')
    .get();
  
  console.log(`\n📊 Total questions in batch_002: ${batch002Snapshot.size}`);
  
  process.exit(0);
}

checkFirestore();







