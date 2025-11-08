const admin = require('firebase-admin');

const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkSubmissions() {
  const userId = 'MEkxQsweaEhNYFa0LeTJgCUDqVc2';
  
  // Get all submissions for this user
  const submissionsRef = db.collection('submissions');
  const snapshot = await submissionsRef
    .where('userId', '==', userId)
    .orderBy('submittedAt', 'desc')
    .limit(3)
    .get();
  
  console.log(`Found ${snapshot.size} submissions for user ${userId}\n`);
  
  snapshot.forEach(doc => {
    const data = doc.data();
    console.log('Submission ID:', doc.id);
    console.log('Test Type:', data.testType);
    console.log('User ID:', data.userId);
    console.log('Status:', data.status);
    console.log('Submitted At:', new Date(data.submittedAt).toISOString());
    console.log('Has data field:', !!data.data);
    console.log('Has responses field:', !!data.responses);
    console.log('---');
  });
  
  process.exit(0);
}

checkSubmissions();
