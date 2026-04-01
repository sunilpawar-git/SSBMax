/**
 * Upload OIR practice sets to Firestore.
 *
 * Reads oir-set-NNN.json (and optionally image-url-map-set-NNN.json),
 * merges image URLs into questions, then writes to:
 *   test_content/oir/test_sets/set_NNN
 *
 * Usage:
 *   node scripts/upload-oir-sets.js                 # upload all discovered sets
 *   node scripts/upload-oir-sets.js --set 2         # upload only Set 2
 *   node scripts/upload-oir-sets.js --set 2 --dry-run
 *
 * --set N    Upload only this set number (1–20)
 * --dry-run  Print what would be uploaded without writing to Firestore.
 */

'use strict';

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const DRY_RUN = process.argv.includes('--dry-run');

// Optional --set N argument
const setArgIdx = process.argv.indexOf('--set');
const setFilter = setArgIdx !== -1 ? parseInt(process.argv[setArgIdx + 1], 10) : null;
if (setArgIdx !== -1 && (isNaN(setFilter) || setFilter < 1 || setFilter > 20)) {
  console.error('❌ Invalid --set value. Must be 1–20.');
  process.exit(1);
}

// --- Firebase init ---
const serviceAccountCandidates = [
  path.join(__dirname, '..', '.firebase', 'service-account.json'),
  path.join(__dirname, '..', 'serviceAccountKey.json'),
];

if (!DRY_RUN) {
  let initialized = false;
  for (const candidate of serviceAccountCandidates) {
    if (fs.existsSync(candidate)) {
      const serviceAccount = require(candidate);
      admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
      console.log(`✅ Firebase initialized (${path.basename(candidate)})`);
      initialized = true;
      break;
    }
  }
  if (!initialized) {
    console.error('❌ No service account file found. Tried:');
    serviceAccountCandidates.forEach(p => console.error(`   ${p}`));
    process.exit(1);
  }
}

const db = DRY_RUN ? null : admin.firestore();

// --- Helpers ---
function loadJson(filePath) {
  if (!fs.existsSync(filePath)) return null;
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function padSet(n) {
  return String(n).padStart(3, '0');
}

async function uploadSet(setData, imageUrlMap) {
  // Merge image URLs into questions if map is available
  const questions = setData.questions.map(q => {
    const imageUrl = imageUrlMap ? (imageUrlMap[q.id] || null) : q.questionImageUrl || null;
    return { ...q, questionImageUrl: imageUrl };
  });

  const doc = {
    setNumber: setData.setNumber,
    title: setData.title,
    version: setData.version,
    totalQuestions: setData.totalQuestions,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    questions,
  };

  const docId = `set_${padSet(setData.setNumber)}`;
  const ref = db.collection('test_content').doc('oir').collection('test_sets').doc(docId);

  await ref.set(doc);
  return { docId, questionCount: questions.length };
}

// --- Main ---
async function main() {
  console.log(`📚 OIR Set Upload${DRY_RUN ? ' (DRY RUN)' : ''}`);
  console.log('==================');

  // Build the list of set files to process
  let setFiles;
  if (setFilter !== null) {
    // Single-set mode (used by process-oir-set.py --phase upload)
    const pad3 = String(setFilter).padStart(3, '0');
    setFiles = [{
      json: path.join(__dirname, `oir-set-${pad3}.json`),
      urlMap: path.join(__dirname, `image-url-map-set-${pad3}.json`),
    }];
  } else {
    // Auto-discover all oir-set-NNN.json files present in the scripts dir
    setFiles = fs.readdirSync(__dirname)
      .filter(f => /^oir-set-\d{3}\.json$/.test(f))
      .sort()
      .map(f => {
        const num = f.match(/(\d{3})/)[1];
        return {
          json: path.join(__dirname, f),
          urlMap: path.join(__dirname, `image-url-map-set-${num}.json`),
        };
      });
    if (setFiles.length === 0) {
      console.error('❌ No oir-set-NNN.json files found in scripts/.');
      process.exit(1);
    }
  }

  let totalUploaded = 0;
  let totalFailed = 0;

  for (const { json: jsonPath, urlMap: urlMapPath } of setFiles) {
    const setData = loadJson(jsonPath);
    if (!setData) {
      console.warn(`⚠️  Skipping — file not found: ${jsonPath}`);
      continue;
    }

    const imageUrlMap = loadJson(urlMapPath);
    if (imageUrlMap) {
      const urlCount = Object.keys(imageUrlMap).length;
      console.log(`🖼️  Image URL map loaded: ${urlCount} entries (${path.basename(urlMapPath)})`);
    } else {
      console.log(`ℹ️  No image URL map found at ${path.basename(urlMapPath)} — questionImageUrl will be null`);
    }

    const docId = `set_${padSet(setData.setNumber)}`;
    const imageQs = setData.questions.filter(q =>
      imageUrlMap ? !!imageUrlMap[q.id] : !!q.questionImageUrl
    );

    console.log();
    console.log(`📦 Set ${setData.setNumber}: "${setData.title}"`);
    console.log(`   Questions : ${setData.totalQuestions}`);
    console.log(`   With image: ${imageQs.length}`);
    console.log(`   Firestore : test_content/oir/test_sets/${docId}`);

    if (DRY_RUN) {
      console.log('   ✅ [DRY RUN] Would upload above document.');

      // Show per-type breakdown
      const byType = {};
      setData.questions.forEach(q => { byType[q.type] = (byType[q.type] || 0) + 1; });
      console.log('   Type breakdown:', byType);

      // Show first 3 question IDs as a sanity check
      const sample = setData.questions.slice(0, 3).map(q => q.id);
      console.log(`   Sample IDs: ${sample.join(', ')} ...`);
      continue;
    }

    try {
      const { questionCount } = await uploadSet(setData, imageUrlMap);
      console.log(`   ✅ Uploaded ${questionCount} questions`);

      // Verify
      const docId2 = `set_${padSet(setData.setNumber)}`;
      const snap = await db.collection('test_content').doc('oir').collection('test_sets').doc(docId2).get();
      if (snap.exists) {
        const data = snap.data();
        console.log(`   ✅ Verified: ${data.questions.length} questions in Firestore`);
      } else {
        console.error('   ❌ Verification failed: document not found after upload');
      }

      totalUploaded++;
    } catch (err) {
      console.error(`   ❌ Failed: ${err.message}`);
      totalFailed++;
    }
  }

  console.log();
  console.log(`🏁 Done — ${totalUploaded} set(s) uploaded, ${totalFailed} failed.`);

  if (!DRY_RUN && totalFailed === 0) {
    console.log();
    console.log('📝 Next steps:');
    console.log('   1. Launch app, navigate to OIR → Practice Sets');
    console.log('   2. Verify Set 1 shows "Start" and Sets 2-20 show "Locked"');
    console.log('   3. Start Set 1 and confirm all 50 questions load correctly');
  }

  process.exit(totalFailed > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('\n❌ Unexpected error:', err);
  process.exit(1);
});
