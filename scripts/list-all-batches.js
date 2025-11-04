#!/usr/bin/env node
const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function listAllBatches() {
  console.log('📦 Checking Firestore for ALL OIR batches...\n');
  
  const batchesSnapshot = await db
    .collection('test_content/oir/question_batches')
    .get();
  
  console.log(`Found ${batchesSnapshot.size} batches in Firestore:\n`);
  
  for (const doc of batchesSnapshot.docs) {
    const data = doc.data();
    console.log(`✅ ${doc.id}:`);
    console.log(`   - Questions: ${data.questions?.length || 0}`);
    console.log(`   - Version: ${data.version || 'N/A'}`);
    console.log(`   - Last Updated: ${data.lastUpdated?.toDate?.() || 'N/A'}`);
    console.log('');
  }
  
  await admin.app().delete();
}

listAllBatches();
