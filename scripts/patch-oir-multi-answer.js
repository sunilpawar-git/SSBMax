#!/usr/bin/env node
/**
 * Phase 0: Patch multi-answer OIR figure questions in Firestore.
 *
 * Problem: ~160 questions in batches batch_pdf_001..batch_pdf_020 ask
 * "Which two of the following figures belong to Class A?" but were stored
 * with correctAnswerId="" and the answer pair only in the explanation text
 * (e.g. "Answer: 2 and 3."). Every user answer therefore evaluates to
 * "opt_x" == "" → false, so these questions are always scored wrong.
 *
 * Fix: Parse the explanation, derive correctAnswerIds: ["opt_b","opt_c"],
 * and write it back alongside the existing correctAnswerId="" field.
 * The app will read correctAnswerIds in Phase 4+ and score correctly.
 *
 * Scope: batches batch_pdf_001..batch_pdf_020 only.
 *        Part-3 batches (021-028) are all single-answer — not touched.
 *
 * Usage:
 *   node scripts/patch-oir-multi-answer.js               # dry-run (default)
 *   node scripts/patch-oir-multi-answer.js --commit      # write to Firestore
 *
 * After a verified --commit run, bump contentVersion so all clients
 * re-download the corrected data:
 *   node scripts/set-oir-meta-config.js --version 3 --batches 28 --commit
 */

'use strict';

const fs   = require('fs');
const path = require('path');

// ── CLI flags ──────────────────────────────────────────────────────────────
const argv  = process.argv.slice(2);
const COMMIT = argv.includes('--commit');

// ── Constants ──────────────────────────────────────────────────────────────
const FIRST_AFFECTED_BATCH = 1;
const LAST_AFFECTED_BATCH  = 20;   // 021-028 are Part-3, all single-answer

/** Maps explanation digit ("1".."5") to option ID ("opt_a".."opt_e"). */
const DIGIT_TO_OPT = { '1': 'opt_a', '2': 'opt_b', '3': 'opt_c', '4': 'opt_d', '5': 'opt_e' };

/** Regex that matches "Answer: 2 and 3" (case-insensitive, optional trailing dot). */
const ANSWER_RE = /answer[:\s]+(\d)\s+and\s+(\d)/i;

// ── Helpers ────────────────────────────────────────────────────────────────

function batchId(i) {
  return `batch_pdf_${String(i).padStart(3, '0')}`;
}

/**
 * Returns { ids: ["opt_b","opt_c"] } on success,
 *         { error: "<reason>" }       on failure.
 * Also validates that both IDs exist in the question's options array.
 */
function parseAnswerIds(question) {
  const explanation = question.explanation || '';
  const match = explanation.match(ANSWER_RE);
  if (!match) {
    return { error: `no "Answer: X and Y" pattern in explanation: "${explanation.slice(0, 80)}"` };
  }

  const [, d1, d2] = match;
  const id1 = DIGIT_TO_OPT[d1];
  const id2 = DIGIT_TO_OPT[d2];

  if (!id1) return { error: `digit "${d1}" out of range 1-5` };
  if (!id2) return { error: `digit "${d2}" out of range 1-5` };
  if (id1 === id2) return { error: `both digits are the same (${d1})` };

  const optionIds = new Set((question.options || []).map(o => o.id));
  if (!optionIds.has(id1)) return { error: `parsed id "${id1}" not in options [${[...optionIds].join(', ')}]` };
  if (!optionIds.has(id2)) return { error: `parsed id "${id2}" not in options [${[...optionIds].join(', ')}]` };

  return { ids: [id1, id2] };
}

// ── Main ───────────────────────────────────────────────────────────────────

