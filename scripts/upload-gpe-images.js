const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const bucket = admin.storage().bucket('ssbmax-49e68.firebasestorage.app');

/**
 * Upload GPE images from local folder to Firebase Storage
 */
async function uploadGPEImages() {
    try {
        console.log('📤 Starting GPE image upload...\n');

        // Target the specific generated asset
        const localPath = path.join(__dirname, '../app/src/main/assets/gpe_gen_map.png');
        const fileName = 'gpe_gen_map.png';

        // Check if file exists
        if (!fs.existsSync(localPath)) {
            throw new Error(`Generated map not found at: ${localPath}`);
        }

        console.log(`📊 Found generated map to upload\n`);

        const uploadedImages = [];

        const storagePath = `gpe_images/${fileName}`;

        console.log(`Uploading ${fileName}...`);

        try {
            // Upload to Firebase Storage
            await bucket.upload(localPath, {
                destination: storagePath,
                metadata: {
                    contentType: 'image/png', // It's a PNG
                    metadata: {
                        firebaseStorageDownloadTokens: require('uuid').v4()
                    }
                }
            });

            // Make file publicly accessible
            await bucket.file(storagePath).makePublic();

            const publicUrl = `https://storage.googleapis.com/${bucket.name}/${storagePath}`;

            uploadedImages.push({
                fileName: fileName,
                storagePath: storagePath,
                publicUrl: publicUrl,
                index: 1
            });

            console.log(`   ✅ Uploaded: ${publicUrl}\n`);
        } catch (error) {
            console.error(`   ❌ Failed to upload ${fileName}:`, error.message);
            throw error;
        }

        console.log('\n🎉 Upload complete!\n');

        // Save upload results to JSON for creating batch file
        const resultsPath = path.join(__dirname, 'gpe-upload-results.json');
        fs.writeFileSync(resultsPath, JSON.stringify(uploadedImages, null, 2));
        console.log(`📝 Upload results saved to: ${resultsPath}\n`);

        return uploadedImages;
    } catch (error) {
        console.error('❌ Upload failed:', error);
        throw error;
    }
}

// Run the upload
uploadGPEImages()
    .then(() => {
        console.log('✨ Upload process completed successfully!');
        process.exit(0);
    })
    .catch((error) => {
        console.error('💥 Upload process failed:', error);
        process.exit(1);
    });
