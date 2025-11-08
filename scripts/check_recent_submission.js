const admin = require('firebase-admin');

const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkRecentSubmissions() {
  // Get all submissions (no query, just scan recent ones)
  const submissionsRef = db.collection('submissions');
  const snapshot = await submissionsRef
    .limit(5)
    .get();
  
  console.log(`Found ${snapshot.size} recent submissions\n`);
  
  snapshot.forEach(doc => {
    const data = doc.data();
    console.log('Submission ID:', doc.id);
    console.log('Test Type:', data.testType);
    console.log('User ID:', data.userId);
    console.log('Status:', data.status);
    console.log('Submitted At:', data.submittedAt ? new Date(data.submittedAt).toISOString() : 'N/A');
    console.log('---');
  });
  
  process.exit(0);
}

checkRecentSubmissions();
