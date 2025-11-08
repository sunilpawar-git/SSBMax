const admin = require('firebase-admin');

const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Try different bucket names
const possibleBuckets = [
  'ssbmax-49e68.appspot.com',
  'ssbmax-49e68.firebasestorage.app',
  'ssbmax-49e68'
];

async function checkBucket(bucketName) {
  try {
    const bucket = admin.storage().bucket(bucketName);
    const [files] = await bucket.getFiles({ prefix: 'ppdt_images/', maxResults: 70 });
    return { success: true, bucket: bucketName, files };
  } catch (error) {
    return { success: false, bucket: bucketName, error: error.message };
  }
}

async function findAndListImages() {
  console.log('🔍 Searching for PPDT images in Firebase Storage...\n');
  
  for (const bucketName of possibleBuckets) {
    console.log(`📦 Trying bucket: ${bucketName}`);
    const result = await checkBucket(bucketName);
    
    if (result.success) {
      console.log(`✅ Found bucket!`);
      console.log(`\n📊 Found ${result.files.length} files:\n`);
      
      result.files.forEach((file, index) => {
        if (index < 10 || index >= result.files.length - 5) {
          console.log(`${index + 1}. ${file.name}`);
        } else if (index === 10) {
          console.log(`   ... (${result.files.length - 15} more files) ...`);
        }
      });
      
      const extensions = new Set();
      result.files.forEach(file => {
        const ext = file.name.split('.').pop().toLowerCase();
        extensions.add(ext);
      });
      
      console.log(`\n📊 Extensions: ${Array.from(extensions).join(', ')}`);
      console.log(`✅ Total: ${result.files.length} files`);
      console.log(`\n✅ Correct bucket: ${bucketName}`);
      process.exit(0);
    } else {
      console.log(`❌ Failed: ${result.error}\n`);
    }
  }
  
  console.log('❌ Could not find images in any bucket');
  console.log('\n💡 Please check Firebase Console → Storage to verify:');
  console.log('   1. Storage is enabled for your project');
  console.log('   2. Images were actually uploaded');
  console.log('   3. The correct path (ppdt_images/...)');
  process.exit(1);
}

findAndListImages();
