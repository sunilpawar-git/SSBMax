const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function uploadOIRBatch002Part2() {
  try {
    console.log('\n🚀 OIR BATCH_002 PART 2 UPLOAD');
    console.log('============================================================\n');

    // Read Part 2 data
    const part2Data = JSON.parse(
      fs.readFileSync(path.join(__dirname, 'oir-batch-002-part2.json'), 'utf8')
    );

    console.log(`📊 Part 2 Info:`);
    console.log(`   Batch ID: ${part2Data.batch_info.batch_id}`);
    console.log(`   Part: ${part2Data.batch_info.part}`);
    console.log(`   Question Range: ${part2Data.batch_info.question_range}`);
    console.log(`   Questions: ${part2Data.batch_info.total_questions}\n`);

    // Get existing batch_002 document
    const batchRef = db.doc('test_content/oir/question_batches/batch_002');
    const batchDoc = await batchRef.get();

    if (!batchDoc.exists) {
      throw new Error('batch_002 not found! Upload Part 1 first.');
    }

    const existingData = batchDoc.data();
    const existingQuestions = existingData.questions || [];

    console.log(`📦 Existing batch_002 has ${existingQuestions.length} questions`);
    console.log(`➕ Adding ${part2Data.questions.length} more questions\n`);

    // Merge Part 1 and Part 2 questions
    const allQuestions = [...existingQuestions, ...part2Data.questions];

    // Update batch_002 with merged questions
    await batchRef.update({
      questions: allQuestions,
      question_count: allQuestions.length,
      updated_at: admin.firestore.Timestamp.now(),
      part_info: 'Complete (Part 1 + Part 2)',
      description: 'Additional questions for OIR test - 100 questions total'
    });

    console.log('✅ batch_002 updated successfully!\n');

    // Update metadata
    const metaRef = db.doc('test_content/oir/meta/overview');
    const metaDoc = await metaRef.get();

    if (metaDoc.exists) {
      const metaData = metaDoc.data();
      await metaRef.update({
        total_questions: (metaData.total_questions || 0) + part2Data.questions.length,
        last_updated: admin.firestore.Timestamp.now()
      });
      console.log('✅ Metadata updated!\n');
    }

    console.log('============================================================');
    console.log('🎉 OIR BATCH_002 PART 2 UPLOAD COMPLETE!\n');
    console.log('📊 Final Summary:');
    console.log(`   ✅ Total questions in batch_002: ${allQuestions.length}`);
    console.log(`   ✅ Question range: #101-#200`);
    console.log(`   ✅ Combined from 2 parts\n`);

    // Verify distribution
    const typeCount = {};
    const difficultyCount = {};
    
    allQuestions.forEach(q => {
      typeCount[q.type] = (typeCount[q.type] || 0) + 1;
      difficultyCount[q.difficulty] = (difficultyCount[q.difficulty] || 0) + 1;
    });

    console.log('📊 Question Distribution:');
    Object.entries(typeCount).forEach(([type, count]) => {
      console.log(`   ${type}: ${count}`);
    });

    console.log('\n📊 Difficulty Distribution:');
    Object.entries(difficultyCount).forEach(([difficulty, count]) => {
      console.log(`   ${difficulty}: ${count}`);
    });

    console.log('\n✨ Ready for testing!\n');

  } catch (error) {
    console.error('❌ Upload failed:', error);
    throw error;
  }
}

// Run the upload
uploadOIRBatch002Part2()
  .then(() => {
    console.log('✅ Upload process completed successfully!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Upload process failed:', error);
    process.exit(1);
  });

