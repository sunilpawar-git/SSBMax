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

        const gpeImagesPath = '/Users/sunil/Downloads/GPE Pics';

        // Check if directory exists
        if (!fs.existsSync(gpeImagesPath)) {
            throw new Error(`Directory not found: ${gpeImagesPath}`);
        }

        // Get all image files
        const files = fs.readdirSync(gpeImagesPath)
            .filter(file => /\.(jpg|jpeg|png|webp)$/i.test(file))
            .sort();

        console.log(`📊 Found ${files.length} images to upload\n`);

        const uploadedImages = [];

        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            const localPath = path.join(gpeImagesPath, file);
            const storagePath = `gpe_images/${file}`;

            console.log(`[${i + 1}/${files.length}] Uploading ${file}...`);

            try {
                // Upload to Firebase Storage
                await bucket.upload(localPath, {
                    destination: storagePath,
                    metadata: {
                        contentType: 'image/jpeg',
                        metadata: {
                            firebaseStorageDownloadTokens: require('uuid').v4()
                        }
                    }
                });

                // Make file publicly accessible
                await bucket.file(storagePath).makePublic();

                const publicUrl = `https://storage.googleapis.com/${bucket.name}/${storagePath}`;

                uploadedImages.push({
                    fileName: file,
                    storagePath: storagePath,
                    publicUrl: publicUrl,
                    index: i + 1
                });

                console.log(`   ✅ Uploaded: ${publicUrl}\n`);
            } catch (error) {
                console.error(`   ❌ Failed to upload ${file}:`, error.message);
            }
        }

        console.log('\n🎉 Upload complete!\n');
        console.log('📊 Summary:');
        console.log(`   ✅ ${uploadedImages.length}/${files.length} images uploaded\n`);

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
