const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function uploadPPDTBatch001() {
  try {
    console.log('📤 Starting PPDT batch_001 upload...\n');

    // Read the batch file
    const batchData = JSON.parse(
      fs.readFileSync(path.join(__dirname, 'ppdt_batch_001.json'), 'utf8')
    );

    console.log(`📊 Batch Info:`);
    console.log(`   ID: ${batchData.batchId}`);
    console.log(`   Version: ${batchData.version}`);
    console.log(`   Image Count: ${batchData.totalImages}`);
    console.log(`   Description: ${batchData.description}\n`);

    // Upload to test_content/ppdt/image_batches/batch_001
    const batchRef = db.doc(`test_content/ppdt/image_batches/${batchData.batchId}`);
    
    await batchRef.set({
      batchId: batchData.batchId,
      version: batchData.version,
      totalImages: batchData.totalImages,
      uploaded_at: admin.firestore.Timestamp.now(),
      description: batchData.description,
      createdAt: batchData.createdAt,
      images: batchData.images
    });

    console.log('✅ Batch uploaded successfully!');

    // Update or create metadata
    const metaRef = db.doc('test_content/ppdt/meta/overview');
    const metaDoc = await metaRef.get();

    if (metaDoc.exists) {
      // Update existing metadata
      const currentData = metaDoc.data();
      const updatedBatches = currentData.available_batches || [];
      if (!updatedBatches.includes(batchData.batchId)) {
        updatedBatches.push(batchData.batchId);
        updatedBatches.sort(); // Keep batches in order
      }

      await metaRef.update({
        total_images: (currentData.total_images || 0) + batchData.totalImages,
        available_batches: updatedBatches,
        last_updated: admin.firestore.Timestamp.now()
      });

      console.log('✅ Metadata updated!');
    } else {
      // Create new metadata
      await metaRef.set({
        total_images: batchData.totalImages,
        available_batches: [batchData.batchId],
        last_updated: admin.firestore.Timestamp.now(),
        content_type: 'ppdt_images'
      });

      console.log('✅ Metadata created!');
    }

    console.log('\n🎉 PPDT batch_001 upload complete!\n');
    console.log('📊 Summary:');
    console.log(`   ✅ ${batchData.totalImages} images uploaded`);
    console.log(`   ✅ Batch: ${batchData.batchId}`);
    console.log(`   ✅ Version: ${batchData.version}\n`);
    console.log('📝 Next steps:');
    console.log('   1. Upload actual PPDT images to Firebase Storage');
    console.log('   2. Update imageUrl fields in Firestore with Storage URLs\n');

  } catch (error) {
    console.error('❌ Upload failed:', error);
    throw error;
  }
}

// Run the upload
uploadPPDTBatch001()
  .then(() => {
    console.log('✨ Upload process completed successfully!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Upload process failed:', error);
    process.exit(1);
  });