async function main() {
  console.log('╔══════════════════════════════════════════════════════════╗');
  console.log('║  patch-oir-multi-answer.js                               ║');
  console.log(`║  Mode: ${COMMIT ? 'COMMIT (writes to Firestore)          ' : 'DRY RUN (no writes)              '}   ║`);
  console.log('╚══════════════════════════════════════════════════════════╝\n');

  // ── Firebase init ────────────────────────────────────────────────────────
  const admin = require('firebase-admin');
  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT ||
    path.join(process.env.HOME, 'Downloads/SSBMax/firebase-admin-key.json');

  if (!fs.existsSync(serviceAccountPath)) {
    console.error(`❌ Service account not found: ${serviceAccountPath}`);
    console.error('   Set FIREBASE_SERVICE_ACCOUNT=/path/to/key.json to override.');
    process.exit(1);
  }

  admin.initializeApp({ credential: admin.credential.cert(require(serviceAccountPath)) });
  const db = admin.firestore();

  // ── Process batches ───────────────────────────────────────────────────────
  let totalPatched    = 0;
  let totalUnresolved = 0;
  let totalSkipped    = 0;   // already have correctAnswerIds
  const unresolvedLog = [];

  for (let i = FIRST_AFFECTED_BATCH; i <= LAST_AFFECTED_BATCH; i++) {
    const id  = batchId(i);
    const ref = db.collection('test_content').doc('oir').collection('batches').doc(id);

    let doc;
    try {
      doc = await ref.get();
    } catch (e) {
      console.error(`  ❌ Failed to fetch ${id}: ${e.message}`);
      continue;
    }

    if (!doc.exists) {
      console.log(`  ⚠️  ${id}: not found in Firestore — skipping`);
      continue;
    }

    const data      = doc.data();
    const questions = data.questions || [];
    let batchPatched    = 0;
    let batchUnresolved = 0;
    let batchSkipped    = 0;

    const updatedQuestions = questions.map(q => {
      // Candidate: empty correctAnswerId and has options (figure question)
      const isCandidate = (q.correctAnswerId === '' || q.correctAnswerId == null)
        && Array.isArray(q.options) && q.options.length > 0;

      if (!isCandidate) return q;

      // Already patched by a previous run — idempotent
      if (Array.isArray(q.correctAnswerIds) && q.correctAnswerIds.length > 0) {
        batchSkipped++;
        return q;
      }

      const result = parseAnswerIds(q);
      if (result.error) {
        batchUnresolved++;
        unresolvedLog.push({ batchId: id, questionId: q.id, reason: result.error });
        return q;   // don't corrupt — leave as-is
      }

      batchPatched++;
      return { ...q, correctAnswerIds: result.ids };
    });

    console.log(`  ${id}: ${batchPatched} patched, ${batchUnresolved} unresolved, ${batchSkipped} already done`
      + ` (of ${questions.length} total)`);

    totalPatched    += batchPatched;
    totalUnresolved += batchUnresolved;
    totalSkipped    += batchSkipped;

    if (COMMIT && batchPatched > 0) {
      await ref.update({ questions: updatedQuestions });
    }
  }

  // ── Summary ───────────────────────────────────────────────────────────────
  console.log('\n──────────────────────────────────────────────────────────');
  console.log(`Total patched:    ${totalPatched}`);
  console.log(`Total unresolved: ${totalUnresolved}`);
  console.log(`Total skipped (already had correctAnswerIds): ${totalSkipped}`);

  if (unresolvedLog.length > 0) {
    console.log('\n🔴 UNRESOLVED questions (not written — need manual fix):');
    unresolvedLog.forEach(e =>
      console.log(`   ${e.batchId} / ${e.questionId}: ${e.reason}`)
    );
  }

  if (!COMMIT) {
    console.log('\n🧪 DRY RUN complete. Re-run with --commit to write to Firestore.');
    console.log('   Expect: ~160 patched and 0 unresolved before committing.');
  } else {
    console.log('\n✅ Firestore updated.');
    console.log('   Next step — bump contentVersion so all clients re-download:');
    console.log('   node scripts/set-oir-meta-config.js --version 3 --batches 28 --commit');
  }
}

main()
  .then(() => process.exit(0))
  .catch(e => { console.error('💥', e); process.exit(1); });
