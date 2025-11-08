#!/usr/bin/env node

const admin = require('firebase-admin');
const path = require('path');

const keyPath = path.join(__dirname, '..', 'firebase-admin-key.json');
const serviceAccount = require(keyPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

console.log('🔍 Checking available buckets...\n');

// Try to list buckets
const storage = admin.storage();

// Try different bucket names
const bucketsToTry = [
  'ssbmax-49e68.appspot.com',
  'ssbmax-49e68.firebasestorage.app',
  'ssbmax.appspot.com',
  'ssbmax.firebasestorage.app'
];

async function checkBucket(bucketName) {
  try {
    const bucket = storage.bucket(bucketName);
    const [exists] = await bucket.exists();
    console.log(`✅ ${bucketName}: ${exists ? 'EXISTS' : 'DOES NOT EXIST'}`);
    
    if (exists) {
      const [files] = await bucket.getFiles({ maxResults: 1 });
      console.log(`   📁 File count check: ${files.length > 0 ? 'Has files' : 'Empty'}`);
    }
  } catch (error) {
    console.log(`❌ ${bucketName}: ERROR - ${error.message}`);
  }
}

async function main() {
  for (const bucketName of bucketsToTry) {
    await checkBucket(bucketName);
  }
  
  console.log('\n💡 If all buckets show "DOES NOT EXIST", you need to:');
  console.log('   1. Go to Firebase Console Storage');
  console.log('   2. Click "Get Started" or create a bucket');
  console.log('   3. Wait a few minutes for it to provision');
  process.exit(0);
}

main().catch(console.error);

