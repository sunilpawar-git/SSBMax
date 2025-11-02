#!/usr/bin/env node

/**
 * Upload SRT batch_002 (30 situations) to Firestore
 * 
 * Usage: node upload-srt-batch-002.js
 * 
 * Prerequisites:
 * 1. Firebase Admin SDK initialized
 * 2. Service account key configured
 * 3. srt-batch-002.json file in same directory
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
 * Upload SRT batch_002 to Firestore
 */
async function uploadSRTBatch() {
  try {
    console.log('\n📚 Loading SRT batch_002 from file...');
    
    // Read batch file
    const batchPath = path.join(__dirname, 'srt-batch-002.json');
    if (!fs.existsSync(batchPath)) {
      throw new Error('srt-batch-002.json not found in scripts directory');
    }
    
    const batchData = JSON.parse(fs.readFileSync(batchPath, 'utf8'));
    console.log(`   Loaded: ${batchData.metadata.total_situations} situations`);
    console.log(`   Version: ${batchData.metadata.version}`);
    console.log(`   Batch ID: ${batchData.batch_id}`);
    
    // Upload batch to Firestore
    console.log('\n📦 Uploading SRT batch_002...');
    const batchRef = db.collection('test_content')
      .doc('srt')
      .collection('situation_batches')
      .doc(batchData.batch_id);
    
    await batchRef.set({
      batch_id: batchData.batch_id,
      version: batchData.metadata.version,
      situation_count: batchData.metadata.total_situations,
      situations: batchData.situations,
      uploaded_at: admin.firestore.FieldValue.serverTimestamp()
    });
    
    console.log(`   ✅ ${batchData.batch_id}: ${batchData.metadata.total_situations} situations uploaded`);
    
    // Update metadata (aggregate)
    console.log('\n📝 Updating metadata...');
    const metaRef = db.collection('test_content').doc('srt').collection('meta').doc('config');
    
    // Get existing metadata
    const metaDoc = await metaRef.get();
    const existingMeta = metaDoc.exists ? metaDoc.data() : {};
    
    const updatedMeta = {
      total_situations: (existingMeta.total_situations || 0) + batchData.metadata.total_situations,
      version: batchData.metadata.version,
      last_updated: admin.firestore.FieldValue.serverTimestamp(),
      batches: (existingMeta.batches || 0) + 1,
      distribution: {
        ...existingMeta.distribution,
        ...batchData.metadata.distribution
      },
      difficulty_levels: {
        ...existingMeta.difficulty_levels,
        ...batchData.metadata.difficulty_levels
      },
      description: 'SRT situation repository for progressive caching - covering all 9 SSB categories'
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
    console.log(`   Situation count: ${verifiedData.situation_count}`);
    console.log(`   Situations verified: ${verifiedData.situations.length}`);
    
    if (verifiedData.situations.length === batchData.metadata.total_situations) {
      console.log('\n✅ SUCCESS! All situations uploaded and verified');
    } else {
      console.warn('\n⚠️  Warning: Situation count mismatch');
      console.warn(`   Expected: ${batchData.metadata.total_situations}`);
      console.warn(`   Found: ${verifiedData.situations.length}`);
    }
    
    // Display sample situations
    console.log('\n📄 Sample situations from Firestore:');
    const sampleSituations = verifiedData.situations.slice(0, 3);
    sampleSituations.forEach(situation => {
      console.log(`\n   ID: ${situation.id}`);
      console.log(`   Category: ${situation.category}`);
      console.log(`   Difficulty: ${situation.difficulty}`);
      console.log(`   Situation: ${situation.situation.substring(0, 80)}...`);
    });
    
    console.log('\n🎉 Upload complete!');
    console.log(`\n📊 Summary:`);
    console.log(`   Total situations in batch: ${batchData.metadata.total_situations}`);
    console.log(`   Categories: ${Object.keys(batchData.metadata.distribution).length}`);
    console.log(`   Difficulty levels: Easy: ${batchData.metadata.difficulty_levels.easy}, Medium: ${batchData.metadata.difficulty_levels.medium}, Hard: ${batchData.metadata.difficulty_levels.hard}`);
    console.log(`   Firestore path: test_content/srt/situation_batches/${batchData.batch_id}`);
    
  } catch (error) {
    console.error('\n❌ Error uploading SRT batch:', error);
    throw error;
  }
}

// Main execution
(async () => {
  try {
    await uploadSRTBatch();
    process.exit(0);
  } catch (error) {
    console.error('\n💥 Fatal error:', error);
    process.exit(1);
  }
})();

