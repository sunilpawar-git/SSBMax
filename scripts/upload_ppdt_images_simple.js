#!/usr/bin/env node

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Configuration
const IMAGES_DIR = '/Users/sunil/Downloads/ppdt_images';
const PROJECT_ID = 'ssbmax-49e68';
const STORAGE_BUCKET = 'ssbmax-49e68.firebasestorage.app';
const STORAGE_PATH = 'ppdt_images/batch_001';

console.log('📤 PPDT Images Upload Script (Node.js)');
console.log('==============================');
console.log('');

// Get Firebase auth token
let authToken;
try {
  authToken = execSync('firebase login:ci --no-localhost 2>/dev/null || firebase login:ci', { 
    encoding: 'utf8',
    stdio: ['pipe', 'pipe', 'pipe']
  }).trim();
} catch (error) {
  // Try to get token from current login
  try {
    const loginList = execSync('firebase login:list', { encoding: 'utf8' });
    console.log('✅ Firebase CLI authenticated');
  } catch {
    console.error('❌ Not logged in to Firebase');
    console.log('📝 Run: firebase login');
    process.exit(1);
  }
}

// Check if we can use the google-auth-library
let initialized = false;

// Try to load service account key
const keyPath = path.join(__dirname, '..', 'firebase-admin-key.json');

if (!fs.existsSync(keyPath)) {
  console.error('❌ Service account key not found:', keyPath);
  console.log('');
  console.log('📝 To upload images, you need a service account key:');
  console.log('  1. Go to: https://console.firebase.google.com/project/ssbmax-56e16/settings/serviceaccounts/adminsdk');
  console.log('  2. Click "Generate New Private Key"');
  console.log('  3. Save as firebase-admin-key.json in project root');
  console.log('');
  process.exit(1);
}

try {
  const serviceAccount = require(keyPath);
  
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    storageBucket: STORAGE_BUCKET
  });
  
  console.log('✅ Firebase Admin initialized');
  console.log(`📦 Using bucket: ${STORAGE_BUCKET}`);
  
  initialized = true;
} catch (error) {
  console.error('❌ Failed to initialize Firebase Admin:', error.message);
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
        cacheControl: 'public, max-age=31536000',
      },
      public: true
    });
    return { success: true, filename };
  } catch (error) {
    return { success: false, filename, error: error.message };
  }
}

// Upload all images
async function uploadAll() {
  console.log('📤 Starting upload...');
  console.log('');
  
  let uploaded = 0;
  let failed = 0;
  const failedFiles = [];
  
  for (const file of files) {
    process.stdout.write(`⬆️  Uploading ${file}... `);
    
    const result = await uploadImage(file);
    
    if (result.success) {
      console.log('✅');
      uploaded++;
    } else {
      console.log(`❌`);
      console.log(`   Error: ${result.error}`);
      failed++;
      failedFiles.push({ file, error: result.error });
    }
  }
  
  console.log('');
  console.log('==============================');
  console.log('📊 Upload Summary');
  console.log('==============================');
  console.log(`✅ Uploaded: ${uploaded} images`);
  console.log(`❌ Failed: ${failed} images`);
  console.log('');
  
  if (failedFiles.length > 0 && failedFiles.length <= 5) {
    console.log('Failed files:');
    failedFiles.forEach(({ file, error }) => {
      console.log(`  - ${file}: ${error}`);
    });
    console.log('');
  }
  
  if (uploaded > 0) {
    console.log('🎉 Upload complete!');
    console.log('');
    console.log('📝 Next steps:');
    console.log('1. Run: node scripts/update_ppdt_image_urls.js');
    console.log('2. Test PPDT in app');
  } else {
    console.log('❌ No images were uploaded');
  }
  
  process.exit(failed > 0 ? 1 : 0);
}

// Run upload
uploadAll().catch(error => {
  console.error('❌ Upload failed:', error.message);
  process.exit(1);
});

