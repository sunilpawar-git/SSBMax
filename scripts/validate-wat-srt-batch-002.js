#!/usr/bin/env node

/**
 * Comprehensive WAT & SRT Batch Validator
 * 
 * Validates batch_002 JSON files BEFORE upload to Firestore
 * Prevents field name typos, malformed IDs, and formatting issues
 * Modeled after OIR validation to catch all errors we witnessed
 * 
 * Usage: node validate-wat-srt-batch-002.js
 */

const fs = require('fs');
const path = require('path');

// Validation counters
let totalErrors = 0;
let totalWarnings = 0;

// Allowed categories for validation
const WAT_CATEGORIES = [
  'positive_military_traits',
  'challenging_situations',
  'neutral_general',
  'character_traits'
];

const SRT_CATEGORIES = [
  'leadership',
  'decision_making',
  'crisis_management',
  'ethical_dilemma',
  'responsibility',
  'teamwork',
  'interpersonal',
  'courage',
  'general'
];

const DIFFICULTIES = ['easy', 'medium', 'hard'];

/**
 * Validate WAT word object
 */
function validateWATWord(word, index) {
  const errors = [];
  const warnings = [];
  
  // 1. Validate ID format
  if (!word.id || typeof word.id !== 'string') {
    errors.push(`Word #${index + 1}: Missing or invalid 'id' field`);
  } else {
    // Check pattern: wat_w_XXXX (4 digits)
    const idPattern = /^wat_w_\d{4}$/;
    if (!idPattern.test(word.id)) {
      errors.push(`Word #${index + 1}: ID '${word.id}' doesn't match pattern 'wat_w_XXXX' (4 digits)`);
    }
  }
  
  // 2. Validate 'word' field (NOT 'Word' with capital)
  if (!word.hasOwnProperty('word')) {
    errors.push(`Word #${index + 1}: Missing 'word' field`);
    // Check for common typo
    if (word.hasOwnProperty('Word')) {
      errors.push(`Word #${index + 1}: Found 'Word' (capital W) - should be 'word' (lowercase)`);
    }
  } else if (typeof word.word !== 'string' || word.word.trim().length === 0) {
    errors.push(`Word #${index + 1}: 'word' field is empty`);
  } else if (word.word.length < 3) {
    warnings.push(`Word #${index + 1}: Word '${word.word}' is very short (< 3 chars)`);
  } else if (word.word.length > 50) {
    warnings.push(`Word #${index + 1}: Word '${word.word}' is very long (> 50 chars)`);
  }
  
  // Check for HTML tags
  if (word.word && /<[^>]*>/.test(word.word)) {
    errors.push(`Word #${index + 1}: Word contains HTML tags: '${word.word}'`);
  }
  
  // 3. Validate sequenceNumber (NOT sequence_number)
  if (!word.hasOwnProperty('sequenceNumber')) {
    errors.push(`Word #${index + 1}: Missing 'sequenceNumber' field`);
    // Check for common typos
    if (word.hasOwnProperty('sequence_number')) {
      errors.push(`Word #${index + 1}: Found 'sequence_number' (snake_case) - should be 'sequenceNumber' (camelCase)`);
    }
  } else if (typeof word.sequenceNumber !== 'number' || word.sequenceNumber <= 0) {
    errors.push(`Word #${index + 1}: Invalid sequenceNumber: ${word.sequenceNumber} (must be > 0)`);
  }
  
  // 4. Validate timeAllowedSeconds
  if (!word.hasOwnProperty('timeAllowedSeconds')) {
    errors.push(`Word #${index + 1}: Missing 'timeAllowedSeconds' field`);
    // Check for common typos
    if (word.hasOwnProperty('time_allowed_seconds') || word.hasOwnProperty('timeAllowed')) {
      errors.push(`Word #${index + 1}: Found incorrect field name - should be 'timeAllowedSeconds'`);
    }
  } else if (word.timeAllowedSeconds !== 15) {
    errors.push(`Word #${index + 1}: timeAllowedSeconds is ${word.timeAllowedSeconds} (must be 15 for WAT)`);
  }
  
  // 5. Validate category
  if (!word.hasOwnProperty('category')) {
    errors.push(`Word #${index + 1}: Missing 'category' field`);
  } else if (!WAT_CATEGORIES.includes(word.category)) {
    errors.push(`Word #${index + 1}: Unknown category '${word.category}'. Allowed: ${WAT_CATEGORIES.join(', ')}`);
  }
  
  // 6. Validate difficulty
  if (!word.hasOwnProperty('difficulty')) {
    errors.push(`Word #${index + 1}: Missing 'difficulty' field`);
  } else if (!DIFFICULTIES.includes(word.difficulty)) {
    errors.push(`Word #${index + 1}: Invalid difficulty '${word.difficulty}'. Must be: ${DIFFICULTIES.join(', ')}`);
  }
  
  // 7. Check for unexpected fields (typos from copy-paste)
  const expectedFields = ['id', 'word', 'sequenceNumber', 'timeAllowedSeconds', 'category', 'difficulty'];
  const actualFields = Object.keys(word);
  const unexpectedFields = actualFields.filter(f => !expectedFields.includes(f));
  if (unexpectedFields.length > 0) {
    warnings.push(`Word #${index + 1}: Unexpected fields: ${unexpectedFields.join(', ')}`);
  }
  
  return { errors, warnings, word };
}

