#!/usr/bin/env node

const admin = require('firebase-admin');
const path = require('path');

const keyPath = path.join(__dirname, '..', 'firebase-admin-key.json');
const serviceAccount = require(keyPath);

const PROJECT_ID = 'ssbmax-49e68';
const BUCKET_NAME = `${PROJECT_ID}.appspot.com`;

console.log('🪣 Creating Firebase Storage Bucket');
console.log('=====================================\n');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: BUCKET_NAME
});

async function createBucket() {
  try {
    const storage = admin.storage();
    const bucket = storage.bucket();
    
    console.log(`📦 Attempting to access/create bucket: ${BUCKET_NAME}`);
    
    // Try to make the bucket public (this also initializes it)
    await bucket.makePublic();
    
    console.log('✅ Bucket created/accessed successfully!');
    console.log(`\n🎉 You can now upload images to: ${BUCKET_NAME}`);
    
    // Try uploading a test file
    const testFile = Buffer.from('Test file created by SSBMax setup');
    const file = bucket.file('test/setup_test.txt');
    
    await file.save(testFile, {
      metadata: {
        contentType: 'text/plain'
      }
    });
    
    console.log('✅ Test file uploaded successfully');
    console.log('\n📝 Next step: Run node scripts/upload_ppdt_images_simple.js');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.log('\n💡 The bucket might not be initialized yet.');
    console.log('Please go to Firebase Console and:');
    console.log('1. Navigate to Storage');
    console.log('2. Click "Get Started"');
    console.log('3. Choose a location and security rules');
    console.log('4. Wait 2-3 minutes for provisioning');
    console.log('5. Then run this script again');
  }
  
  process.exit(0);
}

createBucket();



