const admin = require('firebase-admin');

const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkDoc() {
  const userId = 'MEkxQsweaEhNYFa0LeTJgCUDqVc2';
  const month = '2025-11';
  
  const docRef = db.collection('users')
    .doc(userId)
    .collection('subscription')
    .doc(`usage_${month}`);
  
  const doc = await docRef.get();
  
  console.log('Document exists:', doc.exists);
  if (doc.exists) {
    console.log('Document data:', JSON.stringify(doc.data(), null, 2));
  } else {
    console.log('Document does NOT exist - this is why create rule is being checked');
  }
  
  process.exit(0);
}

checkDoc();
