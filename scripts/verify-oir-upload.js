#!/usr/bin/env node

/**
 * Verify OIR questions upload in Firestore
 */

const admin = require('firebase-admin');

const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function verifyUpload() {
  try {
    console.log('\n🔍 Verifying OIR questions in Firestore...\n');
    
    // Check metadata
    const metaDoc = await db.collection('test_content').doc('oir').collection('meta').doc('config').get();
    if (metaDoc.exists) {
      const meta = metaDoc.data();
      console.log('✅ Metadata found:');
      console.log(`   Total questions: ${meta.total_questions}`);
      console.log(`   Version: ${meta.version}`);
      console.log(`   Batches: ${meta.batches}`);
      console.log(`   Distribution: Verbal=${meta.distribution.VERBAL_REASONING}, Non-Verbal=${meta.distribution.NON_VERBAL_REASONING}`);
      console.log();
    }
    
    // Check batches
    const batchesSnapshot = await db.collection('test_content')
      .doc('oir')
      .collection('question_batches')
      .get();
    
    console.log(`✅ Found ${batchesSnapshot.size} batch(es):\n`);
    
    for (const doc of batchesSnapshot.docs) {
      const batch = doc.data();
      console.log(`   📦 ${batch.batch_id}:`);
      console.log(`      Questions: ${batch.question_count}`);
      console.log(`      Version: ${batch.version}`);
      
      // Show first 3 questions
      console.log('      Sample questions:');
      batch.questions.slice(0, 3).forEach((q, i) => {
        console.log(`        ${i+1}. [${q.difficulty}] ${q.type}: ${q.questionText.substring(0, 60)}...`);
      });
      console.log();
    }
    
    // Count by type
    const batch001 = await db.collection('test_content')
      .doc('oir')
      .collection('question_batches')
      .doc('batch_001')
      .get();
    
    if (batch001.exists) {
      const questions = batch001.data().questions;
      const typeCounts = {};
      const difficultyCounts = {};
      
      questions.forEach(q => {
        typeCounts[q.type] = (typeCounts[q.type] || 0) + 1;
        difficultyCounts[q.difficulty] = (difficultyCounts[q.difficulty] || 0) + 1;
      });
      
      console.log('📊 Distribution Analysis:');
      console.log('   By Type:');
      Object.entries(typeCounts).forEach(([type, count]) => {
        console.log(`      ${type}: ${count} (${(count/questions.length*100).toFixed(1)}%)`);
      });
      console.log();
      console.log('   By Difficulty:');
      Object.entries(difficultyCounts).forEach(([diff, count]) => {
        console.log(`      ${diff}: ${count} (${(count/questions.length*100).toFixed(1)}%)`);
      });
    }
    
    console.log('\n✅ Verification complete!\n');
    process.exit(0);
    
  } catch (error) {
    console.error('❌ Verification failed:', error);
    process.exit(1);
  }
}

verifyUpload();

