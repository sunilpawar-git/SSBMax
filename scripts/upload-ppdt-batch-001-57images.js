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
    console.log('📤 Starting PPDT batch_001 upload (57 images)...\n');

    // Read the JSON file
    const batchData = JSON.parse(
      fs.readFileSync(path.join(__dirname, 'ppdt_batch_001_57images.json'), 'utf8')
    );

    console.log(`📊 Uploading ${batchData.images.length} image entries to Firestore...`);

    // Upload to Firestore: test_content/ppdt/image_batches/batch_001
    const batchRef = db.doc(`test_content/ppdt/image_batches/${batchData.batch_id}`);
    
    await batchRef.set({
      batch_id: batchData.batch_id,
      version: batchData.version,
      totalImages: batchData.images.length,
      uploaded_at: admin.firestore.Timestamp.now(),
      description: batchData.description,
      images: batchData.images
    });

    console.log('✅ Batch uploaded successfully!');

    // Update metadata
    const metaRef = db.doc('test_content/ppdt/meta/overview');
    const metaDoc = await metaRef.get();

    if (metaDoc.exists) {
      const currentData = metaDoc.data();
      const updatedBatches = currentData.available_batches || [];
      
      if (!updatedBatches.includes(batchData.batch_id)) {
        updatedBatches.push(batchData.batch_id);
        updatedBatches.sort();
      }

      await metaRef.update({
        total_images: batchData.images.length,
        available_batches: updatedBatches,
        last_updated: admin.firestore.Timestamp.now()
      });
      console.log('✅ Metadata updated!');
    } else {
      await metaRef.set({
        total_images: batchData.images.length,
        available_batches: [batchData.batch_id],
        last_updated: admin.firestore.Timestamp.now(),
        content_type: 'ppdt_images'
      });
      console.log('✅ Metadata created!');
    }

    console.log('\n🎉 PPDT batch_001 upload complete!');
    console.log(`📊 Total images: ${batchData.images.length}`);
    console.log('\n📝 Next step: Run update_ppdt_image_urls.js to fetch real Storage URLs');
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Upload failed:', error);
    process.exit(1);
  }
}

// Run the upload
uploadPPDTBatch001();

