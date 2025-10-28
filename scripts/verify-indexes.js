#!/usr/bin/env node

/**
 * Firestore Index Verification Script
 * 
 * Tests if the required indexes are created and working by running
 * the actual queries that the app will use.
 * 
 * Usage: node scripts/verify-indexes.js
 */

const admin = require('firebase-admin');
const path = require('path');

// Initialize Firebase Admin SDK
const serviceAccountPath = path.join(__dirname, '..', 'firebase-admin-key.json');

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: serviceAccount.project_id
  });
  console.log('✓ Firebase Admin SDK initialized');
  console.log(`✓ Project: ${serviceAccount.project_id}\n`);
} catch (error) {
  console.error('✗ Failed to initialize Firebase Admin SDK');
  console.error(`  Error: ${error.message}\n`);
  process.exit(1);
}

const db = admin.firestore();

/**
 * Test 1: Query study materials by topic (requires index)
 */
async function testTopicMaterialsQuery() {
  console.log('Test 1: Query study materials by topic');
  console.log('  Query: study_materials where topicType="TEST" orderBy displayOrder');
  
  try {
    const snapshot = await db.collection('study_materials')
      .where('topicType', '==', 'TEST')
      .orderBy('displayOrder', 'asc')
      .get();
    
    console.log(`  ✓ Query succeeded! Found ${snapshot.size} document(s)`);
    console.log('  ✓ Index is working correctly\n');
    return true;
  } catch (error) {
    if (error.message.includes('index')) {
      console.log('  ✗ Query failed - INDEX NOT READY');
      console.log('  ✗ Error: Index is still building or not created');
      console.log('  ℹ️  Wait a few more minutes and try again\n');
      return false;
    } else {
      console.log(`  ✗ Query failed: ${error.message}\n`);
      return false;
    }
  }
}

/**
 * Test 2: Query premium materials (optional index)
 */
async function testPremiumMaterialsQuery() {
  console.log('Test 2: Query premium materials (optional)');
  console.log('  Query: study_materials where isPremium=false and topicType="TEST"');
  
  try {
    const snapshot = await db.collection('study_materials')
      .where('isPremium', '==', false)
      .where('topicType', '==', 'TEST')
      .get();
    
    console.log(`  ✓ Query succeeded! Found ${snapshot.size} document(s)`);
    console.log('  ✓ Premium index is working (optional)\n');
    return true;
  } catch (error) {
    if (error.message.includes('index')) {
      console.log('  ⚠️  Query failed - Premium index not created');
      console.log('  ℹ️  This is optional, you can skip it for now\n');
      return 'optional';
    } else {
      console.log(`  ✗ Query failed: ${error.message}\n`);
      return false;
    }
  }
}

/**
 * Test 3: Simple read (no index required)
 */
async function testSimpleRead() {
  console.log('Test 3: Simple document read (no index needed)');
  
  try {
    const doc = await db.collection('health_check').doc('test').get();
    
    if (doc.exists) {
      console.log('  ✓ Document read successful');
      console.log(`  ✓ Status: ${doc.data().status}\n`);
      return true;
    } else {
      console.log('  ✗ Document not found\n');
      return false;
    }
  } catch (error) {
    console.log(`  ✗ Read failed: ${error.message}\n`);
    return false;
  }
}

/**
 * Check index status via error messages
 */
async function checkIndexStatus() {
  console.log('='.repeat(60));
  console.log('Firestore Index Verification');
  console.log('='.repeat(60));
  console.log('');
  
  const results = {
    simpleRead: await testSimpleRead(),
    topicIndex: await testTopicMaterialsQuery(),
    premiumIndex: await testPremiumMaterialsQuery()
  };
  
  // Summary
  console.log('='.repeat(60));
  console.log('Verification Summary');
  console.log('='.repeat(60));
  console.log('');
  
  console.log('Basic Connectivity:', results.simpleRead ? '✓ PASS' : '✗ FAIL');
  console.log('Topic Materials Index:', results.topicIndex ? '✓ PASS' : '✗ FAIL (REQUIRED)');
  console.log('Premium Materials Index:', 
    results.premiumIndex === true ? '✓ PASS (Optional)' : 
    results.premiumIndex === 'optional' ? '⚠️  SKIP (Optional)' : '✗ FAIL');
  console.log('');
  
  if (results.simpleRead && results.topicIndex) {
    console.log('✓ SUCCESS: All required indexes are working!\n');
    console.log('You can now proceed to Step 9: Verify from app\n');
    return true;
  } else if (!results.topicIndex) {
    console.log('✗ INCOMPLETE: Topic materials index is not ready yet\n');
    console.log('What to do:');
    console.log('1. Go to: https://console.firebase.google.com/project/ssbmax-49e68/firestore/indexes');
    console.log('2. Check if index status shows "Enabled" (green)');
    console.log('3. If it says "Building...", wait 2-5 more minutes');
    console.log('4. Run this script again: node scripts/verify-indexes.js\n');
    return false;
  } else {
    console.log('✗ FAILURE: Basic connectivity test failed\n');
    console.log('Check your Firebase setup and try again\n');
    return false;
  }
}

// Run verification
checkIndexStatus()
  .then(success => {
    process.exit(success ? 0 : 1);
  })
  .catch(error => {
    console.error('\n✗ Verification failed with error:');
    console.error(error);
    process.exit(1);
  });

