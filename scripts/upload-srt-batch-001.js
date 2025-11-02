const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function uploadSRTBatch001() {
  try {
    console.log('📤 Starting SRT batch_001 upload...\n');

    // Read the batch file
    const batchData = JSON.parse(
      fs.readFileSync(path.join(__dirname, 'srt-batch-001.json'), 'utf8')
    );

    console.log(`📊 Batch Info:`);
    console.log(`   ID: ${batchData.batch_id}`);
    console.log(`   Version: ${batchData.version}`);
    console.log(`   Situation Count: ${batchData.situation_count}`);
    console.log(`   Description: ${batchData.description}\n`);

    // Upload to test_content/srt/situation_batches/batch_001
    const batchRef = db.doc(`test_content/srt/situation_batches/${batchData.batch_id}`);
    
    await batchRef.set({
      batch_id: batchData.batch_id,
      version: batchData.version,
      situation_count: batchData.situation_count,
      uploaded_at: admin.firestore.Timestamp.now(),
      description: batchData.description,
      situations: batchData.situations
    });

    console.log('✅ Batch uploaded successfully!');

    // Update or create metadata
    const metaRef = db.doc('test_content/srt/meta/overview');
    const metaDoc = await metaRef.get();

    if (metaDoc.exists) {
      // Update existing metadata
      const currentData = metaDoc.data();
      const updatedBatches = currentData.available_batches || [];
      if (!updatedBatches.includes(batchData.batch_id)) {
        updatedBatches.push(batchData.batch_id);
        updatedBatches.sort(); // Keep batches in order
      }

      await metaRef.update({
        total_situations: (currentData.total_situations || 0) + batchData.situation_count,
        available_batches: updatedBatches,
        last_updated: admin.firestore.Timestamp.now()
      });

      console.log('✅ Metadata updated!');
    } else {
      // Create new metadata
      await metaRef.set({
        total_situations: batchData.situation_count,
        available_batches: [batchData.batch_id],
        last_updated: admin.firestore.Timestamp.now(),
        content_type: 'srt_situations'
      });

      console.log('✅ Metadata created!');
    }

    console.log('\n🎉 SRT batch_001 upload complete!\n');
    console.log('📊 Summary:');
    console.log(`   ✅ ${batchData.situation_count} situations uploaded`);
    console.log(`   ✅ Batch: ${batchData.batch_id}`);
    console.log(`   ✅ Version: ${batchData.version}\n`);

  } catch (error) {
    console.error('❌ Upload failed:', error);
    throw error;
  }
}

// Run the upload
uploadSRTBatch001()
  .then(() => {
    console.log('✨ Upload process completed successfully!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Upload process failed:', error);
    process.exit(1);
  });

