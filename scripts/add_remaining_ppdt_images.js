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

async function addRemainingImages() {
  try {
    console.log('📤 Adding Remaining 27 PPDT Images');
    console.log('====================================\n');

    // Get the batch document
    const batchRef = db.doc('test_content/ppdt/image_batches/batch_001');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.error('❌ Batch document not found!');
      process.exit(1);
    }

    const batchData = batchDoc.data();
    const existingImages = batchData.images || [];
    
    console.log(`📊 Current images in Firestore: ${existingImages.length}`);

    // Get all uploaded files
    const [files] = await bucket.getFiles({ prefix: 'ppdt_images/batch_001/' });
    console.log(`📦 Total images in Storage: ${files.length}`);
    
    // Extract existing image IDs
    const existingIds = new Set(existingImages.map(img => img.id));
    console.log(`\n🔍 Existing image IDs: ppdt_001 to ppdt_030`);

    // Find images that aren't in Firestore yet
    const newImages = [];
    
    for (const file of files) {
      const fileName = file.name.split('/').pop();
      
      // Extract number from filename (works for ppdt_0031.jpg, ppdt_031.jpg, etc.)
      const match = fileName.match(/ppdt_(\d+)\.jpg/);
      if (!match) continue;
      
      const number = parseInt(match[1]);
      const imageId = `ppdt_${String(number).padStart(3, '0')}`; // Format as ppdt_031
      
      // Skip if already exists in Firestore
      if (existingIds.has(imageId)) continue;
      
      // Make file public
      await file.makePublic();
      
      // Get public URL
      const publicUrl = `https://storage.googleapis.com/${bucket.name}/${file.name}`;
      
      // Create new image entry
      const newImage = {
        id: imageId,
        imageUrl: publicUrl,
        uploadedAt: new Date().toISOString(),
        isActive: true
      };
      
      newImages.push(newImage);
      console.log(`✅ Added ${imageId} (${fileName})`);
    }

    console.log(`\n📊 New images to add: ${newImages.length}`);

    if (newImages.length === 0) {
      console.log('✅ No new images to add - all images already in Firestore');
      process.exit(0);
    }

    // Sort by image ID
    newImages.sort((a, b) => a.id.localeCompare(b.id));

    // Combine with existing images
    const allImages = [...existingImages, ...newImages];
    
    // Sort all images by ID
    allImages.sort((a, b) => a.id.localeCompare(b.id));

    // Update Firestore
    await batchRef.update({
      images: allImages,
      totalImages: allImages.length,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log('\n==============================');
    console.log('📊 Final Summary');
    console.log('==============================');
    console.log(`✅ Total images in batch: ${allImages.length}`);
    console.log(`   - Previously: ${existingImages.length}`);
    console.log(`   - Added: ${newImages.length}`);
    console.log('');
    console.log('🎉 All PPDT images are now available!');
    console.log('📝 Test PPDT in the app to verify all images load correctly');
    
    // Show the range
    const firstId = allImages[0].id;
    const lastId = allImages[allImages.length - 1].id;
    console.log(`\n📋 Image range: ${firstId} to ${lastId}`);

    process.exit(0);
  } catch (error) {
    console.error('❌ Error adding images:', error.message);
    console.error(error);
    process.exit(1);
  }
}

// Run the script
addRemainingImages();

