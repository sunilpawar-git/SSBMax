#!/usr/bin/env node

/**
 * Update TAT image URLs in Firestore with real Firebase Storage URLs
 * Fetches images from Storage, makes them public, and updates Firestore
 */

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
    console.log('🔄 Updating TAT image URLs in Firestore...\n');

    // Get the batch document
    const batchRef = db.doc('test_content/tat/image_batches/batch_001');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      throw new Error('Batch document not found! Run create_tat_firestore_metadata.js first.');
    }

    const batchData = batchDoc.data();
    const images = batchData.images;

    console.log(`📊 Found ${images.length} images to update\n`);

    let updatedCount = 0;
    let notFoundCount = 0;

    // Update each image URL
    for (let i = 0; i < images.length; i++) {
      const image = images[i];
      const imageId = image.id;
      const fileName = `${imageId}.jpg`;
      const filePath = `tat_images/batch_001/${fileName}`;

      try {
        // Get the file from Storage
        const file = bucket.file(filePath);
        
        // Check if file exists
        const [exists] = await file.exists();
        if (!exists) {
          console.log(`⚠️  ${fileName} not found in Storage (will keep placeholder)`);
          notFoundCount++;
          continue;
        }

        // Make file public (if not already)
        try {
          await file.makePublic();
        } catch (publicError) {
          // Might already be public, continue
        }

        // Get public URL
        const publicUrl = `https://storage.googleapis.com/${bucket.name}/${filePath}`;
        
        // Update the image URL in the array
        images[i].imageUrl = publicUrl;
        
        updatedCount++;
        console.log(`✅ Updated ${imageId}: ${publicUrl}`);
      } catch (error) {
        console.error(`❌ Error updating ${imageId}:`, error.message);
      }
    }

    // Save updated batch back to Firestore
    await batchRef.update({ 
      images: images,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log('\n' + '='.repeat(60));
    console.log('📊 UPDATE SUMMARY');
    console.log('='.repeat(60));
    console.log(`✅ Updated: ${updatedCount} images`);
    console.log(`⚠️  Not found: ${notFoundCount} images`);
    console.log(`📝 Total: ${images.length} images`);

    console.log('\n✅ All image URLs updated in Firestore!');
    console.log('\n📝 Next Steps:');
    console.log('1. Verify in Firestore Console:');
    console.log('   https://console.firebase.google.com/project/ssbmax-49e68/firestore');
    console.log('   → test_content → tat → image_batches → batch_001');
    console.log('2. Test TAT in app to verify images load correctly');
    console.log('3. Deploy storage rules: firebase deploy --only storage\n');

    process.exit(0);
  } catch (error) {
    console.error('❌ Update failed:', error);
    process.exit(1);
  }
}

// Run the update
updateImageUrls();

