#!/usr/bin/env node

/**
 * Make all PPDT images publicly accessible in Firebase Storage
 * 
 * This script ensures all images in ppdt_images/batch_001/ have public read access
 * without requiring authentication or App Check tokens.
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: 'ssbmax-49e68.firebasestorage.app'
});

const bucket = admin.storage().bucket();

async function makeImagesPublic() {
  try {
    console.log('🔓 Making PPDT images publicly accessible...\n');

    const [files] = await bucket.getFiles({
      prefix: 'ppdt_images/batch_001/'
    });

    console.log(`📦 Found ${files.length} files in ppdt_images/batch_001/\n`);

    let successCount = 0;
    let failCount = 0;

    for (const file of files) {
      try {
        // Make file publicly readable
        await file.makePublic();
        console.log(`✅ ${file.name} → Public`);
        successCount++;
      } catch (error) {
        console.error(`❌ ${file.name} → Failed: ${error.message}`);
        failCount++;
      }
    }

    console.log(`\n📊 Results:`);
    console.log(`   ✅ Success: ${successCount}`);
    console.log(`   ❌ Failed: ${failCount}`);
    console.log(`\n🎉 All PPDT images are now publicly accessible!`);
    console.log(`\n🔗 Test URL: https://storage.googleapis.com/${bucket.name}/ppdt_images/batch_001/ppdt_001.jpg`);
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

makeImagesPublic();

