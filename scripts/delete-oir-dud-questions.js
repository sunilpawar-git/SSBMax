/**
 * ⚠️⚠️⚠️ DO NOT RUN — SUPERSEDED / UNUSED AS OF 2026-06-07 ⚠️⚠️⚠️
 *
 * TODO(future-sprint): This deletion approach was ABANDONED. The 140 "duds" are
 * genuine free-response (fill-in-the-blank) questions, NOT broken MCQs — deleting
 * them throws away valid SSB practice content. The decided direction is to RESCUE
 * them via a free-response OIR question type (plan: ~/.claude/plans/staged-splashing-sifakis.md),
 * NOT delete them. Until that lands, duds are skipped at selection-time by
 * OIRQuestionSelector's validity filter (they never reach an assembled test).
 * Kept only for reference. If you're about to run this, STOP and read the plan first.
 *
 * ---------------------------------------------------------------------------
 * Remove the structurally-invalid "dud" questions from the original PDF batches
 * test_content/oir/batches/batch_pdf_001 .. batch_pdf_020.
 *
 * A dud = a question with NO options (empty/absent) AND NO questionImageUrl.
 * These are the ~123 VERBAL fill-in-the-blank entries that were never multiple
 * choice; the runtime OIRQuestionValidator drops them at test-assembly, which is
 * what silently collapsed a 50-question test down to ~43. We delete them at the
 * source. The "AND no questionImageUrl" guard deliberately PRESERVES legitimate
 * multi-answer figure questions (figure present, single correctAnswerId absent).
 *
 * Updates both `questions` and `totalQuestions` on each batch doc. Idempotent —
 * re-running finds 0 duds once cleaned.
 *
 * Usage:
 *   node delete-oir-dud-questions.js              # DRY RUN (default) — prints per-batch counts
 *   node delete-oir-dud-questions.js --commit     # actually write the cleaned docs
 *
 * Live writes require the service account (same as upload-oir-batch.js):
 *   ~/Downloads/SSBMax/firebase-admin-key.json  (or FIREBASE_SERVICE_ACCOUNT=/path)
 */

const fs = require('fs');
const path = require('path');

const commit = process.argv.includes('--commit');
const FIRST = 1;
const LAST = 20;

function isDud(q) {
  const noOptions = !Array.isArray(q.options) || q.options.length === 0;
  const noImage = !q.questionImageUrl;
  return noOptions && noImage;
}

async function main() {
  const admin = require('firebase-admin');
  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT ||
    path.join(process.env.HOME, 'Downloads/SSBMax/firebase-admin-key.json');
  if (!fs.existsSync(serviceAccountPath)) {
    console.error(`❌ Service account not found at ${serviceAccountPath}`);
    console.error('   Set FIREBASE_SERVICE_ACCOUNT=/path/to/key.json');
    process.exit(1);
  }
  admin.initializeApp({ credential: admin.credential.cert(require(serviceAccountPath)) });
  const db = admin.firestore();
  const batches = db.collection('test_content').doc('oir').collection('batches');

  console.log(commit
    ? '✍️  COMMIT MODE — cleaned batch docs WILL be written.'
    : '🧪 DRY RUN (default) — no writes. Re-run with --commit to apply.');

  let totalDuds = 0;
  let totalBefore = 0;
  let totalAfter = 0;
  const perBatch = [];

  for (let n = FIRST; n <= LAST; n++) {
    const batchId = `batch_pdf_${String(n).padStart(3, '0')}`;
    const ref = batches.doc(batchId);
    const snap = await ref.get();
    if (!snap.exists) {
      perBatch.push(`${batchId}: (missing doc — skipped)`);
      continue;
    }
    const data = snap.data();
    const questions = Array.isArray(data.questions) ? data.questions : [];
    const kept = questions.filter((q) => !isDud(q));
    const removed = questions.length - kept.length;

    totalDuds += removed;
    totalBefore += questions.length;
    totalAfter += kept.length;
    perBatch.push(`${batchId}: ${questions.length} -> ${kept.length}  (removed ${removed})`);

    if (commit && removed > 0) {
      await ref.update({ questions: kept, totalQuestions: kept.length });
    }
  }

  console.log('\nPer-batch (before -> after):');
  perBatch.forEach((l) => console.log('  ' + l));
  console.log(`\nTotals: ${totalBefore} questions, ${totalDuds} duds, ${totalAfter} remaining.`);
  if (!commit && totalDuds > 0) {
    console.log('Review the counts above, then re-run with --commit to delete.');
  } else if (commit) {
    console.log(`✅ Cleanup complete — removed ${totalDuds} dud question(s) across ${LAST - FIRST + 1} batches.`);
  } else {
    console.log('✅ Nothing to remove — batches are already clean (idempotent).');
  }
}

main().then(() => process.exit(0)).catch((e) => { console.error('💥', e); process.exit(1); });
