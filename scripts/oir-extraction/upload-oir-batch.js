/**
 * Upload one deterministic OIR batch (figures -> Storage, questions -> Firestore).
 *
 * Usage:
 *   node upload-oir-batch.js batch_pdf_001            # live upload
 *   node upload-oir-batch.js batch_pdf_001 --dry-run  # offline validation only
 *   node upload-oir-batch.js batch_pdf_001 --verify   # HEAD-check all questionImageUrls in Firestore
 *   node upload-oir-batch.js batch_pdf_001 --repair   # re-upload missing images + patch gs:// placeholders
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
const dryRun   = process.argv.includes('--dry-run');
const verify   = process.argv.includes('--verify');
const repair   = process.argv.includes('--repair');

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

  // ---- shared Firebase initialisation (used by live upload, verify, and repair)
  const admin = require('firebase-admin');
  const { v4: uuidv4 } = require('uuid');
  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT ||
    path.join(process.env.HOME, 'Downloads/SSBMax/firebase-admin-key.json');
  if (!fs.existsSync(serviceAccountPath)) {
    console.error(`❌ Service account not found at ${serviceAccountPath}`);
    console.error('   Set FIREBASE_SERVICE_ACCOUNT=/path/to/key.json');
    process.exit(1);
  }
  admin.initializeApp({
    credential: admin.credential.cert(require(serviceAccountPath)),
    storageBucket: BUCKET,
  });
  const bucket = admin.storage().bucket(BUCKET);
  const db = admin.firestore();
  const docRef = db.collection('test_content').doc('oir').collection('batches').doc(batchId);

  // ---- --verify: HEAD-check every questionImageUrl already written to Firestore
  if (verify) {
    console.log(`🔍 Verifying image URLs for ${batchId}...`);
    const snap = await docRef.get();
    if (!snap.exists) {
      console.error(`❌ Firestore doc not found: test_content/oir/batches/${batchId}`);
      process.exit(1);
    }
    const https = require('https');
    const questions = snap.data().questions || [];
    const broken = [];
    for (const q of questions) {
      if (!q.questionImageUrl) continue;
      const ok = await new Promise((resolve) => {
        const req = https.request(q.questionImageUrl, { method: 'HEAD' }, (res) => resolve(res.statusCode === 200));
        req.on('error', () => resolve(false));
        req.end();
      });
      if (!ok) broken.push({ id: q.id, url: q.questionImageUrl });
    }
    if (broken.length > 0) {
      console.error(`❌ VERIFY FAILED — ${broken.length} broken image URL(s):`);
      broken.forEach((b) => console.error(`   ${b.id}: ${b.url}`));
      console.error('   Run with --repair to re-upload missing images and patch Firestore.');
      process.exit(1);
    }
    console.log(`✅ VERIFY OK — all ${questions.filter((q) => q.questionImageUrl).length} image URLs resolve.`);
    return;
  }

  // ---- --repair: re-upload only the images whose Storage objects are missing, then patch Firestore
  if (repair) {
    console.log(`🔧 Repair mode — re-uploading missing images for ${batchId}...`);
    const snap = await docRef.get();
    if (!snap.exists) {
      console.error(`❌ Firestore doc not found — run a full upload first.`);
      process.exit(1);
    }
    const storedQuestions = snap.data().questions || [];
    let repaired = 0;
    for (const { file, local } of figures) {
      const destination = `${STORAGE_DIR}/${file}`;
      const [exists] = await bucket.file(destination).exists();
      if (!exists) {
        await bucket.upload(local, {
          destination,
          metadata: { contentType: 'image/png', metadata: { firebaseStorageDownloadTokens: uuidv4() } },
        });
        await bucket.file(destination).makePublic();
        console.log(`   ✅ Re-uploaded: ${file}`);
        repaired++;
      }
    }
    if (repaired === 0) {
      console.log('   ℹ️  All images already present in Storage — no re-uploads needed.');
    }
    // Patch any placeholder gs:// URLs still in the stored questions
    let patched = false;
    for (const q of storedQuestions) {
      if (q.questionImageUrl && q.questionImageUrl.startsWith('gs://')) {
        q.questionImageUrl = publicUrl(q.questionImageUrl.split('/').pop());
        patched = true;
      }
    }
    if (patched) {
      await docRef.update({ questions: storedQuestions });
      console.log('   ✅ Patched gs:// placeholder URLs in Firestore.');
    }
    console.log(`✅ Repair complete (${repaired} image(s) re-uploaded).`);
    return;
  }

  // ---- live upload
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

  await docRef.set({
    batchId: batch.batchId,
    version: batch.version,
    source: batch.source,
    totalQuestions: batch.totalQuestions,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    questions: batch.questions,
  });
  console.log(`✅ Firestore: test_content/oir/batches/${batchId} (${batch.totalQuestions} questions)`);
  console.log(`   Run with --verify to confirm all image URLs resolve.`);
}

main().then(() => process.exit(0)).catch((e) => { console.error('💥', e); process.exit(1); });
