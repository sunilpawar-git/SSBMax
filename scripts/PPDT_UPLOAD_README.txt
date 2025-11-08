╔══════════════════════════════════════════════════════════════╗
║           PPDT IMAGE UPLOAD - COMPLETE GUIDE                 ║
╚══════════════════════════════════════════════════════════════╝

✅ STEP 1: UPLOAD JSON METADATA (COMPLETED)
═════════════════════════════════════════════
Status: DONE ✅
Location: Firestore → test_content/ppdt/image_batches/batch_001
Images: 30 placeholder entries created


📸 STEP 2: UPLOAD ACTUAL PPDT IMAGES
═════════════════════════════════════

PREPARATION:
------------
1. Collect 30 PPDT images (hazy pictures for PPDT test)
2. Rename them to: ppdt_001.jpg, ppdt_002.jpg, ..., ppdt_030.jpg
3. Place them in: /Users/sunil/Downloads/ppdt_images/

OPTION A: Automated Upload (Recommended)
-----------------------------------------
Run the provided script:

    cd /Users/sunil/Downloads/SSBMax/scripts
    ./upload_ppdt_images.sh

The script will:
- Check if images exist
- Verify Firebase CLI is logged in
- Upload all images to Storage: ppdt_images/batch_001/
- Show upload progress and summary

OPTION B: Manual Upload via Firebase Console
---------------------------------------------
1. Go to: https://console.firebase.google.com
2. Select your SSBMax project
3. Click "Storage" in left sidebar
4. Click "Upload file" or "Create folder"
5. Create folder: ppdt_images/batch_001/
6. Upload all 30 images to this folder
7. Verify images are named correctly


🔒 STEP 3: UPDATE STORAGE RULES (REQUIRED)
═══════════════════════════════════════════

Go to Firebase Console → Storage → Rules tab

Replace with:

    rules_version = '2';
    service firebase.storage {
      match /b/{bucket}/o {
        // Allow public read for PPDT images
        match /ppdt_images/{allPaths=**} {
          allow read: if true;
          allow write: if request.auth != null;
        }
        
        // Keep existing rules for other paths
        match /{allPaths=**} {
          allow read, write: if request.auth != null;
        }
      }
    }

Click "Publish" to save.


🔄 STEP 4: UPDATE FIRESTORE WITH REAL URLs
═══════════════════════════════════════════

After uploading images to Storage, run:

    cd /Users/sunil/Downloads/SSBMax/scripts
    node update_ppdt_image_urls.js

This script will:
- Fetch batch_001 document from Firestore
- For each of the 30 images:
  * Check if image exists in Storage
  * Make image publicly accessible
  * Get the public URL
  * Update Firestore with real URL
- Save updated batch back to Firestore

Expected Output:
    🔄 Updating PPDT image URLs...
    📊 Found 30 images to update
    ✅ Updated ppdt_001: https://storage.googleapis.com/...
    ✅ Updated ppdt_002: https://storage.googleapis.com/...
    ...
    🎉 All image URLs updated successfully!


✅ VERIFICATION CHECKLIST
═════════════════════════

□ 30 PPDT images renamed correctly (ppdt_001.jpg to ppdt_030.jpg)
□ Images placed in /Users/sunil/Downloads/ppdt_images/
□ upload_ppdt_images.sh executed successfully
□ Firebase Storage rules updated to allow public read
□ update_ppdt_image_urls.js executed successfully
□ All 30 images showing real URLs (not placeholder URLs)


🧪 TESTING IN APP
═════════════════

1. Build and install debug APK:
   cd /Users/sunil/Downloads/SSBMax
   ./gradle.sh assembleDebug
   ./gradle.sh installDebug

2. Open SSBMax app
3. Navigate to PPDT Test
4. Click "Start Test"
5. Verify:
   □ Real PPDT image loads (not placeholder)
   □ Image is clear and visible
   □ Timer starts correctly (30 seconds)
   □ Can proceed to writing phase
   □ Can submit test successfully


📊 EXPECTED FINAL STRUCTURE
════════════════════════════

Firebase Storage:
    gs://ssbmax-56e16.appspot.com/
    └── ppdt_images/
        └── batch_001/
            ├── ppdt_001.jpg
            ├── ppdt_002.jpg
            ├── ppdt_003.jpg
            ...
            └── ppdt_030.jpg

Firestore:
    test_content/ppdt/image_batches/batch_001
    ├── batchId: "batch_001"
    ├── version: "1.0.0"
    ├── totalImages: 30
    └── images: [
          {
            id: "ppdt_001",
            imageUrl: "https://storage.googleapis.com/.../ppdt_001.jpg",
            imageDescription: "A hazy picture showing...",
            viewingTimeSeconds: 30,
            writingTimeMinutes: 4,
            minCharacters: 200,
            maxCharacters: 1000,
            category: "leadership",
            difficulty: "medium"
          },
          ...
        ]


🆘 TROUBLESHOOTING
══════════════════

Issue: "Firebase CLI not found"
Solution: npm install -g firebase-tools

Issue: "Not logged in to Firebase"
Solution: firebase login

Issue: "Permission denied" when uploading
Solution: firebase login --reauth

Issue: "Images not loading in app"
Solution: 
  1. Check Storage rules allow public read
  2. Verify imageUrl in Firestore is correct
  3. Test URL directly in browser
  4. Clear app cache and restart

Issue: "update_ppdt_image_urls.js fails"
Solution:
  1. Check service account JSON exists: .firebase/service-account.json
  2. Verify project ID in script matches your Firebase project
  3. Ensure images are uploaded to correct Storage path


📞 QUICK REFERENCE
═══════════════════

Upload Images:        ./upload_ppdt_images.sh
Update URLs:          node update_ppdt_image_urls.js
Check Storage:        https://console.firebase.google.com → Storage
Check Firestore:      https://console.firebase.google.com → Firestore
Build App:            ./gradle.sh assembleDebug
Install App:          ./gradle.sh installDebug


═══════════════════════════════════════════════════════════════
Created by: SSBMax Development Team
Last Updated: 2025-11-07
═══════════════════════════════════════════════════════════════
