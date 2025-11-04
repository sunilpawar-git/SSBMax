#!/usr/bin/env node
/**
 * Check all OIR batches in Firestore for corrupted option IDs
 */

const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkAllBatches() {
  console.log('🔍 Checking all OIR batches for corrupted option IDs...\n');
  
  try {
    const batchesSnapshot = await db
      .collection('test_content/oir/question_batches')
      .get();
    
    if (batchesSnapshot.empty) {
      console.log('❌ No batches found in Firestore!');
      return;
    }
    
    console.log(`Found ${batchesSnapshot.size} batches\n`);
    
    for (const doc of batchesSnapshot.docs) {
      const batchId = doc.id;
      const data = doc.data();
      const questions = data.questions || [];
      
      console.log(`\n📦 Batch: ${batchId} (${questions.length} questions)`);
      console.log('─'.repeat(60));
      
      const corruptedQuestions = questions.filter(q => {
        const options = q.options || [];
        if (options.length === 0) return false;
        
        // Check if any option ID is just a letter (a, b, c, d) instead of opt_X
        return options.some(opt => 
          opt.id && opt.id.length === 1 && /^[a-d]$/.test(opt.id)
        );
      });
      
      if (corruptedQuestions.length > 0) {
        console.log(`❌ FOUND ${corruptedQuestions.length} CORRUPTED QUESTIONS:`);
        corruptedQuestions.forEach(q => {
          console.log(`   - ${q.id}`);
          console.log(`     Options: ${q.options.map(o => o.id).join(', ')}`);
          console.log(`     CorrectAnswer: ${q.correctAnswerId}`);
        });
      } else {
        console.log(`✅ All questions valid (all have opt_a, opt_b, opt_c, opt_d format)`);
      }
    }
    
    console.log('\n✅ Check complete!');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

checkAllBatches();
