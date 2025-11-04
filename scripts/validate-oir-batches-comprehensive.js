#!/usr/bin/env node

/**
 * Comprehensive OIR Batch Validator
 * 
 * Implements the same validation logic as OIRQuestionValidator.kt
 * to verify all OIR batches in Firestore BEFORE they cause runtime issues.
 * 
 * This script catches ALL the errors we've encountered:
 * - Missing or empty questionText
 * - Malformed option IDs (single letter vs "opt_X")
 * - Malformed correctAnswerId (e.g., "opt_103_b")
 * - Missing options
 * - Duplicate option IDs
 * - CorrectAnswerId not matching any option
 * - Empty option text
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Validation counters
let totalQuestions = 0;
let validQuestions = 0;
let invalidQuestions = 0;
let totalErrors = 0;
let totalWarnings = 0;

/**
 * Validates a single OIR question
 */
function validateQuestion(question, batchName) {
  const errors = [];
  const warnings = [];
  
  // 1. Validate Question ID
  if (!question.id || question.id.trim() === '') {
    errors.push("Question ID is blank");
  } else if (!isValidQuestionId(question.id)) {
    warnings.push(`Question ID format unusual: ${question.id} (expected: oir_XXX or oir_q_XXXX)`);
  }
  
  // 2. Validate Question Number
  if (!question.questionNumber || question.questionNumber <= 0) {
    errors.push(`Invalid question number: ${question.questionNumber} (must be > 0)`);
  }
  
  // 3. Validate Question Text
  if (!question.questionText || question.questionText.trim() === '') {
    errors.push("Question text is empty or blank");
  } else if (question.questionText.length < 10) {
    warnings.push(`Question text suspiciously short (${question.questionText.length} chars): '${question.questionText.substring(0, 50)}...'`);
  }
  
  // Check for field name typos (common issue we fixed)
  if (question.question && !question.questionText) {
    errors.push("Has 'question' field but missing 'questionText' - field name typo!");
  }
  
  // 4. Validate Options List
  if (!question.options || question.options.length === 0) {
    errors.push("No options provided");
  } else {
    // Check option count (should be 4 for MCQ)
    if (question.options.length !== 4) {
      warnings.push(`Unusual number of options: ${question.options.length} (expected 4)`);
    }
    
    // Check for duplicate option IDs
    const optionIds = question.options.map(opt => opt.id);
    const duplicates = optionIds.filter((id, index) => optionIds.indexOf(id) !== index);
    if (duplicates.length > 0) {
      errors.push(`Duplicate option IDs found: ${[...new Set(duplicates)]}`);
    }
    
    // Validate each option
    question.options.forEach((option, index) => {
      validateOption(option, index, errors, warnings);
    });
  }
  
  // 5. Validate CorrectAnswerId Format
  if (!question.correctAnswerId || question.correctAnswerId.trim() === '') {
    errors.push("CorrectAnswerId is empty");
  } else {
    // Check for common malformed patterns
    
    // Single letter format (e.g., "a", "b", "c", "d")
    if (question.correctAnswerId.length === 1 && /^[a-dA-D]$/.test(question.correctAnswerId)) {
      errors.push(`CorrectAnswerId is single letter '${question.correctAnswerId}' (should be 'opt_${question.correctAnswerId.toLowerCase()}')`);
    }
    
    // Malformed format with question number (e.g., "opt_103_b")
    else if (/^opt_\d+_[a-d]$/.test(question.correctAnswerId)) {
      const correctFormat = question.correctAnswerId.split('_').pop();
      errors.push(`CorrectAnswerId has question number embedded: '${question.correctAnswerId}' (should be 'opt_${correctFormat}')`);
    }
    
    // Check if it follows standard format
    else if (!/^opt_[a-d]$/.test(question.correctAnswerId)) {
      warnings.push(`CorrectAnswerId format unusual: '${question.correctAnswerId}' (expected: opt_a, opt_b, opt_c, or opt_d)`);
    }
    
    // 6. Validate CorrectAnswerId Matches an Option
    if (question.options && question.options.length > 0) {
      const correctOptionExists = question.options.some(opt => opt.id === question.correctAnswerId);
      if (!correctOptionExists) {
        const availableIds = question.options.map(opt => opt.id).join(', ');
        errors.push(`CorrectAnswerId '${question.correctAnswerId}' does not match any option ID. Available: [${availableIds}]`);
      }
    }
  }
  
  // 7. Validate Type
  const validTypes = ['VERBAL_REASONING', 'NON_VERBAL_REASONING', 'NUMERICAL_ABILITY', 'SPATIAL_REASONING'];
  if (!question.type || !validTypes.includes(question.type)) {
    errors.push(`Invalid type: ${question.type} (expected one of: ${validTypes.join(', ')})`);
  }
  
  // 8. Validate Difficulty
  const validDifficulties = ['EASY', 'MEDIUM', 'HARD'];
  if (!question.difficulty || !validDifficulties.includes(question.difficulty)) {
    warnings.push(`Invalid difficulty: ${question.difficulty} (expected: EASY, MEDIUM, or HARD)`);
  }
  
  // 9. Validate Time Allocation
  if (!question.timeSeconds || question.timeSeconds <= 0) {
    warnings.push(`Time allocation invalid: ${question.timeSeconds}s (should be > 0)`);
  } else if (question.timeSeconds > 300) {
    warnings.push(`Time allocation unusually high: ${question.timeSeconds}s (>5 minutes)`);
  }
  
  return {
    isValid: errors.length === 0,
    questionId: question.id,
    errors,
    warnings
  };
}

