/**
 * Upload one deterministic OIR batch (figures -> Storage, questions -> Firestore).
 *
 * Usage:
 *   node upload-oir-batch.js batch_pdf_001            # live upload
 *   node upload-oir-batch.js batch_pdf_001 --dry-run  # offline validation only
 *
 * Live upload requires (same as the other scripts in ../):
 *   - npm install  (in SSBMax/scripts/ — firebase-admin, uuid)
 *   - service account at SSBMax/.firebase/service-account.json
 *
 * Reuses conventions from ../upload-gpe-images.js (Storage) and
 * ../upload-oir-batch-002.js (Firestore test_content/oir/batches/{batchId}).
 */

const fs = require('fs');
const path = require('path');

const BUCKET = 'ssbmax-49e68.firebasestorage.app';
const STORAGE_DIR = 'oir/pdf_questions';
const OUT_DIR = path.join(__dirname, 'out');
const IMG_DIR = path.join(OUT_DIR, 'images');

const batchId = process.argv[2];
const dryRun = process.argv.includes('--dry-run');

if (!batchId) {
  console.error('❌ Usage: node upload-oir-batch.js <batchId> [--dry-run]');
  process.exit(1);
}

const batchPath = path.join(OUT_DIR, `${batchId}.json`);
if (!fs.existsSync(batchPath)) {
  console.error(`❌ Batch JSON not found: ${batchPath}`);
  console.error('   Generate it first: python3 oir_extract_v2.py --set N');
  process.exit(1);
}

const batch = JSON.parse(fs.readFileSync(batchPath, 'utf8'));
const publicUrl = (file) => `https://storage.googleapis.com/${BUCKET}/${STORAGE_DIR}/${file}`;

// Collect the figure files referenced by this batch and validate they exist locally.
const figures = [];
let missing = [];
for (const q of batch.questions) {
  if (!q.questionImageUrl) continue;
  const file = q.questionImageUrl.split('/').pop();
  const local = path.join(IMG_DIR, file);
  if (!fs.existsSync(local)) missing.push(file);
  else figures.push({ file, local });
}

console.log(`📦 ${batchId}: ${batch.questions.length} questions, ${figures.length} figures`);
if (missing.length) {
  console.error(`❌ Missing local image files (re-run the extractor): ${missing.join(', ')}`);
  process.exit(1);
}

async function main() {
  if (dryRun) {
    console.log('🧪 DRY RUN — no uploads, no Firestore writes.');
    console.log(`   Would upload ${figures.length} figures to gs://${BUCKET}/${STORAGE_DIR}/`);
    if (figures[0]) console.log(`   Sample public URL: ${publicUrl(figures[0].file)}`);
    console.log(`   Would write Firestore doc: test_content/oir/batches/${batchId}`);
    const types = batch.questions.reduce((a, q) => ((a[q.type] = (a[q.type] || 0) + 1), a), {});
    console.log('   Question types:', types);
    console.log('✅ Dry run OK.');
    return;
  }

  // ---- live upload
  const admin = require('firebase-admin');
  const { v4: uuidv4 } = require('uuid');
  // Service account path from env var (kept outside repo to avoid credentials in git)
  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT ||
    path.join(process.env.HOME, 'Downloads/SSBMax/firebase-admin-key.json');
  if (!fs.existsSync(serviceAccountPath)) {
    console.error(`❌ Service account not found at ${serviceAccountPath}`);
    console.error('   Set FIREBASE_SERVICE_ACCOUNT=/path/to/key.json or ensure ~/Downloads/SSBMax/firebase-admin-key.json exists');
    process.exit(1);
  }
  admin.initializeApp({
    credential: admin.credential.cert(require(serviceAccountPath)),
    storageBucket: BUCKET,
  });
  const bucket = admin.storage().bucket(BUCKET);
  const db = admin.firestore();

  console.log(`📤 Uploading ${figures.length} figures...`);
  for (const { file, local } of figures) {
    const destination = `${STORAGE_DIR}/${file}`;
    await bucket.upload(local, {
      destination,
      metadata: {
        contentType: 'image/png',
        metadata: { firebaseStorageDownloadTokens: uuidv4() },
      },
    });
    await bucket.file(destination).makePublic();
  }
  console.log('   ✅ Figures uploaded & made public.');

  // Rewrite questionImageUrl placeholders to public URLs.
  for (const q of batch.questions) {
    if (q.questionImageUrl) q.questionImageUrl = publicUrl(q.questionImageUrl.split('/').pop());
  }

  await db.collection('test_content').doc('oir').collection('batches').doc(batchId).set({
    batchId: batch.batchId,
    version: batch.version,
    source: batch.source,
    totalQuestions: batch.totalQuestions,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    questions: batch.questions,
  });
  console.log(`✅ Firestore: test_content/oir/batches/${batchId} (${batch.totalQuestions} questions)`);
}

main().then(() => process.exit(0)).catch((e) => { console.error('💥', e); process.exit(1); });
