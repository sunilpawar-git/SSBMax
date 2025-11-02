const admin = require('firebase-admin');
const path = require('path');

// Initialize Firebase Admin with service account
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function verifyAllBatches() {
  console.log('\n🔍 CHECKING ALL BATCHES IN FIRESTORE');
  console.log('============================================================\n');

  // Check OIR batches
  console.log('📝 Checking OIR batches...');
  const oirBatches = await db.collection('test_content/oir/question_batches').get();
  console.log(`  Found ${oirBatches.size} OIR batches:`);
  oirBatches.forEach(doc => {
    const data = doc.data();
    console.log(`    ✅ ${doc.id}: ${data.questions?.length || 0} questions (v${data.version})`);
  });

  // Check WAT batches
  console.log('\n📝 Checking WAT batches...');
  const watBatches = await db.collection('test_content/wat/word_batches').get();
  console.log(`  Found ${watBatches.size} WAT batches:`);
  if (watBatches.empty) {
    console.log('    ❌ No WAT batches found!');
  } else {
    watBatches.forEach(doc => {
      const data = doc.data();
      console.log(`    ✅ ${doc.id}: ${data.words?.length || 0} words (v${data.version})`);
    });
  }

  // Check SRT batches
  console.log('\n📝 Checking SRT batches...');
  const srtBatches = await db.collection('test_content/srt/situation_batches').get();
  console.log(`  Found ${srtBatches.size} SRT batches:`);
  if (srtBatches.empty) {
    console.log('    ❌ No SRT batches found!');
  } else {
    srtBatches.forEach(doc => {
      const data = doc.data();
      console.log(`    ✅ ${doc.id}: ${data.situations?.length || 0} situations (v${data.version})`);
    });
  }

  // Check metadata
  console.log('\n📝 Checking metadata documents...');
  
  const oirMeta = await db.doc('test_content/oir/meta/overview').get();
  console.log(`  OIR meta: ${oirMeta.exists ? '✅' : '❌'}`);
  if (oirMeta.exists) {
    const data = oirMeta.data();
    console.log(`    Total questions: ${data.total_questions}`);
    console.log(`    Available batches: ${data.available_batches?.join(', ')}`);
  }

  const watMeta = await db.doc('test_content/wat/meta/overview').get();
  console.log(`  WAT meta: ${watMeta.exists ? '✅' : '❌'}`);
  if (watMeta.exists) {
    const data = watMeta.data();
    console.log(`    Total words: ${data.total_words}`);
    console.log(`    Available batches: ${data.available_batches?.join(', ')}`);
  }

  const srtMeta = await db.doc('test_content/srt/meta/overview').get();
  console.log(`  SRT meta: ${srtMeta.exists ? '✅' : '❌'}`);
  if (srtMeta.exists) {
    const data = srtMeta.data();
    console.log(`    Total situations: ${data.total_situations}`);
    console.log(`    Available batches: ${data.available_batches?.join(', ')}`);
  }

  console.log('\n============================================================');
  console.log('✅ Verification complete\n');
}

verifyAllBatches()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Error:', error);
    process.exit(1);
  });

