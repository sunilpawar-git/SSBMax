const admin = require('firebase-admin');

const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: 'ssbmax-49e68.firebasestorage.app'
});

const db = admin.firestore();
const bucket = admin.storage().bucket();

async function updateImageUrls() {
  try {
    console.log('🔄 Updating PPDT image URLs with actual Storage files...\n');

    // Get all files from Storage
    const [files] = await bucket.getFiles({ prefix: 'ppdt_images/batch_001/' });
    console.log(`📦 Found ${files.length} files in Storage\n`);

    // Create a map of ID -> Storage file
    const storageMap = {};
    files.forEach(file => {
      const fileName = file.name.split('/').pop(); // Get filename without path
      const fileId = fileName.replace('.jpg', '').replace('.png', ''); // Remove extension
      storageMap[fileId] = file;
    });

    console.log('📋 Storage files available:');
    Object.keys(storageMap).sort().forEach(id => console.log(`   - ${id}`));
    console.log('');

    // Get the batch document
    const batchRef = db.doc('test_content/ppdt/image_batches/batch_001');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.log('❌ Firestore batch document not found!');
      console.log('   Run: node upload-ppdt-batch-001-57images.js first');
      process.exit(1);
    }

    const batchData = batchDoc.data();
    const images = batchData.images;
    console.log(`📊 Firestore has ${images.length} image entries\n`);

    let updatedCount = 0;
    let notFoundCount = 0;

    // Update each image URL by matching with actual Storage files
    for (let i = 0; i < images.length; i++) {
      const image = images[i];
      const imageId = image.id; // e.g., "ppdt_001"

      // Try to find matching file in Storage (handle different naming patterns)
      let file = storageMap[imageId]; // Exact match
      
      // Try with leading zeros removed/added
      if (!file) {
        const numPart = imageId.replace('ppdt_', '');
        const paddedId = `ppdt_${numPart.padStart(4, '0')}`;
        file = storageMap[paddedId];
      }
      
      if (file) {
        // Make file public and get URL
        await file.makePublic();
        const publicUrl = `https://storage.googleapis.com/${bucket.name}/${file.name}`;
        images[i].imageUrl = publicUrl;
        console.log(`✅ ${imageId} → ${file.name.split('/').pop()}`);
        updatedCount++;
      } else {
        console.log(`⚠️  ${imageId} → NOT FOUND in Storage`);
        notFoundCount++;
      }
    }

    // Save updated batch back to Firestore
    await batchRef.update({ 
      images: images,
      last_url_update: admin.firestore.Timestamp.now()
    });

    console.log(`\n✅ Updated ${updatedCount} image URLs`);
    if (notFoundCount > 0) {
      console.log(`⚠️  ${notFoundCount} images not found in Storage`);
    }
    console.log('\n🎉 Firestore updated successfully!');
    console.log('\n📝 Next: Build app and test PPDT to verify images load');
    process.exit(0);
  } catch (error) {
    console.error('❌ Update failed:', error);
    process.exit(1);
  }
}

updateImageUrls();

