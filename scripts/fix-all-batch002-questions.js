#!/usr/bin/env node
/**
 * Fix ALL corrupted questions in batch_002
 * Convert all single-letter option IDs (a,b,c,d) to opt_X format
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixAllBatch002Questions() {
  console.log('🔧 Fixing ALL corrupted questions in batch_002...\n');
  
  try {
    // Read the corrected batch file
    const batchPath = path.join(__dirname, 'oir-batch-002.json');
    const batchData = JSON.parse(fs.readFileSync(batchPath, 'utf8'));
    
    console.log(`📄 Loaded batch_002.json with ${batchData.questions.length} questions\n`);
    
    // Fix ALL questions by ensuring option IDs are in opt_X format
    const fixedQuestions = batchData.questions.map(q => {
      const fixed = { ...q };
      
      // Fix option IDs
      fixed.options = q.options.map((opt, index) => {
        const optionLetter = String.fromCharCode(97 + index); // a, b, c, d
        const correctId = `opt_${optionLetter}`;
        
        return {
          ...opt,
          id: correctId // Force correct format
        };
      });
      
      // Fix correctAnswerId if it's a single letter
      if (fixed.correctAnswerId && fixed.correctAnswerId.length === 1) {
        fixed.correctAnswerId = `opt_${fixed.correctAnswerId}`;
      }
      
      return fixed;
    });
    
    console.log('✅ Fixed all question option IDs to opt_X format\n');
    
    // Upload to Firestore
    console.log('📤 Uploading entire corrected batch to Firestore...\n');
    
    const docRef = db.collection('test_content/oir/question_batches').doc('batch_002');
    
    await docRef.update({
      questions: fixedQuestions,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      version: '1.1',
      notes: 'Fixed all option IDs to opt_a/opt_b/opt_c/opt_d format'
    });
    
    console.log('✅ Successfully uploaded corrected batch_002!\n');
    console.log(`📊 Total questions fixed: ${fixedQuestions.length}`);
    console.log('\n🎉 All OIR batch_002 questions are now valid!');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

fixAllBatch002Questions();
