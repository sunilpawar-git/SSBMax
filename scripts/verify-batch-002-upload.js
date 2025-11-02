#!/usr/bin/env node

/**
 * Verify batch_002 uploads in Firestore
 * - Checks WAT, SRT, and OIR batch_002 data
 * - Validates counts and structure
 * - Displays summary statistics
 * 
 * Usage: node verify-batch-002-upload.js
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin SDK
const serviceAccountPath = path.join(__dirname, '../.firebase/service-account.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error('❌ Error: Firebase service account key not found');
  process.exit(1);
}

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('✅ Firebase Admin SDK initialized\n');
} catch (error) {
  console.error('❌ Error initializing Firebase:', error.message);
  process.exit(1);
}

const db = admin.firestore();

async function verifyWATBatch() {
  console.log('📝 Verifying WAT batch_002...');
  try {
    const batchRef = db.collection('test_content').doc('wat').collection('word_batches').doc('batch_002');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.log('  ❌ WAT batch_002 not found');
      return { success: false, count: 0 };
    }
    
    const data = batchDoc.data();
    console.log(`  ✅ WAT batch_002 found`);
    console.log(`  📊 Word count: ${data.word_count}`);
    console.log(`  📊 Actual words: ${data.words.length}`);
    console.log(`  📅 Uploaded: ${data.uploaded_at ? data.uploaded_at.toDate().toLocaleString() : 'N/A'}`);
    
    // Verify word range
    const firstWord = data.words[0];
    const lastWord = data.words[data.words.length - 1];
    console.log(`  📍 Word range: ${firstWord.word} (#${firstWord.sequenceNumber}) to ${lastWord.word} (#${lastWord.sequenceNumber})`);
    
    return { success: true, count: data.words.length };
  } catch (error) {
    console.log(`  ❌ Error: ${error.message}`);
    return { success: false, count: 0 };
  }
}

async function verifySRTBatch() {
  console.log('\n📝 Verifying SRT batch_002...');
  try {
    const batchRef = db.collection('test_content').doc('srt').collection('situation_batches').doc('batch_002');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.log('  ❌ SRT batch_002 not found');
      return { success: false, count: 0 };
    }
    
    const data = batchDoc.data();
    console.log(`  ✅ SRT batch_002 found`);
    console.log(`  📊 Situation count: ${data.situation_count}`);
    console.log(`  📊 Actual situations: ${data.situations.length}`);
    console.log(`  📅 Uploaded: ${data.uploaded_at ? data.uploaded_at.toDate().toLocaleString() : 'N/A'}`);
    
    // Verify situation range
    const firstSit = data.situations[0];
    const lastSit = data.situations[data.situations.length - 1];
    console.log(`  📍 Situation range: #${firstSit.sequenceNumber} (${firstSit.category}) to #${lastSit.sequenceNumber} (${lastSit.category})`);
    
    // Category distribution
    const categories = {};
    data.situations.forEach(sit => {
      categories[sit.category] = (categories[sit.category] || 0) + 1;
    });
    console.log(`  📊 Categories: ${Object.keys(categories).length}`);
    Object.entries(categories).forEach(([cat, count]) => {
      console.log(`     - ${cat}: ${count}`);
    });
    
    return { success: true, count: data.situations.length };
  } catch (error) {
    console.log(`  ❌ Error: ${error.message}`);
    return { success: false, count: 0 };
  }
}

async function verifyOIRBatch() {
  console.log('\n📝 Verifying OIR batch_002...');
  try {
    const batchRef = db.collection('test_content').doc('oir').collection('question_batches').doc('batch_002');
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.log('  ❌ OIR batch_002 not found');
      return { success: false, count: 0 };
    }
    
    const data = batchDoc.data();
    console.log(`  ✅ OIR batch_002 found`);
    console.log(`  📊 Question count: ${data.question_count}`);
    console.log(`  📊 Actual questions: ${data.questions.length}`);
    console.log(`  📅 Uploaded: ${data.uploaded_at ? data.uploaded_at.toDate().toLocaleString() : 'N/A'}`);
    console.log(`  📝 Part info: ${data.part_info || 'N/A'}`);
    
    // Verify question range
    const firstQ = data.questions[0];
    const lastQ = data.questions[data.questions.length - 1];
    console.log(`  📍 Question range: #${firstQ.questionNumber} to #${lastQ.questionNumber}`);
    
    // Type distribution
    const types = {};
    data.questions.forEach(q => {
      types[q.type] = (types[q.type] || 0) + 1;
    });
    console.log(`  📊 Question types:`);
    Object.entries(types).forEach(([type, count]) => {
      console.log(`     - ${type}: ${count}`);
    });
    
    return { success: true, count: data.questions.length };
  } catch (error) {
    console.log(`  ❌ Error: ${error.message}`);
    return { success: false, count: 0 };
  }
}

async function verifyAll() {
  console.log('='.repeat(60));
  console.log('🔍 BATCH_002 VERIFICATION REPORT');
  console.log('='.repeat(60));
  console.log();
  
  const watResult = await verifyWATBatch();
  const srtResult = await verifySRTBatch();
  const oirResult = await verifyOIRBatch();
  
  console.log('\n' + '='.repeat(60));
  console.log('📊 SUMMARY');
  console.log('='.repeat(60));
  
  const totalItems = watResult.count + srtResult.count + oirResult.count;
  const allSuccess = watResult.success && srtResult.success && oirResult.success;
  
  console.log(`\n  WAT: ${watResult.success ? '✅' : '❌'} ${watResult.count} words`);
  console.log(`  SRT: ${srtResult.success ? '✅' : '❌'} ${srtResult.count} situations`);
  console.log(`  OIR: ${oirResult.success ? '✅' : '❌'} ${oirResult.count} questions`);
  console.log(`\n  📈 Total items in Firestore: ${totalItems}`);
  console.log(`  🎯 Expected (Part 1): 120 items`);
  
  if (allSuccess && totalItems >= 120) {
    console.log('\n🎉 SUCCESS! All batch_002 content verified!');
    console.log('\n📱 Next steps:');
    console.log('  1. Run the app and check progressive caching');
    console.log('  2. Test WAT with new words (61-100)');
    console.log('  3. Test SRT with new situations (61-90)');
    console.log('  4. Test OIR with new questions (101-150)');
    console.log('  5. Upload OIR part2 for questions 151-200');
  } else if (allSuccess) {
    console.log('\n⚠️  Content found but count is lower than expected');
  } else {
    console.log('\n❌ Verification failed - some batches not found or have errors');
  }
  
  console.log('\n' + '='.repeat(60));
}

// Main execution
(async () => {
  try {
    await verifyAll();
    process.exit(0);
  } catch (error) {
    console.error('\n💥 Fatal error:', error);
    process.exit(1);
  }
})();

