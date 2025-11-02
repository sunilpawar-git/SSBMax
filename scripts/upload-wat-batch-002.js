#!/usr/bin/env node

/**
 * Upload WAT batch_002 (40 words) to Firestore
 * 
 * Usage: node upload-wat-batch-002.js
 * 
 * Prerequisites:
 * 1. Firebase Admin SDK initialized
 * 2. Service account key configured
 * 3. wat-batch-002.json file in same directory
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin SDK
const serviceAccountPath = path.join(__dirname, '../.firebase/service-account.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error('❌ Error: Firebase service account key not found');
  console.error(`   Expected at: ${serviceAccountPath}`);
  process.exit(1);
}

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('✅ Firebase Admin SDK initialized');
} catch (error) {
  console.error('❌ Error initializing Firebase:', error.message);
  process.exit(1);
}

const db = admin.firestore();

/**
 * Upload WAT batch_002 to Firestore
 */
async function uploadWATBatch() {
  try {
    console.log('\n📚 Loading WAT batch_002 from file...');
    
    // Read batch file
    const batchPath = path.join(__dirname, 'wat-batch-002.json');
    if (!fs.existsSync(batchPath)) {
      throw new Error('wat-batch-002.json not found in scripts directory');
    }
    
    const batchData = JSON.parse(fs.readFileSync(batchPath, 'utf8'));
    console.log(`   Loaded: ${batchData.metadata.total_words} words`);
    console.log(`   Version: ${batchData.metadata.version}`);
    console.log(`   Batch ID: ${batchData.batch_id}`);
    
    // Upload batch to Firestore
    console.log('\n📦 Uploading WAT batch_002...');
    const batchRef = db.collection('test_content')
      .doc('wat')
      .collection('word_batches')
      .doc(batchData.batch_id);
    
    await batchRef.set({
      batch_id: batchData.batch_id,
      version: batchData.metadata.version,
      word_count: batchData.metadata.total_words,
      words: batchData.words,
      uploaded_at: admin.firestore.FieldValue.serverTimestamp()
    });
    
    console.log(`   ✅ ${batchData.batch_id}: ${batchData.metadata.total_words} words uploaded`);
    
    // Update metadata (aggregate)
    console.log('\n📝 Updating metadata...');
    const metaRef = db.collection('test_content').doc('wat').collection('meta').doc('config');
    
    // Get existing metadata
    const metaDoc = await metaRef.get();
    const existingMeta = metaDoc.exists ? metaDoc.data() : {};
    
    const updatedMeta = {
      total_words: (existingMeta.total_words || 0) + batchData.metadata.total_words,
      version: batchData.metadata.version,
      last_updated: admin.firestore.FieldValue.serverTimestamp(),
      batches: (existingMeta.batches || 0) + 1,
      distribution: {
        ...existingMeta.distribution,
        ...batchData.metadata.distribution
      },
      description: 'WAT word repository for progressive caching'
    };
    
    await metaRef.set(updatedMeta, { merge: true });
    console.log('   ✅ Metadata updated');
    
    // Verify upload
    console.log('\n🔍 Verifying upload...');
    const uploadedBatch = await batchRef.get();
    
    if (!uploadedBatch.exists) {
      throw new Error('Batch verification failed - document not found');
    }
    
    const verifiedData = uploadedBatch.data();
    console.log(`   Batch ID: ${verifiedData.batch_id}`);
    console.log(`   Word count: ${verifiedData.word_count}`);
    console.log(`   Words verified: ${verifiedData.words.length}`);
    
    if (verifiedData.words.length === batchData.metadata.total_words) {
      console.log('\n✅ SUCCESS! All words uploaded and verified');
    } else {
      console.warn('\n⚠️  Warning: Word count mismatch');
      console.warn(`   Expected: ${batchData.metadata.total_words}`);
      console.warn(`   Found: ${verifiedData.words.length}`);
    }
    
    // Display sample words
    console.log('\n📄 Sample words from Firestore:');
    const sampleWords = verifiedData.words.slice(0, 5);
    sampleWords.forEach(word => {
      console.log(`   - ${word.word} (${word.category}, ${word.difficulty})`);
    });
    
    console.log('\n🎉 Upload complete!');
    console.log(`\n📊 Summary:`);
    console.log(`   Total words in batch: ${batchData.metadata.total_words}`);
    console.log(`   Categories: ${Object.keys(batchData.metadata.distribution).length}`);
    console.log(`   Firestore path: test_content/wat/word_batches/${batchData.batch_id}`);
    
  } catch (error) {
    console.error('\n❌ Error uploading WAT batch:', error);
    throw error;
  }
}

// Main execution
(async () => {
  try {
    await uploadWATBatch();
    process.exit(0);
  } catch (error) {
    console.error('\n💥 Fatal error:', error);
    process.exit(1);
  }
})();

