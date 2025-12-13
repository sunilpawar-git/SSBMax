const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

/**
 * Create GPE batch document from uploaded images
 */
async function createGPEBatch() {
    try {
        console.log('📤 Creating GPE batch document...\n');

        // Read upload results
        const resultsPath = path.join(__dirname, 'gpe-upload-results.json');
        if (!fs.existsSync(resultsPath)) {
            throw new Error('Upload results not found. Run upload-gpe-images.js first.');
        }

        const uploadedImages = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));

        console.log(`📊 Creating batch with ${uploadedImages.length} images\n`);

        // GPE scenarios (you can customize these)
        const scenarios = [
            {
                scenario: "River crossing with limited resources",
                description: "Tactical scenario showing a river with available resources for crossing",
                resources: '["2 planks", "3 ropes", "1 barrel", "4 logs"]',
                difficulty: "MEDIUM"
            },
            {
                scenario: "Mountain terrain navigation",
                description: "Navigate through mountainous terrain with obstacles",
                resources: '["1 rope", "2 hooks", "3 wooden poles"]',
                difficulty: "HARD"
            },
            {
                scenario: "Urban obstacle course",
                description: "Navigate through urban obstacles with team",
                resources: '["2 ladders", "4 ropes", "1 plank"]',
                difficulty: "MEDIUM"
            },
            {
                scenario: "Bridge construction challenge",
                description: "Build a bridge across a gap using available materials",
                resources: '["3 planks", "2 ropes", "4 bamboo sticks"]',
                difficulty: "HARD"
            },
            {
                scenario: "Wall climbing exercise",
                description: "Scale a wall using limited equipment",
                resources: '["2 ropes", "1 ladder", "3 wooden blocks"]',
                difficulty: "MEDIUM"
            },
            {
                scenario: "Rescue operation simulation",
                description: "Rescue team members from difficult terrain",
                resources: '["1 stretcher", "3 ropes", "2 planks"]',
                difficulty: "HARD"
            },
            {
                scenario: "Supply transport challenge",
                description: "Transport supplies across obstacles",
                resources: '["2 barrels", "4 ropes", "3 planks"]',
                difficulty: "MEDIUM"
            },
            {
                scenario: "Trench crossing exercise",
                description: "Cross a trench using available materials",
                resources: '["2 planks", "2 ropes", "1 ladder"]',
                difficulty: "EASY"
            },
            {
                scenario: "Team coordination task",
                description: "Coordinate team movement through obstacles",
                resources: '["3 ropes", "2 poles", "1 plank"]',
                difficulty: "MEDIUM"
            },
            {
                scenario: "Strategic planning scenario",
                description: "Plan and execute a tactical movement",
                resources: '["2 ladders", "3 ropes", "2 planks"]',
                difficulty: "HARD"
            }
        ];

        // Create images array for batch
        const images = uploadedImages.map((img, index) => {
            const scenarioData = scenarios[index % scenarios.length];

            return {
                id: `gpe_${String(index + 1).padStart(3, '0')}`,
                imageUrl: img.publicUrl,
                scenario: scenarioData.scenario,
                imageDescription: scenarioData.description,
                resources: scenarioData.resources,
                viewingTimeSeconds: 60,
                planningTimeSeconds: 1740, // 29 minutes
                minCharacters: 500,
                maxCharacters: 2000,
                category: "Tactical",
                difficulty: scenarioData.difficulty
            };
        });

        const batchData = {
            batch_id: "batch_001",
            version: "1.0.0",
            image_count: images.length,
            uploaded_at: admin.firestore.Timestamp.now(),
            description: "GPE tactical scenario images - Base batch",
            images: images
        };

        // Upload to Firestore
        const batchRef = db.doc('test_content/gpe/image_batches/batch_001');
        await batchRef.set(batchData);

        console.log('✅ Batch document created successfully!');

        // Update or create metadata
        const metaRef = db.doc('test_content/gpe/meta/overview');
        const metaDoc = await metaRef.get();

        if (metaDoc.exists) {
            const currentData = metaDoc.data();
            const updatedBatches = currentData.available_batches || [];
            if (!updatedBatches.includes('batch_001')) {
                updatedBatches.push('batch_001');
                updatedBatches.sort();
            }

            await metaRef.update({
                total_images: (currentData.total_images || 0) + images.length,
                available_batches: updatedBatches,
                last_updated: admin.firestore.Timestamp.now()
            });

            console.log('✅ Metadata updated!');
        } else {
            await metaRef.set({
                total_images: images.length,
                available_batches: ['batch_001'],
                last_updated: admin.firestore.Timestamp.now(),
                content_type: 'gpe_images'
            });

            console.log('✅ Metadata created!');
        }

        console.log('\n🎉 GPE batch creation complete!\n');
        console.log('📊 Summary:');
        console.log(`   ✅ ${images.length} images in batch`);
        console.log(`   ✅ Batch: batch_001`);
        console.log(`   ✅ Version: 1.0.0\n`);

        // Save batch data to JSON for reference
        const batchPath = path.join(__dirname, 'gpe-batch-001.json');
        fs.writeFileSync(batchPath, JSON.stringify(batchData, null, 2));
        console.log(`📝 Batch data saved to: ${batchPath}\n`);

    } catch (error) {
        console.error('❌ Batch creation failed:', error);
        throw error;
    }
}

// Run the batch creation
createGPEBatch()
    .then(() => {
        console.log('✨ Batch creation completed successfully!');
        process.exit(0);
    })
    .catch((error) => {
        console.error('💥 Batch creation failed:', error);
        process.exit(1);
    });
