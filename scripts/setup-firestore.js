#!/usr/bin/env node

/**
 * Firestore Collections Setup Script
 * 
 * Automatically creates the 4 required collections with test documents:
 * 1. health_check - For connectivity testing
 * 2. content_versions - For cache invalidation
 * 3. topic_content - For topic introductions
 * 4. study_materials - For study content
 * 
 * Usage: node scripts/setup-firestore.js
 */

const admin = require('firebase-admin');
const path = require('path');

// Initialize Firebase Admin SDK
// Try multiple authentication methods in order of preference
let initialized = false;
let projectId = null;

// Method 1: Try service account key (firebase-admin-key.json)
const serviceAccountPath = path.join(__dirname, '..', 'firebase-admin-key.json');
try {
  if (require('fs').existsSync(serviceAccountPath)) {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: serviceAccount.project_id
    });
    projectId = serviceAccount.project_id;
    initialized = true;
    console.log('✓ Firebase Admin SDK initialized (using service account key)');
  }
} catch (error) {
  // Will try next method
}

// Method 2: Try application default credentials (if Firebase CLI is logged in)
if (!initialized) {
  try {
    // Read project ID from google-services.json
    const googleServicesPath = path.join(__dirname, '..', 'app', 'google-services.json');
    const googleServices = require(googleServicesPath);
    projectId = googleServices.project_info.project_id;
    
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
      projectId: projectId
    });
    initialized = true;
    console.log('✓ Firebase Admin SDK initialized (using application default credentials)');
  } catch (error) {
    console.error('✗ Failed to initialize Firebase Admin SDK');
    console.error('\nYou need to authenticate with one of these methods:\n');
    console.error('Method 1: Download service account key');
    console.error('  1. Go to: https://console.firebase.google.com/project/ssbmax-49e68/settings/serviceaccounts/adminsdk');
    console.error('  2. Click "Generate new private key"');
    console.error('  3. Save as: /Users/sunil/Downloads/SSBMax/firebase-admin-key.json');
    console.error('  4. Run this script again\n');
    console.error('Method 2: Use Firebase CLI credentials');
    console.error('  1. Run: export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.config/firebase/your-user-id.json"');
    console.error('  2. Or run: firebase login:use');
    console.error('  3. Run this script again\n');
    console.error(`Error: ${error.message}\n`);
    process.exit(1);
  }
}

console.log(`✓ Project: ${projectId}\n`);

const db = admin.firestore();

// Enable offline persistence (optional, but good for development)
db.settings({
  ignoreUndefinedProperties: true
});

/**
 * Collection 1: health_check
 */
async function createHealthCheckCollection() {
  console.log('Creating health_check collection...');
  
  try {
    await db.collection('health_check').doc('test').set({
      status: 'healthy',
      timestamp: Date.now(),
      message: 'Firestore is operational',
      createdBy: 'setup-script',
      version: 1
    });
    
    console.log('  ✓ health_check/test document created\n');
    return true;
  } catch (error) {
    console.error(`  ✗ Failed: ${error.message}\n`);
    return false;
  }
}

/**
 * Collection 2: content_versions
 */
async function createContentVersionsCollection() {
  console.log('Creating content_versions collection...');
  
  try {
    await db.collection('content_versions').doc('global').set({
      topicsVersion: 1,
      materialsVersion: 1,
      lastUpdated: Date.now(),
      updatedBy: 'setup-script',
      notes: 'Initial version - ready for migration'
    });
    
    console.log('  ✓ content_versions/global document created\n');
    return true;
  } catch (error) {
    console.error(`  ✗ Failed: ${error.message}\n`);
    return false;
  }
}

/**
 * Collection 3: topic_content
 */
async function createTopicContentCollection() {
  console.log('Creating topic_content collection...');
  
  try {
    await db.collection('topic_content').doc('TEST').set({
      id: 'TEST',
      topicType: 'TEST',
      title: 'Test Topic',
      introduction: '# Test Topic\n\nThis is a **test topic** created by the setup script.\n\n- This will be replaced during migration\n- Used for testing Firestore connectivity\n- Can be safely deleted after OIR migration',
      version: 1,
      lastUpdated: Date.now(),
      isPremium: false,
      metadata: {
        createdBy: 'setup-script',
        purpose: 'testing'
      }
    });
    
    console.log('  ✓ topic_content/TEST document created');
    console.log('  Note: This test document will be replaced during migration\n');
    return true;
  } catch (error) {
    console.error(`  ✗ Failed: ${error.message}\n`);
    return false;
  }
}

