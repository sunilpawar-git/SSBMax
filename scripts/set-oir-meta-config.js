/**
 * Publish the OIR content-version meta doc that drives client reconciliation.
 *
 * Writes `test_content/oir/meta/config` = { contentVersion, batchCount }.
 * OIRQuestionCacheManager.initialSync() reads this: when contentVersion differs
 * from a device's locally-stored version it clears and re-downloads all
 * `batch_pdf_001..{batchCount}`, so every install (existing ones included)
 * self-heals to the current content. Bump contentVersion whenever the served
 * bank changes; update batchCount when batches are added/removed.
 *
 * Usage:
 *   node set-oir-meta-config.js                       # DRY RUN (default)
 *   node set-oir-meta-config.js --commit              # write the meta doc
 *   node set-oir-meta-config.js --version 2 --batches 28 [--commit]
 *
 * Defaults: contentVersion=2, batchCount=28 (20 original + 8 Part-3 batches).
 * Live write requires the service account (~/Downloads/SSBMax/firebase-admin-key.json
 * or FIREBASE_SERVICE_ACCOUNT=/path).
 */

const fs = require('fs');
const path = require('path');

const argv = process.argv.slice(2);
const commit = argv.includes('--commit');
const flag = (name, def) => {
  const i = argv.indexOf(name);
  return i >= 0 && argv[i + 1] ? Number(argv[i + 1]) : def;
};
const contentVersion = flag('--version', 2);
const batchCount = flag('--batches', 28);

async function main() {
  const admin = require('firebase-admin');
  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT ||
    path.join(process.env.HOME, 'Downloads/SSBMax/firebase-admin-key.json');
  if (!fs.existsSync(serviceAccountPath)) {
    console.error(`❌ Service account not found at ${serviceAccountPath}`);
    process.exit(1);
  }
  admin.initializeApp({ credential: admin.credential.cert(require(serviceAccountPath)) });
  const db = admin.firestore();
  const ref = db.collection('test_content').doc('oir').collection('meta').doc('config');

  const current = await ref.get();
  console.log(`Current meta: ${current.exists ? JSON.stringify(current.data()) : '(none)'}`);
  console.log(`Target  meta: { contentVersion: ${contentVersion}, batchCount: ${batchCount} }`);

  if (!commit) {
    console.log('🧪 DRY RUN (default) — no write. Re-run with --commit to publish.');
    console.log('   Effect on clients: every device whose stored contentVersion != ' +
      `${contentVersion} will clear its OIR cache and re-download batch_pdf_001..${String(batchCount).padStart(3, '0')}.`);
    return;
  }

  // merge:true — a stale meta doc may already hold legacy fields (distribution,
  // difficulty_levels, etc.) written by older scripts. We only own contentVersion /
  // batchCount; preserve everything else rather than overwrite the doc.
  await ref.set({
    contentVersion,
    batchCount,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  console.log(`✅ Wrote test_content/oir/meta/config = { contentVersion: ${contentVersion}, batchCount: ${batchCount} }`);
}

main().then(() => process.exit(0)).catch((e) => { console.error('💥', e); process.exit(1); });
