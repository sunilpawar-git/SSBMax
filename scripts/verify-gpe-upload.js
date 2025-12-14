const admin = require('firebase-admin');

const serviceAccount = require('../.firebase/service-account.json');

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function verify() {
    console.log('🔍 Verifying GPE Upload...');

    // Path matches FirestoreGTORepository.kt
    // test_content/gto/scenarios/gpe/batches/batch_001
    const path = 'test_content/gto/scenarios/gpe/batches/batch_001';

    const doc = await db.doc(path).get();

    if (!doc.exists) {
        console.error('❌ Document NOT found at:', path);
        return;
    }

    const data = doc.data();
    console.log('✅ Document found!');
    console.log('   Image Count:', data.image_count);
    console.log('   Images array length:', data.images ? data.images.length : 0);

    if (data.images && data.images.length > 0) {
        const first = data.images[0];
        console.log('\n   First Scenario Sample:');
        console.log('   - ID:', first.id);
        console.log('   - Solution present:', !!first.solution);
        console.log('   - Solution length:', first.solution ? first.solution.length : 0);
    } else {
        console.warn('   ⚠️ No images array found!');
    }
}

verify().catch(console.error);