/**
 * Collection 4: study_materials
 */
async function createStudyMaterialsCollection() {
  console.log('Creating study_materials collection...');
  
  try {
    const testMaterial = {
      id: 'test_1',
      topicType: 'TEST',
      title: 'Test Study Material',
      displayOrder: 1,
      category: 'Test Category',
      contentMarkdown: '# Test Material\n\nThis is a **test study material** created by the setup script.\n\n## Features\n\n- Markdown formatting\n- **Bold text** support\n- Bullet lists\n\n## Purpose\n\nThis document tests:\n1. Firestore write operations\n2. Document structure validation\n3. Query index requirements\n\nThis will be replaced during migration with actual OIR content.',
      author: 'Setup Script',
      readTime: '5 min read',
      isPremium: false,
      version: 1,
      lastUpdated: Date.now(),
      tags: ['test', 'setup'],
      relatedMaterials: [],
      attachments: []
    };
    
    // Let Firestore auto-generate document ID
    const docRef = await db.collection('study_materials').add(testMaterial);
    
    console.log(`  ✓ study_materials/${docRef.id} document created`);
    console.log('  Note: This test document will be replaced during migration\n');
    return true;
  } catch (error) {
    console.error(`  ✗ Failed: ${error.message}\n`);
    return false;
  }
}

/**
 * Verify collections were created
 */
async function verifyCollections() {
  console.log('Verifying collections...\n');
  
  const collections = ['health_check', 'content_versions', 'topic_content', 'study_materials'];
  let allGood = true;
  
  for (const collectionName of collections) {
    try {
      const snapshot = await db.collection(collectionName).limit(1).get();
      
      if (snapshot.empty) {
        console.log(`  ✗ ${collectionName}: No documents found`);
        allGood = false;
      } else {
        console.log(`  ✓ ${collectionName}: ${snapshot.size} document(s)`);
      }
    } catch (error) {
      console.log(`  ✗ ${collectionName}: ${error.message}`);
      allGood = false;
    }
  }
  
  console.log('');
  return allGood;
}

/**
 * Main execution
 */
async function main() {
  console.log('='.repeat(60));
  console.log('Firestore Collections Setup');
  console.log('='.repeat(60));
  console.log('');
  
  const results = [];
  
  // Create collections sequentially
  results.push(await createHealthCheckCollection());
  results.push(await createContentVersionsCollection());
  results.push(await createTopicContentCollection());
  results.push(await createStudyMaterialsCollection());
  
  // Verify
  const verified = await verifyCollections();
  
  // Summary
  console.log('='.repeat(60));
  console.log('Setup Summary');
  console.log('='.repeat(60));
  
  const successCount = results.filter(r => r).length;
  console.log(`Collections created: ${successCount}/4`);
  console.log(`Verification: ${verified ? '✓ PASSED' : '✗ FAILED'}`);
  console.log('');
  
  if (successCount === 4 && verified) {
    console.log('✓ SUCCESS: All collections created successfully!\n');
    console.log('Next Steps:');
    console.log('1. Create Firestore indexes (Step 7 in guide)');
    console.log('2. Run health check from app (Step 9)');
    console.log('3. Test migration with OIR topic (Step 10)');
    console.log('');
    console.log('Index Creation:');
    console.log('  Go to: https://console.firebase.google.com/project/YOUR_PROJECT/firestore/indexes');
    console.log('  Create index: study_materials (topicType ASC, displayOrder ASC)');
    console.log('');
  } else {
    console.log('✗ FAILURE: Some collections failed to create\n');
    console.log('Troubleshooting:');
    console.log('1. Check Firebase security rules are deployed');
    console.log('2. Verify google-services.json is valid');
    console.log('3. Check Firebase Console for error details');
    console.log('');
  }
  
  process.exit(successCount === 4 && verified ? 0 : 1);
}

// Run the script
main().catch((error) => {
  console.error('\n✗ Script failed with error:');
  console.error(error);
  process.exit(1);
});

