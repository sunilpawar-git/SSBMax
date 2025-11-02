const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin SDK
const serviceAccountPath = path.join(__dirname, '../.firebase/service-account.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error('❌ Error: Firebase service account key not found');
  console.error(`   Expected at: ${serviceAccountPath}`);
  process.exit(1);
}

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
} catch (error) {
  console.error('❌ Error initializing Firebase:', error.message);
  process.exit(1);
}

const db = admin.firestore();

async function uploadOIRBatch002() {
  try {
    console.log('📚 Reading OIR batch_002.json...');
    const batchData = JSON.parse(fs.readFileSync('./oir-batch-002.json', 'utf8'));
    
    console.log(`📊 Batch Info: ${batchData.totalQuestions} questions`);
    console.log(`🆔 Batch ID: ${batchData.batchId}`);
    
    // Reference to the batch document
    const batchRef = db.collection('test_content')
      .doc('oir')
      .collection('batches')
      .doc(batchData.batchId);
    
    // Upload batch metadata and questions
    await batchRef.set({
      batchId: batchData.batchId,
      version: batchData.version,
      totalQuestions: batchData.totalQuestions,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      questions: batchData.questions
    });
    
    console.log(`✅ Successfully uploaded batch_002 with ${batchData.totalQuestions} questions`);
    
    // Verify upload
    const doc = await batchRef.get();
    if (doc.exists) {
      const data = doc.data();
      console.log(`✅ Verification: batch_002 contains ${data.questions.length} questions`);
      
      // Show question type breakdown
      const breakdown = data.questions.reduce((acc, q) => {
        acc[q.type] = (acc[q.type] || 0) + 1;
        return acc;
      }, {});
      console.log('📊 Question Types:', breakdown);
      
      // Show difficulty breakdown
      const diffBreakdown = data.questions.reduce((acc, q) => {
        acc[q.difficulty] = (acc[q.difficulty] || 0) + 1;
        return acc;
      }, {});
      console.log('📊 Difficulty Levels:', diffBreakdown);
    } else {
      console.error('❌ Verification failed: Document not found');
    }
    
  } catch (error) {
    console.error('❌ Error uploading batch:', error);
    process.exit(1);
  }
}

// Run upload
uploadOIRBatch002()
  .then(() => {
    console.log('\n🎉 Upload complete!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Upload failed:', error);
    process.exit(1);
  });

