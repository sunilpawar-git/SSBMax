#!/usr/bin/env node

/**
 * Firestore Content Verification Script
 * 
 * Verifies that all study materials are present in Firestore before
 * deleting legacy hardcoded content files.
 * 
 * Usage: node scripts/verify_firestore_content.js
 */

const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Initialize Firebase Admin SDK
const serviceAccountPath = path.join(__dirname, '..', '.firebase', 'service-account.json');

if (!fs.existsSync(serviceAccountPath)) {
    console.error('❌ Service account key not found at:', serviceAccountPath);
    console.error('   Please ensure the service account JSON file exists.');
    process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Expected topics
const EXPECTED_TOPICS = [
    'OIR',
    'PPDT',
    'PSYCHOLOGY',
    'PIQ_FORM',
    'GTO',
    'INTERVIEW',
    'SSB_OVERVIEW',
    'MEDICALS',
    'CONFERENCE'
];

// Expected minimum materials per topic
const EXPECTED_MATERIALS_PER_TOPIC = {
    'OIR': 7,
    'PPDT': 6,
    'PSYCHOLOGY': 8,
    'PIQ_FORM': 3,
    'GTO': 7,
    'INTERVIEW': 7,
    'SSB_OVERVIEW': 4,
    'MEDICALS': 3,
    'CONFERENCE': 3
};

async function verifyTopicContent() {
    console.log('\n📋 Verifying topic_content collection...\n');
    
    const topicContentRef = db.collection('topic_content');
    const snapshot = await topicContentRef.get();
    
    const foundTopics = [];
    const missingTopics = [];
    
    snapshot.forEach(doc => {
        const data = doc.data();
        foundTopics.push({
            id: doc.id,
            title: data.title || 'N/A',
            hasIntroduction: !!data.introduction && data.introduction.length > 0,
            introductionLength: data.introduction?.length || 0
        });
    });
    
    EXPECTED_TOPICS.forEach(topic => {
        const found = foundTopics.find(t => t.id === topic);
        if (found) {
            console.log(`✅ ${topic.padEnd(15)} - Found (intro: ${found.introductionLength} chars)`);
        } else {
            console.log(`❌ ${topic.padEnd(15)} - MISSING`);
            missingTopics.push(topic);
        }
    });
    
    console.log(`\n📊 Summary: ${foundTopics.length}/${EXPECTED_TOPICS.length} topics found`);
    
    if (missingTopics.length > 0) {
        console.log(`\n⚠️  Missing topics: ${missingTopics.join(', ')}`);
        return false;
    }
    
    // Verify introduction content
    const topicsWithoutContent = foundTopics.filter(t => !t.hasIntroduction || t.introductionLength < 100);
    if (topicsWithoutContent.length > 0) {
        console.log(`\n⚠️  Topics with insufficient introduction content:`);
        topicsWithoutContent.forEach(t => {
            console.log(`   - ${t.id}: ${t.introductionLength} chars`);
        });
        return false;
    }
    
    return true;
}

async function verifyStudyMaterials() {
    console.log('\n📚 Verifying study_materials collection...\n');
    
    const materialsRef = db.collection('study_materials');
    const snapshot = await materialsRef.get();
    
    const materialsByTopic = {};
    const materialsWithContent = [];
    const materialsWithoutContent = [];
    
    snapshot.forEach(doc => {
        const data = doc.data();
        const topicType = data.topicType || 'UNKNOWN';
        
        if (!materialsByTopic[topicType]) {
            materialsByTopic[topicType] = [];
        }
        
        const hasContent = !!data.contentMarkdown && data.contentMarkdown.length > 100;
        const material = {
            id: doc.id,
            title: data.title || 'N/A',
            topicType: topicType,
            hasContent: hasContent,
            contentLength: data.contentMarkdown?.length || 0
        };
        
        materialsByTopic[topicType].push(material);
        
        if (hasContent) {
            materialsWithContent.push(material);
        } else {
            materialsWithoutContent.push(material);
        }
    });
    
    // Check each topic
    let allTopicsValid = true;
    EXPECTED_TOPICS.forEach(topic => {
        const materials = materialsByTopic[topic] || [];
        const expected = EXPECTED_MATERIALS_PER_TOPIC[topic] || 0;
        const withContent = materials.filter(m => m.hasContent).length;
        
        if (materials.length >= expected) {
            console.log(`✅ ${topic.padEnd(15)} - ${materials.length} materials (${withContent} with content, expected: ${expected})`);
        } else {
            console.log(`❌ ${topic.padEnd(15)} - ${materials.length} materials (expected: ${expected})`);
            allTopicsValid = false;
        }
    });
    
    console.log(`\n📊 Summary:`);
    console.log(`   Total materials: ${snapshot.size}`);
    console.log(`   Materials with content: ${materialsWithContent.length}`);
    console.log(`   Materials without content: ${materialsWithoutContent.length}`);
    
    if (materialsWithoutContent.length > 0) {
        console.log(`\n⚠️  Materials missing contentMarkdown:`);
        materialsWithoutContent.slice(0, 10).forEach(m => {
            console.log(`   - ${m.topicType}/${m.id}: "${m.title}" (${m.contentLength} chars)`);
        });
        if (materialsWithoutContent.length > 10) {
            console.log(`   ... and ${materialsWithoutContent.length - 10} more`);
        }
    }
    
    return allTopicsValid && materialsWithoutContent.length === 0;
}

async function sampleContentCheck() {
    console.log('\n🔍 Sampling content quality...\n');
    
    const materialsRef = db.collection('study_materials');
    const snapshot = await materialsRef.limit(5).get();
    
    if (snapshot.empty) {
        console.log('❌ No materials found to sample');
        return false;
    }
    
    let samplesChecked = 0;
    snapshot.forEach(doc => {
        const data = doc.data();
        const contentLength = data.contentMarkdown?.length || 0;
        const hasMarkdown = data.contentMarkdown?.includes('#') || false;
        const hasParagraphs = data.contentMarkdown?.includes('\n\n') || false;
        
        console.log(`📄 ${doc.id}:`);
        console.log(`   Title: ${data.title || 'N/A'}`);
        console.log(`   Topic: ${data.topicType || 'N/A'}`);
        console.log(`   Content length: ${contentLength} chars`);
        console.log(`   Has markdown: ${hasMarkdown ? '✅' : '❌'}`);
        console.log(`   Has paragraphs: ${hasParagraphs ? '✅' : '❌'}`);
        console.log(`   Preview: ${data.contentMarkdown?.substring(0, 100).replace(/\n/g, ' ')}...`);
        console.log('');
        
        samplesChecked++;
    });
    
    return samplesChecked > 0;
}

async function main() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🔍 Firestore Content Verification');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    try {
        // Step 1: Verify topic_content
        const topicsValid = await verifyTopicContent();
        
        // Step 2: Verify study_materials
        const materialsValid = await verifyStudyMaterials();
        
        // Step 3: Sample content quality
        const samplesValid = await sampleContentCheck();
        
        // Final verdict
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        if (topicsValid && materialsValid && samplesValid) {
            console.log('✅ VERIFICATION PASSED');
            console.log('   All content is present in Firestore.');
            console.log('   Safe to proceed with Phase 1 (delete legacy files).');
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            process.exit(0);
        } else {
            console.log('❌ VERIFICATION FAILED');
            console.log('   Some content is missing or incomplete.');
            console.log('   DO NOT proceed with Phase 1 deletion.');
            console.log('   Upload missing content to Firestore first.');
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            process.exit(1);
        }
    } catch (error) {
        console.error('\n❌ ERROR:', error.message);
        console.error('\nStack trace:', error.stack);
        process.exit(1);
    } finally {
        // Cleanup
        await admin.app().delete();
    }
}

main();

