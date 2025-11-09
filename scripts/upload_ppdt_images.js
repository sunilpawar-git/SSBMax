#!/usr/bin/env node

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Configuration
const IMAGES_DIR = '/Users/sunil/Downloads/ppdt_images';
const STORAGE_PATH = 'ppdt_images/batch_001';

console.log('📤 PPDT Images Upload Script (Node.js)');
console.log('==============================');
console.log('');

// Initialize Firebase Admin
try {
  // Try to use existing credentials
  const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');
  
  if (fs.existsSync(serviceAccountPath)) {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      storageBucket: 'ssbmax-56e16.appspot.com'
    });
  } else {
    // Use application default credentials
    admin.initializeApp({
      storageBucket: 'ssbmax-56e16.appspot.com'
    });
  }
  console.log('✅ Firebase Admin initialized');
} catch (error) {
  console.error('❌ Failed to initialize Firebase Admin:', error.message);
  console.log('');
  console.log('📝 Please ensure you have:');
  console.log('   1. Downloaded service account key from Firebase Console');
  console.log('   2. Saved it as serviceAccountKey.json in project root');
  console.log('   OR');
  console.log('   3. Set up Application Default Credentials (gcloud auth application-default login)');
  process.exit(1);
}

const bucket = admin.storage().bucket();

// Check if images directory exists
if (!fs.existsSync(IMAGES_DIR)) {
  console.error(`❌ Images directory not found: ${IMAGES_DIR}`);
  process.exit(1);
}

// Get all PPDT images
const files = fs.readdirSync(IMAGES_DIR)
  .filter(file => file.startsWith('ppdt_') && file.endsWith('.jpg'))
  .sort();

console.log(`📊 Found ${files.length} PPDT images`);
console.log('');

if (files.length === 0) {
  console.log('⚠️  No PPDT images found!');
  process.exit(1);
}

// Upload function
async function uploadImage(filename) {
  const localPath = path.join(IMAGES_DIR, filename);
  const storagePath = `${STORAGE_PATH}/${filename}`;
  
  try {
    await bucket.upload(localPath, {
      destination: storagePath,
      metadata: {
        contentType: 'image/jpeg',
        metadata: {
          firebaseStorageDownloadTokens: generateUUID()
        }
      }
    });
    return { success: true, filename };
  } catch (error) {
    return { success: false, filename, error: error.message };
  }
}

// Generate UUID for download token
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// Upload all images
async function uploadAll() {
  console.log('📤 Starting upload...');
  console.log('');
  
  let uploaded = 0;
  let failed = 0;
  
  for (const file of files) {
    process.stdout.write(`⬆️  Uploading ${file}... `);
    
    const result = await uploadImage(file);
    
    if (result.success) {
      console.log('✅');
      uploaded++;
    } else {
      console.log(`❌ ${result.error}`);
      failed++;
    }
  }
  
  console.log('');
  console.log('==============================');
  console.log('📊 Upload Summary');
  console.log('==============================');
  console.log(`✅ Uploaded: ${uploaded} images`);
  console.log(`❌ Failed: ${failed} images`);
  console.log('');
  
  if (uploaded > 0) {
    console.log('🎉 Upload complete!');
    console.log('');
    console.log('📝 Next steps:');
    console.log('1. Update Storage rules to allow public read');
    console.log('2. Run: node scripts/update_ppdt_image_urls.js');
    console.log('3. Test PPDT in app');
  } else {
    console.log('❌ No images were uploaded');
  }
  
  process.exit(failed > 0 ? 1 : 0);
}

// Run upload
uploadAll().catch(error => {
  console.error('❌ Upload failed:', error);
  process.exit(1);
});



