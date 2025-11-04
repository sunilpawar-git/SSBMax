const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Initialize Firebase Admin
const serviceAccountPath = path.join(__dirname, '../.firebase/service-account.json');
const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function deleteAndReupload() {
  try {
    console.log('🗑️  Step 1: Deleting old batch_002 from Firestore...');
    
    // Delete the old batch document
    await db.collection('oir_questions').doc('batch_002').delete();
    console.log('✅ Deleted batch_002 document');
    
    // Delete all questions in batch_002
    const questionsSnapshot = await db.collection('oir_questions')
      .doc('batch_002')
      .collection('questions')
      .get();
    
    if (!questionsSnapshot.empty) {
      const batch = db.batch();
      questionsSnapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      await batch.commit();
      console.log(`✅ Deleted ${questionsSnapshot.size} questions from batch_002/questions`);
    }
    
    console.log('\n📤 Step 2: Uploading corrected batch_002...');
    
    // Load the corrected JSON
    const jsonPath = path.join(__dirname, 'oir-batch-002.json');
    const batchData = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
    
    // Upload batch metadata
    await db.collection('oir_questions').doc('batch_002').set({
      batch_id: 'batch_002',
      version: batchData.version || '1.0',
      question_count: batchData.totalQuestions || batchData.questions.length,
      uploaded_at: admin.firestore.FieldValue.serverTimestamp(),
      description: 'OIR Questions Batch 002 - Corrected'
    });
    console.log('✅ Uploaded batch_002 metadata');
    
    // Upload questions in batches of 500 (Firestore limit)
    const questions = batchData.questions;
    const batchSize = 500;
    let uploadedCount = 0;
    
    for (let i = 0; i < questions.length; i += batchSize) {
      const batch = db.batch();
      const chunk = questions.slice(i, i + batchSize);
      
      chunk.forEach(question => {
        const docRef = db.collection('oir_questions')
          .doc('batch_002')
          .collection('questions')
          .doc(question.id);
        batch.set(docRef, question);
      });
      
      await batch.commit();
      uploadedCount += chunk.length;
      console.log(`✅ Uploaded ${uploadedCount}/${questions.length} questions`);
    }
    
    console.log('\n🎉 Upload complete!');
    console.log(`📊 Total questions uploaded: ${uploadedCount}`);
    
    // Verify correctAnswerId fields
    console.log('\n🔍 Verifying correctAnswerId fields...');
    const verifySnapshot = await db.collection('oir_questions')
      .doc('batch_002')
      .collection('questions')
      .limit(10)
      .get();
    
    let allGood = true;
    verifySnapshot.docs.forEach(doc => {
      const data = doc.data();
      if (!data.correctAnswerId || data.correctAnswerId === '') {
        console.log(`❌ Question ${doc.id} has empty correctAnswerId!`);
        allGood = false;
      }
    });
    
    if (allGood) {
      console.log('✅ All sampled questions have valid correctAnswerId');
    }
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

deleteAndReupload();