/**
 * Validates a single option
 */
function validateOption(option, index, errors, warnings) {
  // Check option ID format
  if (!option.id || option.id.trim() === '') {
    errors.push(`Option #${index + 1} has blank ID`);
  } else {
    // Single letter ID (corrupted data)
    if (option.id.length === 1 && /^[a-dA-D]$/.test(option.id)) {
      errors.push(`Option #${index + 1} has single-letter ID '${option.id}' (should be 'opt_${option.id.toLowerCase()}')`);
    }
    
    // Standard format check
    else if (!/^opt_[a-d]$/.test(option.id) && !/^opt_\d+[a-d]$/.test(option.id)) {
      warnings.push(`Option #${index + 1} ID format unusual: '${option.id}' (expected: opt_a, opt_b, opt_c, or opt_d)`);
    }
  }
  
  // Check option text
  if (!option.text || option.text.trim() === '') {
    errors.push(`Option '${option.id}' has empty text`);
  } else if (option.text.length < 1) {
    warnings.push(`Option '${option.id}' has very short text: '${option.text}'`);
  }
}

/**
 * Checks if question ID follows expected patterns
 */
function isValidQuestionId(id) {
  return /^oir_\d+$/.test(id) || // Legacy: oir_103
         /^oir_q_\d{4}$/.test(id); // Standard: oir_q_0103
}

/**
 * Validates a batch of questions from Firestore
 */
