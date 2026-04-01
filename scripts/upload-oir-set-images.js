/**
 * Upload OIR question images (WebP) to Firebase Storage.
 *
 * Works for any set number (1–20).
 * Reads WebP files from scripts/oir_images/set_NN/ named q01.webp, q03.webp, etc.
 * Uploads to Firebase Storage path: oir_question_images/set_NN/qXX.webp
 * Outputs download URL map to: scripts/image-url-map-set-NNN.json
 *
 * Usage:
 *   node scripts/upload-oir-set-images.js          # defaults to set 1
 *   node scripts/upload-oir-set-images.js --set 2  # set 2
 *   node scripts/upload-oir-set-images.js --set 15
 */

'use strict';

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// --- Parse --set N argument (default: 1) ---
const setArgIdx = process.argv.indexOf('--set');
const setNumber = setArgIdx !== -1 ? parseInt(process.argv[setArgIdx + 1], 10) : 1;
if (isNaN(setNumber) || setNumber < 1 || setNumber > 20) {
  console.error('❌ Invalid --set value. Must be 1–20.');
  process.exit(1);
}

const SET_PAD2 = String(setNumber).padStart(2, '0');   // "01", "02", …
const SET_PAD3 = String(setNumber).padStart(3, '0');   // "001", "002", …

const IMAGES_DIR = path.join(__dirname, 'oir_images', `set_${SET_PAD2}`);
const STORAGE_PREFIX = `oir_question_images/set_${SET_PAD2}`;
const URL_MAP_OUTPUT = path.join(__dirname, `image-url-map-set-${SET_PAD3}.json`);

// --- Firebase init ---
const serviceAccountCandidates = [
  path.join(__dirname, '..', '.firebase', 'service-account.json'),
  path.join(__dirname, '..', 'serviceAccountKey.json'),
];

let initialized = false;
for (const candidate of serviceAccountCandidates) {
  if (fs.existsSync(candidate)) {
    const serviceAccount = require(candidate);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      storageBucket: 'ssbmax-49e68.firebasestorage.app',
    });
    console.log(`✅ Firebase initialized (service account: ${path.basename(candidate)})`);
    initialized = true;
    break;
  }
}
if (!initialized) {
  console.error('❌ No service account file found. Tried:');
  serviceAccountCandidates.forEach(p => console.error(`   ${p}`));
  process.exit(1);
}

const bucket = admin.storage().bucket();

// --- Helpers ---
function generateToken() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

function downloadUrl(bucketName, storagePath, token) {
  const encoded = encodeURIComponent(storagePath);
  return `https://firebasestorage.googleapis.com/v0/b/${bucketName}/o/${encoded}?alt=media&token=${token}`;
}

async function uploadImage(localFile) {
  const filename = path.basename(localFile); // e.g. q01.webp
  const storagePath = `${STORAGE_PREFIX}/${filename}`;
  const token = generateToken();

  await bucket.upload(localFile, {
    destination: storagePath,
    metadata: {
      contentType: 'image/webp',
      metadata: { firebaseStorageDownloadTokens: token },
    },
  });

  return {
    filename,
    storagePath,
    downloadUrl: downloadUrl(bucket.name, storagePath, token),
  };
}

// --- Main ---
async function main() {
  console.log(`📤 OIR Set ${setNumber} Image Upload`);
  console.log('===============================');

  if (!fs.existsSync(IMAGES_DIR)) {
    console.error(`❌ Images directory not found: ${IMAGES_DIR}`);
    console.error('   Run auto-crop-oir-images.py first.');
    process.exit(1);
  }

  const files = fs.readdirSync(IMAGES_DIR)
    .filter(f => /^q\d{2}\.webp$/i.test(f))
    .sort();

  if (files.length === 0) {
    console.error('❌ No WebP files found (expected names like q01.webp, q03.webp).');
    process.exit(1);
  }

  console.log(`📊 Found ${files.length} image(s) to upload`);
  console.log();

  const urlMap = {};
  let uploaded = 0;
  let failed = 0;

  for (const filename of files) {
    const localPath = path.join(IMAGES_DIR, filename);
    // Derive question ID: q01.webp → oir_s01_q01
    const qNum = filename.replace(/^q/, '').replace(/\.webp$/i, ''); // e.g. "01"
    const questionId = `oir_s${SET_PAD2}_q${qNum}`;

    process.stdout.write(`⬆️  ${filename} → ${STORAGE_PREFIX}/${filename} ... `);
    try {
      const result = await uploadImage(localPath);
      urlMap[questionId] = result.downloadUrl;
      console.log('✅');
      uploaded++;
    } catch (err) {
      console.log(`❌ ${err.message}`);
      failed++;
    }
  }

  console.log();
  console.log(`✅ Uploaded: ${uploaded}  ❌ Failed: ${failed}`);

  if (Object.keys(urlMap).length > 0) {
    fs.writeFileSync(URL_MAP_OUTPUT, JSON.stringify(urlMap, null, 2));
    console.log(`\n📝 URL map written to: ${URL_MAP_OUTPUT}`);
    console.log('\n📝 Next step: node scripts/upload-oir-sets.js');
    console.log('   (It will automatically merge image URLs from the map above)');
  }

  process.exit(failed > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('\n❌ Unexpected error:', err);
  process.exit(1);
});