/**
 * Validate SRT situation object
 */
function validateSRTSituation(situation, index) {
  const errors = [];
  const warnings = [];
  
  // 1. Validate ID format
  if (!situation.id || typeof situation.id !== 'string') {
    errors.push(`Situation #${index + 1}: Missing or invalid 'id' field`);
  } else {
    // Check pattern: srt_s_XXXX (4 digits)
    const idPattern = /^srt_s_\d{4}$/;
    if (!idPattern.test(situation.id)) {
      errors.push(`Situation #${index + 1}: ID '${situation.id}' doesn't match pattern 'srt_s_XXXX' (4 digits)`);
    }
  }
  
  // 2. Validate 'situation' field (NOT 'Situation' with capital)
  if (!situation.hasOwnProperty('situation')) {
    errors.push(`Situation #${index + 1}: Missing 'situation' field`);
    // Check for common typo
    if (situation.hasOwnProperty('Situation')) {
      errors.push(`Situation #${index + 1}: Found 'Situation' (capital S) - should be 'situation' (lowercase)`);
    }
  } else if (typeof situation.situation !== 'string' || situation.situation.trim().length === 0) {
    errors.push(`Situation #${index + 1}: 'situation' field is empty`);
  } else if (situation.situation.length < 20) {
    warnings.push(`Situation #${index + 1}: Situation text is very short (< 20 chars): '${situation.situation.substring(0, 50)}...'`);
  } else if (situation.situation.length > 500) {
    warnings.push(`Situation #${index + 1}: Situation text is very long (> 500 chars)`);
  }
  
  // Check if ends with punctuation
  if (situation.situation && !/[.?!]$/.test(situation.situation.trim())) {
    warnings.push(`Situation #${index + 1}: Situation doesn't end with punctuation (., ?, !)`);
  }
  
  // Check for HTML tags
  if (situation.situation && /<[^>]*>/.test(situation.situation)) {
    errors.push(`Situation #${index + 1}: Situation contains HTML tags`);
  }
  
  // 3. Validate sequenceNumber
  if (!situation.hasOwnProperty('sequenceNumber')) {
    errors.push(`Situation #${index + 1}: Missing 'sequenceNumber' field`);
    // Check for common typos
    if (situation.hasOwnProperty('sequence_number')) {
      errors.push(`Situation #${index + 1}: Found 'sequence_number' (snake_case) - should be 'sequenceNumber' (camelCase)`);
    }
  } else if (typeof situation.sequenceNumber !== 'number' || situation.sequenceNumber <= 0) {
    errors.push(`Situation #${index + 1}: Invalid sequenceNumber: ${situation.sequenceNumber} (must be > 0)`);
  }
  
  // 4. Validate timeAllowedSeconds
  if (!situation.hasOwnProperty('timeAllowedSeconds')) {
    errors.push(`Situation #${index + 1}: Missing 'timeAllowedSeconds' field`);
  } else if (situation.timeAllowedSeconds !== 30) {
    errors.push(`Situation #${index + 1}: timeAllowedSeconds is ${situation.timeAllowedSeconds} (must be 30 for SRT)`);
  }
  
  // 5. Validate category
  if (!situation.hasOwnProperty('category')) {
    errors.push(`Situation #${index + 1}: Missing 'category' field`);
  } else if (!SRT_CATEGORIES.includes(situation.category)) {
    errors.push(`Situation #${index + 1}: Unknown category '${situation.category}'. Allowed: ${SRT_CATEGORIES.join(', ')}`);
    // Check for common typos
    if (situation.category === 'decision-making') {
      errors.push(`Situation #${index + 1}: Use 'decision_making' not 'decision-making' (underscore not hyphen)`);
    }
  }
  
  // 6. Validate difficulty
  if (!situation.hasOwnProperty('difficulty')) {
    errors.push(`Situation #${index + 1}: Missing 'difficulty' field`);
  } else if (!DIFFICULTIES.includes(situation.difficulty)) {
    errors.push(`Situation #${index + 1}: Invalid difficulty '${situation.difficulty}'. Must be: ${DIFFICULTIES.join(', ')}`);
  }
  
  // 7. Check for unexpected fields
  const expectedFields = ['id', 'situation', 'sequenceNumber', 'category', 'timeAllowedSeconds', 'difficulty'];
  const actualFields = Object.keys(situation);
  const unexpectedFields = actualFields.filter(f => !expectedFields.includes(f));
  if (unexpectedFields.length > 0) {
    warnings.push(`Situation #${index + 1}: Unexpected fields: ${unexpectedFields.join(', ')}`);
  }
  
  return { errors, warnings, situation };
}