async function validateBatch(batchName) {
  console.log(`\n${'='.repeat(70)}`);
  console.log(`VALIDATING BATCH: ${batchName}`);
  console.log('='.repeat(70));
  
  try {
    const batchRef = db.collection('test_content/oir/question_batches').doc(batchName);
    const batchDoc = await batchRef.get();
    
    if (!batchDoc.exists) {
      console.log(`❌ Batch not found: ${batchName}`);
      return;
    }
    
    const questions = batchDoc.data().questions || [];
    console.log(`📊 Total questions in batch: ${questions.length}`);
    
    const batchResults = [];
    let batchValidCount = 0;
    let batchInvalidCount = 0;
    let batchErrors = 0;
    let batchWarnings = 0;
    
    questions.forEach((question, index) => {
      const result = validateQuestion(question, batchName);
      batchResults.push(result);
      
      totalQuestions++;
      
      if (result.isValid) {
        validQuestions++;
        batchValidCount++;
      } else {
        invalidQuestions++;
        batchInvalidCount++;
      }
      
      totalErrors += result.errors.length;
      totalWarnings += result.warnings.length;
      batchErrors += result.errors.length;
      batchWarnings += result.warnings.length;
      
      // Print errors immediately
      if (result.errors.length > 0) {
        console.log(`\n❌ Question ${result.questionId} (index ${index}):`);
        result.errors.forEach(err => console.log(`   ERROR: ${err}`));
      }
      
      // Print warnings if verbose
      if (result.warnings.length > 0 && process.argv.includes('--verbose')) {
        console.log(`\n⚠️  Question ${result.questionId} (index ${index}):`);
        result.warnings.forEach(warn => console.log(`   WARNING: ${warn}`));
      }
    });
    
    // Batch summary
    console.log(`\n${'─'.repeat(70)}`);
    console.log(`BATCH SUMMARY: ${batchName}`);
    console.log(`─`.repeat(70));
    console.log(`✅ Valid:     ${batchValidCount}/${questions.length} (${Math.round(batchValidCount * 100 / questions.length)}%)`);
    console.log(`❌ Invalid:   ${batchInvalidCount}/${questions.length}`);
    console.log(`⚠️  Errors:    ${batchErrors}`);
    console.log(`⚠️  Warnings:  ${batchWarnings}`);
    
    if (batchInvalidCount === 0) {
      console.log(`\n🎉 BATCH ${batchName} IS 100% VALID! 🎉`);
    } else {
      console.log(`\n⚠️  BATCH ${batchName} HAS ${batchInvalidCount} INVALID QUESTIONS!`);
    }
    
  } catch (error) {
    console.error(`❌ Error validating batch ${batchName}:`, error);
  }
}

/**
 * Main function
 */
async function main() {
  console.log('🔍 COMPREHENSIVE OIR BATCH VALIDATOR');
  console.log('=====================================\n');
  console.log('This validator checks for ALL data corruption issues:');
  console.log('  ✓ Missing/empty questionText');
  console.log('  ✓ Malformed option IDs (single letter vs opt_X)');
  console.log('  ✓ Malformed correctAnswerId (e.g., opt_103_b)');
  console.log('  ✓ Missing options');
  console.log('  ✓ Duplicate option IDs');
  console.log('  ✓ CorrectAnswerId not matching options');
  console.log('  ✓ Empty option text');
  console.log('  ✓ Invalid types and difficulties\n');
  
  const batches = ['batch_001', 'batch_002', 'batch_003', 'batch_004'];
  
  for (const batch of batches) {
    await validateBatch(batch);
  }
  
  // Overall summary
  console.log(`\n${'='.repeat(70)}`);
  console.log('OVERALL VALIDATION SUMMARY');
  console.log('='.repeat(70));
  console.log(`Total Questions Validated: ${totalQuestions}`);
  console.log(`✅ Valid Questions:         ${validQuestions} (${Math.round(validQuestions * 100 / totalQuestions)}%)`);
  console.log(`❌ Invalid Questions:       ${invalidQuestions}`);
  console.log(`⚠️  Total Errors:            ${totalErrors}`);
  console.log(`⚠️  Total Warnings:          ${totalWarnings}`);
  console.log('='.repeat(70));
  
  if (invalidQuestions === 0) {
    console.log('\n✅✅✅ ALL BATCHES ARE 100% VALID! ✅✅✅');
    console.log('🎉 Ready for production!');
    process.exit(0);
  } else {
    console.log(`\n❌ FOUND ${invalidQuestions} INVALID QUESTIONS ACROSS ALL BATCHES`);
    console.log('⚠️  These questions will be automatically filtered out during test loading.');
    console.log('⚠️  Consider fixing them in Firestore for best quality.');
    process.exit(1);
  }
}

// Run the validator
main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});

