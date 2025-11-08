#!/usr/bin/env node

const admin = require('firebase-admin');
const path = require('path');

// Configuration
const keyPath = path.join(__dirname, '..', 'firebase-admin-key.json');
const PROJECT_ID = 'ssbmax-49e68';
const STORAGE_BUCKET = 'ssbmax-49e68.firebasestorage.app';

// Initialize Firebase Admin
const serviceAccount = require(keyPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: STORAGE_BUCKET
});

const db = admin.firestore();
const bucket = admin.storage().bucket();

async function updateImageUrls() {
  try {
    console.log('🔄 Updating PPDT Image URLs in Firestore');
    console.log('==========================================\n');

    // Get the batch document
    const batchRef = db.doc('test_content/ppdt/image_batches/batch_001');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.log('⚠️  Batch document not found in Firestore');
      console.log('📝 This might be expected if PPDT data structure is different');
      console.log('');
      console.log('Uploaded images are available at:');
      console.log(`   gs://${STORAGE_BUCKET}/ppdt_images/batch_001/`);
      process.exit(0);
    }

    const batchData = batchDoc.data();
    const images = batchData.images;

    console.log(`📊 Found ${images.length} images in Firestore\n`);

    // List all uploaded files to see what we have
    const [files] = await bucket.getFiles({ prefix: 'ppdt_images/batch_001/' });
    console.log(`📦 Found ${files.length} uploaded files in Storage\n`);

    // Create a map of available files
    const availableFiles = new Map();
    files.forEach(file => {
      const fileName = file.name.split('/').pop();
      availableFiles.set(fileName, file);
    });

    console.log('🔄 Updating image URLs...\n');

    // Update each image URL
    let updated = 0;
    let notFound = 0;

    for (let i = 0; i < images.length; i++) {
      const image = images[i];
      const imageId = image.id;
      
      // Try different filename formats
      const possibleNames = [
        `${imageId}.jpg`,           // ppdt_001.jpg
        `ppdt_${imageId.split('_')[1]}.jpg`, // Extract number and recreate
      ];

      // Check what files match this image
      let matchedFile = null;
      for (const name of possibleNames) {
        if (availableFiles.has(name)) {
          matchedFile = availableFiles.get(name);
          break;
        }
      }

      // Also check if we can find by partial match
      if (!matchedFile) {
        const imageNumber = imageId.replace('ppdt_', '');
        for (const [fileName, file] of availableFiles) {
          if (fileName.includes(imageNumber)) {
            matchedFile = file;
            break;
          }
        }
      }

      if (matchedFile) {
        try {
          // Make file public
          await matchedFile.makePublic();

          // Get public URL
          const publicUrl = `https://storage.googleapis.com/${bucket.name}/${matchedFile.name}`;
          
          // Update the image URL in the array
          images[i].imageUrl = publicUrl;
          
          console.log(`✅ ${imageId} → ${matchedFile.name.split('/').pop()}`);
          updated++;
        } catch (error) {
          console.error(`❌ Error updating ${imageId}:`, error.message);
        }
      } else {
        console.log(`⚠️  ${imageId}: No matching file found`);
        notFound++;
      }
    }

    if (updated > 0) {
      // Save updated batch back to Firestore
      await batchRef.update({ images: images });
      console.log('\n✅ Firestore updated successfully!');
    }

    console.log('\n==============================');
    console.log('📊 Update Summary');
    console.log('==============================');
    console.log(`✅ Updated: ${updated} images`);
    console.log(`⚠️  Not found: ${notFound} images`);
    console.log('');

    if (updated > 0) {
      console.log('🎉 PPDT images are ready!');
      console.log('📝 Test PPDT in the app to verify images load correctly');
    }

    process.exit(0);
  } catch (error) {
    console.error('❌ Update failed:', error.message);
    console.error(error);
    process.exit(1);
  }
}

// Run the update
updateImageUrls();

