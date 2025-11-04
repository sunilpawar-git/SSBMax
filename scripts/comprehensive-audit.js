#!/usr/bin/env node
/**
 * COMPREHENSIVE AUDIT of all OIR batches
 * Check for ALL possible data quality issues
 */

const admin = require('firebase-admin');
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function comprehensiveAudit() {
  console.log('🔍 COMPREHENSIVE OIR BATCH AUDIT\n');
  console.log('='.repeat(70));
  
  try {
    const batchesSnapshot = await db
      .collection('test_content/oir/question_batches')
      .get();
    
    let totalIssues = 0;
    
    for (const doc of batchesSnapshot.docs) {
      const batchId = doc.id;
      const data = doc.data();
      const questions = data.questions || [];
      
      console.log(`\n📦 BATCH: ${batchId}`);
      console.log(`   Questions: ${questions.length}`);
      console.log(`   Version: ${data.version || 'N/A'}`);
      console.log('─'.repeat(70));
      
      let batchIssues = 0;
      
      // ============ CHECK 1: Required Fields ============
      console.log('\n✓ CHECK 1: Required Fields');
      const missingFields = questions.filter(q => 
        !q.id || !q.type || !q.questionText || !q.options || !q.correctAnswerId || !q.difficulty
      );
      if (missingFields.length > 0) {
        console.log(`  ❌ ${missingFields.length} questions missing required fields:`);
        missingFields.forEach(q => console.log(`     - ${q.id || 'NO_ID'}`));
        batchIssues += missingFields.length;
      } else {
        console.log('  ✅ All questions have required fields');
      }
      
      // ============ CHECK 2: Option ID Format ============
      console.log('\n✓ CHECK 2: Option ID Format (must be opt_a, opt_b, opt_c, opt_d)');
      const wrongOptionFormat = questions.filter(q => {
        if (!q.options || q.options.length === 0) return false;
        return q.options.some(opt => 
          !opt.id || !/^opt_[a-d]$/.test(opt.id)
        );
      });
      if (wrongOptionFormat.length > 0) {
        console.log(`  ❌ ${wrongOptionFormat.length} questions with wrong option ID format:`);
        wrongOptionFormat.forEach(q => {
          console.log(`     - ${q.id}: ${q.options.map(o => o.id).join(', ')}`);
        });
        batchIssues += wrongOptionFormat.length;
      } else {
        console.log('  ✅ All options use correct ID format');
      }
      
      // ============ CHECK 3: Correct Answer Match ============
      console.log('\n✓ CHECK 3: correctAnswerId Matches Available Options');
      const mismatchedAnswer = questions.filter(q => {
        const optionIds = q.options ? q.options.map(o => o.id) : [];
        return q.correctAnswerId && !optionIds.includes(q.correctAnswerId);
      });
      if (mismatchedAnswer.length > 0) {
        console.log(`  ❌ ${mismatchedAnswer.length} questions with mismatched correctAnswerId:`);
        mismatchedAnswer.forEach(q => {
          console.log(`     - ${q.id}:`);
          console.log(`       correctAnswerId: "${q.correctAnswerId}"`);
          console.log(`       Available: ${q.options.map(o => o.id).join(', ')}`);
        });
        batchIssues += mismatchedAnswer.length;
      } else {
        console.log('  ✅ All correctAnswerId fields match available options');
      }
      
      // ============ CHECK 4: Option Count ============
      console.log('\n✓ CHECK 4: Option Count (must have exactly 4 options)');
      const wrongOptionCount = questions.filter(q => 
        !q.options || q.options.length !== 4
      );
      if (wrongOptionCount.length > 0) {
        console.log(`  ❌ ${wrongOptionCount.length} questions with wrong option count:`);
        wrongOptionCount.forEach(q => {
          console.log(`     - ${q.id}: ${q.options?.length || 0} options`);
        });
        batchIssues += wrongOptionCount.length;
      } else {
        console.log('  ✅ All questions have exactly 4 options');
      }
      
      // ============ CHECK 5: Empty Option Text ============
      console.log('\n✓ CHECK 5: Option Text (no empty text)');
      const emptyOptionText = questions.filter(q => 
        q.options && q.options.some(opt => !opt.text || opt.text.trim() === '')
      );
      if (emptyOptionText.length > 0) {
        console.log(`  ❌ ${emptyOptionText.length} questions with empty option text:`);
        emptyOptionText.forEach(q => console.log(`     - ${q.id}`));
        batchIssues += emptyOptionText.length;
      } else {
        console.log('  ✅ All options have text');
      }
      
      // ============ CHECK 6: Question Text ============
      console.log('\n✓ CHECK 6: Question Text (not empty or null)');
      const emptyQuestionText = questions.filter(q => 
        !q.questionText || q.questionText.trim() === ''
      );
      if (emptyQuestionText.length > 0) {
        console.log(`  ❌ ${emptyQuestionText.length} questions with empty question text:`);
        emptyQuestionText.forEach(q => console.log(`     - ${q.id}`));
        batchIssues += emptyQuestionText.length;
      } else {
        console.log('  ✅ All questions have text');
      }
      
      // ============ CHECK 7: Valid Type ============
      console.log('\n✓ CHECK 7: Question Type (VERBAL_REASONING, NON_VERBAL_REASONING, etc.)');
      const validTypes = ['VERBAL_REASONING', 'NON_VERBAL_REASONING', 'NUMERICAL_ABILITY', 'SPATIAL_REASONING'];
      const invalidType = questions.filter(q => !validTypes.includes(q.type));
      if (invalidType.length > 0) {
        console.log(`  ❌ ${invalidType.length} questions with invalid type:`);
        invalidType.forEach(q => console.log(`     - ${q.id}: "${q.type}"`));
        batchIssues += invalidType.length;
      } else {
        console.log('  ✅ All questions have valid type');
      }
      
      // ============ CHECK 8: Valid Difficulty ============
      console.log('\n✓ CHECK 8: Difficulty Level (EASY, MEDIUM, HARD)');
      const validDifficulties = ['EASY', 'MEDIUM', 'HARD'];
      const invalidDifficulty = questions.filter(q => !validDifficulties.includes(q.difficulty));
      if (invalidDifficulty.length > 0) {
        console.log(`  ❌ ${invalidDifficulty.length} questions with invalid difficulty:`);
        invalidDifficulty.forEach(q => console.log(`     - ${q.id}: "${q.difficulty}"`));
        batchIssues += invalidDifficulty.length;
      } else {
        console.log('  ✅ All questions have valid difficulty');
      }
      
      // ============ CHECK 9: Duplicate IDs ============
      console.log('\n✓ CHECK 9: Duplicate Question IDs');
      const idCounts = {};
      questions.forEach(q => {
        idCounts[q.id] = (idCounts[q.id] || 0) + 1;
      });
      const duplicates = Object.entries(idCounts).filter(([id, count]) => count > 1);
      if (duplicates.length > 0) {
        console.log(`  ❌ ${duplicates.length} duplicate question IDs:`);
        duplicates.forEach(([id, count]) => console.log(`     - ${id}: appears ${count} times`));
        batchIssues += duplicates.length;
      } else {
        console.log('  ✅ All question IDs are unique');
      }
      
      // ============ CHECK 10: Duplicate Option IDs within Question ============
      console.log('\n✓ CHECK 10: Duplicate Option IDs within Questions');
      const duplicateOptions = questions.filter(q => {
        if (!q.options) return false;
        const optIds = q.options.map(o => o.id);
        return new Set(optIds).size !== optIds.length;
      });
      if (duplicateOptions.length > 0) {
        console.log(`  ❌ ${duplicateOptions.length} questions with duplicate option IDs:`);
        duplicateOptions.forEach(q => {
          console.log(`     - ${q.id}: ${q.options.map(o => o.id).join(', ')}`);
        });
        batchIssues += duplicateOptions.length;
      } else {
        console.log('  ✅ All questions have unique option IDs');
      }
      
      // ============ BATCH SUMMARY ============
      console.log('\n' + '─'.repeat(70));
      if (batchIssues === 0) {
        console.log(`✅ BATCH ${batchId}: PERFECT - No issues found!`);
      } else {
        console.log(`❌ BATCH ${batchId}: ${batchIssues} TOTAL ISSUES FOUND`);
      }
      
      totalIssues += batchIssues;
    }
    
    // ============ FINAL SUMMARY ============
    console.log('\n' + '='.repeat(70));
    console.log('\n📊 FINAL AUDIT SUMMARY\n');
    
    if (totalIssues === 0) {
      console.log('🎉 PERFECT! All batches are 100% valid!');
      console.log('✅ All 220 OIR questions are ready to use.');
    } else {
      console.log(`❌ TOTAL ISSUES FOUND: ${totalIssues}`);
      console.log('⚠️  These issues need to be fixed before the test can work correctly.');
    }
    
    console.log('\n' + '='.repeat(70));
    
  } catch (error) {
    console.error('❌ Audit failed:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

comprehensiveAudit();
