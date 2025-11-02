#!/usr/bin/env node

/**
 * Master script to upload all batch_002 content to Firestore
 * - WAT: 40 words (61-100)
 * - SRT: 30 situations (61-90)
 * - OIR: 50 questions (101-150) - Part 1 of 2
 * 
 * Total: 120 new items
 * 
 * Usage: node upload-all-batch-002.js
 */

const { exec } = require('child_process');
const path = require('path');
const util = require('util');

const execPromise = util.promisify(exec);

// ANSI color codes
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  red: '\x1b[31m'
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

async function runScript(scriptName, description) {
  log(`\n${'='.repeat(60)}`, colors.cyan);
  log(`📦 ${description}`, colors.bright);
  log('='.repeat(60), colors.cyan);
  
  try {
    const scriptPath = path.join(__dirname, scriptName);
    const { stdout, stderr } = await execPromise(`node "${scriptPath}"`);
    
    if (stdout) console.log(stdout);
    if (stderr) console.error(stderr);
    
    log(`✅ ${description} - SUCCESS`, colors.green);
    return true;
  } catch (error) {
    log(`❌ ${description} - FAILED`, colors.red);
    console.error(error.message);
    return false;
  }
}

async function uploadAllBatches() {
  log('\n' + '='.repeat(60), colors.bright);
  log('🚀 SSBMax Batch 002 - Mass Upload', colors.bright);
  log('='.repeat(60), colors.bright);
  log('\nUploading 120 new items to Firestore:', colors.cyan);
  log('  • WAT: 40 words', colors.yellow);
  log('  • SRT: 30 situations', colors.yellow);
  log('  • OIR: 50 questions (part 1)', colors.yellow);
  
  const startTime = Date.now();
  let successCount = 0;
  const totalUploads = 3;
  
  // Upload in sequence
  const results = [];
  
  // 1. Upload WAT batch_002
  const watSuccess = await runScript('upload-wat-batch-002.js', 'WAT Batch 002 Upload (40 words)');
  results.push({ name: 'WAT', success: watSuccess });
  if (watSuccess) successCount++;
  
  // 2. Upload SRT batch_002
  const srtSuccess = await runScript('upload-srt-batch-002.js', 'SRT Batch 002 Upload (30 situations)');
  results.push({ name: 'SRT', success: srtSuccess });
  if (srtSuccess) successCount++;
  
  // 3. Upload OIR batch_002 part1
  const oirSuccess = await runScript('upload-oir-batch-002-part1.js', 'OIR Batch 002 Part 1 Upload (50 questions)');
  results.push({ name: 'OIR', success: oirSuccess });
  if (oirSuccess) successCount++;
  
  // Summary
  const endTime = Date.now();
  const duration = ((endTime - startTime) / 1000).toFixed(2);
  
  log('\n' + '='.repeat(60), colors.bright);
  log('📊 UPLOAD SUMMARY', colors.bright);
  log('='.repeat(60), colors.bright);
  
  results.forEach(result => {
    const status = result.success ? '✅ SUCCESS' : '❌ FAILED';
    const color = result.success ? colors.green : colors.red;
    log(`  ${result.name}: ${status}`, color);
  });
  
  log(`\n⏱️  Time taken: ${duration} seconds`, colors.cyan);
  log(`📈 Success rate: ${successCount}/${totalUploads} (${((successCount/totalUploads)*100).toFixed(0)}%)`, colors.cyan);
  
  if (successCount === totalUploads) {
    log('\n🎉 ALL UPLOADS COMPLETED SUCCESSFULLY!', colors.green + colors.bright);
    log('\n📱 Next Steps:', colors.cyan);
    log('  1. Open Firebase Console to verify data', colors.yellow);
    log('  2. Run the app and test progressive caching', colors.yellow);
    log('  3. Verify new content appears in tests', colors.yellow);
    log('  4. Upload OIR batch_002 part2 (50 more questions) when ready', colors.yellow);
  } else {
    log('\n⚠️  SOME UPLOADS FAILED', colors.red + colors.bright);
    log('Please check the error messages above and retry failed uploads.', colors.yellow);
  }
  
  log('\n' + '='.repeat(60), colors.bright);
}

// Main execution
(async () => {
  try {
    await uploadAllBatches();
    process.exit(0);
  } catch (error) {
    log('\n💥 Fatal error:', colors.red + colors.bright);
    console.error(error);
    process.exit(1);
  }
})();

