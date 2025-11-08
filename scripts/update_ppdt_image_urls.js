const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: 'ssbmax-49e68.firebasestorage.app'
});

const db = admin.firestore();
const bucket = admin.storage().bucket();

async function updateImageUrls() {
  try {
    console.log('🔄 Updating PPDT image URLs...\n');

    // Get the batch document
    const batchRef = db.doc('test_content/ppdt/image_batches/batch_001');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      throw new Error('Batch document not found!');
    }

    const batchData = batchDoc.data();
    const images = batchData.images;

    console.log(`📊 Found ${images.length} images to update\n`);

    // Update each image URL
    for (let i = 0; i < images.length; i++) {
      const image = images[i];
      const imageId = image.id;
      const fileName = `${imageId}.jpg`;  // Adjust extension if using .png
      const filePath = `ppdt_images/batch_001/${fileName}`;

      try {
        // Get the file from Storage
        const file = bucket.file(filePath);
        
        // Check if file exists
        const [exists] = await file.exists();
        if (!exists) {
          console.log(`⚠️  Warning: ${fileName} not found in Storage`);
          continue;
        }

        // Make file public (if not already)
        await file.makePublic();

        // Get public URL
        const publicUrl = `https://storage.googleapis.com/${bucket.name}/${filePath}`;
        
        // Update the image URL in the array
        images[i].imageUrl = publicUrl;
        
        console.log(`✅ Updated ${imageId}: ${publicUrl}`);
      } catch (error) {
        console.error(`❌ Error updating ${imageId}:`, error.message);
      }
    }

    // Save updated batch back to Firestore
    await batchRef.update({ images: images });

    console.log('\n🎉 All image URLs updated successfully!');
    console.log('\n📝 Next: Test PPDT in app to verify images load correctly');
    process.exit(0);
  } catch (error) {
    console.error('❌ Update failed:', error);
    process.exit(1);
  }
}

// Run the update
updateImageUrls();