/**
 * Validate WAT batch file
 */
function validateWATBatch() {
  console.log('📝 Validating WAT batch_002...\n');
  
  const filePath = path.join(__dirname, 'wat-batch-002.json');
  
  if (!fs.existsSync(filePath)) {
    console.error('❌ ERROR: wat-batch-002.json not found in scripts directory');
    return { success: false, errors: 1 };
  }
  
  let batchData;
  try {
    batchData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch (error) {
    console.error('❌ ERROR: Failed to parse wat-batch-002.json:', error.message);
    return { success: false, errors: 1 };
  }
  
  const errors = [];
  const warnings = [];
  
  // Validate metadata
  if (!batchData.metadata) {
    errors.push('Missing metadata object');
  } else {
    if (batchData.metadata.total_words !== 40) {
      errors.push(`Metadata total_words is ${batchData.metadata.total_words} (expected 40)`);
    }
  }
  
  // Validate batch_id
  if (batchData.batch_id !== 'batch_002') {
    errors.push(`batch_id is '${batchData.batch_id}' (expected 'batch_002')`);
  }
  
  // Validate words array
  if (!Array.isArray(batchData.words)) {
    errors.push('words is not an array');
    console.error(`\n❌ WAT Validation: FAILED (${errors.length} errors)\n`);
    errors.forEach(err => console.error(`   ${err}`));
    return { success: false, errors: errors.length };
  }
  
  if (batchData.words.length !== 40) {
    errors.push(`words array has ${batchData.words.length} items (expected 40)`);
  }
  
  // Validate each word
  const seenIds = new Set();
  const seenSequences = new Set();
  
  batchData.words.forEach((word, index) => {
    const result = validateWATWord(word, index);
    
    // Check for duplicate IDs
    if (word.id) {
      if (seenIds.has(word.id)) {
        result.errors.push(`Duplicate ID: ${word.id}`);
      }
      seenIds.add(word.id);
    }
    
    // Check for duplicate sequence numbers
    if (word.sequenceNumber) {
      if (seenSequences.has(word.sequenceNumber)) {
        result.errors.push(`Duplicate sequenceNumber: ${word.sequenceNumber}`);
      }
      seenSequences.add(word.sequenceNumber);
    }
    
    errors.push(...result.errors);
    warnings.push(...result.warnings);
  });
  
  // Validate sequence continuity (should be 61-100)
  const sequences = Array.from(seenSequences).sort((a, b) => a - b);
  const expectedMin = 61;
  const expectedMax = 100;
  
  if (sequences.length > 0) {
    if (sequences[0] !== expectedMin) {
      errors.push(`First sequence number is ${sequences[0]} (expected ${expectedMin})`);
    }
    if (sequences[sequences.length - 1] !== expectedMax) {
      errors.push(`Last sequence number is ${sequences[sequences.length - 1]} (expected ${expectedMax})`);
    }
    
    // Check for gaps
    for (let i = 0; i < sequences.length - 1; i++) {
      if (sequences[i + 1] !== sequences[i] + 1) {
        errors.push(`Sequence gap: ${sequences[i]} -> ${sequences[i + 1]} (missing ${sequences[i] + 1})`);
      }
    }
  }
  
  // Print results
  if (errors.length === 0) {
    console.log('✅ WAT Validation: PASS (0 errors, ' + warnings.length + ' warnings)');
    console.log(`   - ${batchData.words.length} words validated`);
    console.log(`   - Sequence: ${sequences[0]}-${sequences[sequences.length - 1]} (continuous)`);
    console.log('   - All fields correct\n');
    
    if (warnings.length > 0) {
      console.log('⚠️  Warnings:');
      warnings.slice(0, 5).forEach(warn => console.log(`   ${warn}`));
      if (warnings.length > 5) {
        console.log(`   ... and ${warnings.length - 5} more warnings\n`);
      }
    }
  } else {
    console.error(`\n❌ WAT Validation: FAILED (${errors.length} errors, ${warnings.length} warnings)\n`);
    console.error('Errors:');
    errors.forEach(err => console.error(`   ${err}`));
    
    if (warnings.length > 0) {
      console.error('\nWarnings:');
      warnings.slice(0, 5).forEach(warn => console.error(`   ${warn}`));
    }
    console.error('\n⚠️  Fix errors before uploading!\n');
  }
  
  totalErrors += errors.length;
  totalWarnings += warnings.length;
  
  return { success: errors.length === 0, errors: errors.length, warnings: warnings.length };
}

/**
 * Validate SRT batch file
 */
function validateSRTBatch() {
  console.log('📝 Validating SRT batch_002...\n');
  
  const filePath = path.join(__dirname, 'srt-batch-002.json');
  
  if (!fs.existsSync(filePath)) {
    console.error('❌ ERROR: srt-batch-002.json not found in scripts directory');
    return { success: false, errors: 1 };
  }
  
  let batchData;
  try {
    batchData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch (error) {
    console.error('❌ ERROR: Failed to parse srt-batch-002.json:', error.message);
    return { success: false, errors: 1 };
  }
  
  const errors = [];
  const warnings = [];
  
  // Validate metadata
  if (!batchData.metadata) {
    errors.push('Missing metadata object');
  } else {
    if (batchData.metadata.total_situations !== 30) {
      errors.push(`Metadata total_situations is ${batchData.metadata.total_situations} (expected 30)`);
    }
  }
  
  // Validate batch_id
  if (batchData.batch_id !== 'batch_002') {
    errors.push(`batch_id is '${batchData.batch_id}' (expected 'batch_002')`);
  }
  
  // Validate situations array
  if (!Array.isArray(batchData.situations)) {
    errors.push('situations is not an array');
    console.error(`\n❌ SRT Validation: FAILED (${errors.length} errors)\n`);
    errors.forEach(err => console.error(`   ${err}`));
    return { success: false, errors: errors.length };
  }
  
  if (batchData.situations.length !== 30) {
    errors.push(`situations array has ${batchData.situations.length} items (expected 30)`);
  }
  
  // Validate each situation
  const seenIds = new Set();
  const seenSequences = new Set();
  const categoryCount = {};
  
  batchData.situations.forEach((situation, index) => {
    const result = validateSRTSituation(situation, index);
    
    // Check for duplicate IDs
    if (situation.id) {
      if (seenIds.has(situation.id)) {
        result.errors.push(`Duplicate ID: ${situation.id}`);
      }
      seenIds.add(situation.id);
    }
    
    // Check for duplicate sequence numbers
    if (situation.sequenceNumber) {
      if (seenSequences.has(situation.sequenceNumber)) {
        result.errors.push(`Duplicate sequenceNumber: ${situation.sequenceNumber}`);
      }
      seenSequences.add(situation.sequenceNumber);
    }
    
    // Count categories
    if (situation.category) {
      categoryCount[situation.category] = (categoryCount[situation.category] || 0) + 1;
    }
    
    errors.push(...result.errors);
    warnings.push(...result.warnings);
  });
  
  // Validate sequence continuity (should be 61-90)
  const sequences = Array.from(seenSequences).sort((a, b) => a - b);
  const expectedMin = 61;
  const expectedMax = 90;
  
  if (sequences.length > 0) {
    if (sequences[0] !== expectedMin) {
      errors.push(`First sequence number is ${sequences[0]} (expected ${expectedMin})`);
    }
    if (sequences[sequences.length - 1] !== expectedMax) {
      errors.push(`Last sequence number is ${sequences[sequences.length - 1]} (expected ${expectedMax})`);
    }
    
    // Check for gaps
    for (let i = 0; i < sequences.length - 1; i++) {
      if (sequences[i + 1] !== sequences[i] + 1) {
        errors.push(`Sequence gap: ${sequences[i]} -> ${sequences[i + 1]} (missing ${sequences[i] + 1})`);
      }
    }
  }
  
  // Print results
  if (errors.length === 0) {
    console.log('✅ SRT Validation: PASS (0 errors, ' + warnings.length + ' warnings)');
    console.log(`   - ${batchData.situations.length} situations validated`);
    console.log(`   - Sequence: ${sequences[0]}-${sequences[sequences.length - 1]} (continuous)`);
    console.log('   - All fields correct');
    console.log(`   - Categories: ${Object.keys(categoryCount).length} types`);
    Object.entries(categoryCount).forEach(([cat, count]) => {
      console.log(`     • ${cat}: ${count}`);
    });
    console.log('');
    
    if (warnings.length > 0) {
      console.log('⚠️  Warnings:');
      warnings.slice(0, 5).forEach(warn => console.log(`   ${warn}`));
      if (warnings.length > 5) {
        console.log(`   ... and ${warnings.length - 5} more warnings\n`);
      }
    }
  } else {
    console.error(`\n❌ SRT Validation: FAILED (${errors.length} errors, ${warnings.length} warnings)\n`);
    console.error('Errors:');
    errors.forEach(err => console.error(`   ${err}`));
    
    if (warnings.length > 0) {
      console.error('\nWarnings:');
      warnings.slice(0, 5).forEach(warn => console.error(`   ${warn}`));
    }
    console.error('\n⚠️  Fix errors before uploading!\n');
  }
  
  totalErrors += errors.length;
  totalWarnings += warnings.length;
  
  return { success: errors.length === 0, errors: errors.length, warnings: warnings.length };
}

/**
 * Main validation
 */
function main() {
  console.log('=' .repeat(60));
  console.log('🔍 WAT/SRT BATCH_002 VALIDATION');
  console.log('=' .repeat(60));
  console.log('');
  
  const watResult = validateWATBatch();
  const srtResult = validateSRTBatch();
  
  console.log('=' .repeat(60));
  console.log('📊 VALIDATION SUMMARY');
  console.log('=' .repeat(60));
  console.log('');
  
  const allSuccess = watResult.success && srtResult.success;
  
  console.log(`WAT: ${watResult.success ? '✅ PASS' : '❌ FAIL'} (${watResult.errors} errors, ${watResult.warnings} warnings)`);
  console.log(`SRT: ${srtResult.success ? '✅ PASS' : '❌ FAIL'} (${srtResult.errors} errors, ${srtResult.warnings} warnings)`);
  console.log('');
  console.log(`Total: ${totalErrors} errors, ${totalWarnings} warnings`);
  console.log('');
  
  if (allSuccess) {
    console.log('🎉 Ready for upload!');
    console.log('');
    console.log('Next steps:');
    console.log('  1. Run: node scripts/upload-wat-batch-002.js');
    console.log('  2. Run: node scripts/upload-srt-batch-002.js');
    console.log('  3. Run: node scripts/verify-batch-002-upload.js');
    console.log('');
    process.exit(0);
  } else {
    console.log('❌ VALIDATION FAILED');
    console.log('');
    console.log('⚠️  Fix all errors before uploading to Firestore!');
    console.log('');
    process.exit(1);
  }
}

// Run validation
main();


