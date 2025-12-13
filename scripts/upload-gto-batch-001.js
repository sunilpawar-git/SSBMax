const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function uploadGTOBatch001() {
  try {
    console.log('📤 Starting GTO batch_001 upload...\n');

    // Read the batch file
    const batchData = JSON.parse(
      fs.readFileSync(path.join(__dirname, 'gto-batch-001.json'), 'utf8')
    );

    console.log(`📊 Batch Info:`);
    console.log(`   ID: ${batchData.batch_id}`);
    console.log(`   Version: ${batchData.version}`);
    console.log(`   Task Count: ${batchData.task_count}`);
    console.log(`   Description: ${batchData.description}\n`);

    // Upload to test_content/gto/task_batches/batch_001
    const batchRef = db.doc(`test_content/gto/task_batches/${batchData.batch_id}`);
    
    await batchRef.set({
      batch_id: batchData.batch_id,
      version: batchData.version,
      task_count: batchData.task_count,
      uploaded_at: admin.firestore.Timestamp.now(),
      description: batchData.description,
      tasks: batchData.tasks
    });

    console.log('✅ Batch uploaded successfully!');

    // Update or create metadata
    const metaRef = db.doc('test_content/gto/meta/overview');
    const metaDoc = await metaRef.get();

    if (metaDoc.exists) {
      // Update existing metadata
      const currentData = metaDoc.data();
      const updatedBatches = currentData.available_batches || [];
      if (!updatedBatches.includes(batchData.batch_id)) {
        updatedBatches.push(batchData.batch_id);
        updatedBatches.sort(); // Keep batches in order
      }

      await metaRef.update({
        total_tasks: (currentData.total_tasks || 0) + batchData.task_count,
        available_batches: updatedBatches,
        last_updated: admin.firestore.Timestamp.now()
      });

      console.log('✅ Metadata updated!');
    } else {
      // Create new metadata
      await metaRef.set({
        total_tasks: batchData.task_count,
        available_batches: [batchData.batch_id],
        last_updated: admin.firestore.Timestamp.now(),
        content_type: 'gto_tasks'
      });

      console.log('✅ Metadata created!');
    }

    console.log('\n🎉 GTO batch_001 upload complete!\n');
    console.log('📊 Summary:');
    console.log(`   ✅ ${batchData.task_count} tasks uploaded`);
    
    // Count by task type
    const gdCount = batchData.tasks.filter(t => t.taskType === 'GD').length;
    const lectCount = batchData.tasks.filter(t => t.taskType === 'LECTURETTE').length;
    
    console.log(`   ✅ Group Discussion: ${gdCount} topics`);
    console.log(`   ✅ Lecturette: ${lectCount} topics`);
    console.log(`   ✅ Batch: ${batchData.batch_id}`);
    console.log(`   ✅ Version: ${batchData.version}\n`);

  } catch (error) {
    console.error('❌ Upload failed:', error);
    throw error;
  }
}

// Run the upload
uploadGTOBatch001()
  .then(() => {
    console.log('✨ Upload process completed successfully!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Upload process failed:', error);
    process.exit(1);
  });
