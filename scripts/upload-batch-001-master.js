const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin (only once)
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Upload WAT batch_001
async function uploadWATBatch001() {
  const batchData = JSON.parse(
    fs.readFileSync(path.join(__dirname, 'wat-batch-001.json'), 'utf8')
  );

  console.log('📤 Uploading WAT batch_001...');
  
  const batchRef = db.doc(`test_content/wat/word_batches/${batchData.batch_id}`);
  await batchRef.set({
    batch_id: batchData.batch_id,
    version: batchData.version,
    word_count: batchData.word_count,
    uploaded_at: admin.firestore.Timestamp.now(),
    description: batchData.description,
    words: batchData.words
  });

  // Update metadata
  const metaRef = db.doc('test_content/wat/meta/overview');
  const metaDoc = await metaRef.get();

  if (metaDoc.exists) {
    const currentData = metaDoc.data();
    const updatedBatches = currentData.available_batches || [];
    if (!updatedBatches.includes(batchData.batch_id)) {
      updatedBatches.push(batchData.batch_id);
      updatedBatches.sort();
    }

    await metaRef.update({
      total_words: (currentData.total_words || 0) + batchData.word_count,
      available_batches: updatedBatches,
      last_updated: admin.firestore.Timestamp.now()
    });
  } else {
    await metaRef.set({
      total_words: batchData.word_count,
      available_batches: [batchData.batch_id],
      last_updated: admin.firestore.Timestamp.now(),
      content_type: 'wat_words'
    });
  }

  console.log(`✅ WAT batch_001: ${batchData.word_count} words uploaded\n`);
}

// Upload SRT batch_001
async function uploadSRTBatch001() {
  const batchData = JSON.parse(
    fs.readFileSync(path.join(__dirname, 'srt-batch-001.json'), 'utf8')
  );

  console.log('📤 Uploading SRT batch_001...');
  
  const batchRef = db.doc(`test_content/srt/situation_batches/${batchData.batch_id}`);
  await batchRef.set({
    batch_id: batchData.batch_id,
    version: batchData.version,
    situation_count: batchData.situation_count,
    uploaded_at: admin.firestore.Timestamp.now(),
    description: batchData.description,
    situations: batchData.situations
  });

  // Update metadata
  const metaRef = db.doc('test_content/srt/meta/overview');
  const metaDoc = await metaRef.get();

  if (metaDoc.exists) {
    const currentData = metaDoc.data();
    const updatedBatches = currentData.available_batches || [];
    if (!updatedBatches.includes(batchData.batch_id)) {
      updatedBatches.push(batchData.batch_id);
      updatedBatches.sort();
    }

    await metaRef.update({
      total_situations: (currentData.total_situations || 0) + batchData.situation_count,
      available_batches: updatedBatches,
      last_updated: admin.firestore.Timestamp.now()
    });
  } else {
    await metaRef.set({
      total_situations: batchData.situation_count,
      available_batches: [batchData.batch_id],
      last_updated: admin.firestore.Timestamp.now(),
      content_type: 'srt_situations'
    });
  }

  console.log(`✅ SRT batch_001: ${batchData.situation_count} situations uploaded\n`);
}

// Main upload function
async function uploadAll() {
  try {
    console.log('\n🚀 BATCH_001 MASTER UPLOAD');
    console.log('============================================================\n');

    await uploadWATBatch001();
    await uploadSRTBatch001();

    console.log('============================================================');
    console.log('🎉 ALL BATCH_001 UPLOADS COMPLETE!\n');
    console.log('📊 Summary:');
    console.log('   ✅ WAT: 60 words');
    console.log('   ✅ SRT: 60 situations');
    console.log('   ✅ Total: 120 items\n');

  } catch (error) {
    console.error('❌ Upload failed:', error);
    throw error;
  }
}

// Run the upload
uploadAll()
  .then(() => {
    console.log('✨ Upload process completed successfully!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Upload process failed:', error);
    process.exit(1);
  });

