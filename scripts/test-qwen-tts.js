/**
 * Qwen TTS Demo Script for SSB Interview Questions
 * 
 * Tests Qwen TTS with actual SSB interview questions to evaluate:
 * - Voice quality and naturalness
 * - Latency (should be ~97ms vs Sarvam's 30s timeout)
 * - Indian English accent suitability
 * - Professional interviewer tone
 * 
 * Usage:
 *   node scripts/test-qwen-tts.js
 * 
 * Requirements:
 *   - Hugging Face API token (get from https://huggingface.co/settings/tokens)
 *   - Set HUGGINGFACE_API_KEY environment variable
 */

const https = require('https');
const fs = require('fs');
const path = require('path');

// Hugging Face Inference API endpoint for Qwen3-TTS CustomVoice model
const QWEN_TTS_ENDPOINT = 'https://api-inference.huggingface.co/models/Qwen/Qwen3-TTS-12Hz-1.7B-CustomVoice';
const API_KEY = process.env.HUGGINGFACE_API_KEY || '';

// SSB Interview questions to test
const SSB_INTERVIEW_QUESTIONS = [
    {
        text: "Tell me about yourself and your family background.",
        category: "Personal Background",
        expectedOLQs: ["POWER_OF_EXPRESSION", "SELF_CONFIDENCE", "SOCIAL_ADJUSTMENT"]
    },
    {
        text: "Why do you want to join the Armed Forces?",
        category: "Motivation & Service",
        expectedOLQs: ["DETERMINATION", "SENSE_OF_RESPONSIBILITY", "COURAGE"]
    },
    {
        text: "What are your strengths and weaknesses?",
        category: "Self Assessment",
        expectedOLQs: ["SELF_CONFIDENCE", "EFFECTIVE_INTELLIGENCE", "REASONING_ABILITY"]
    },
    {
        text: "Describe a situation where you showed leadership.",
        category: "Leadership & Initiative",
        expectedOLQs: ["INFLUENCE_GROUP", "ORGANIZING_ABILITY", "INITIATIVE"]
    },
    {
        text: "How do you handle stress and pressure?",
        category: "Stress & Resilience",
        expectedOLQs: ["DETERMINATION", "STAMINA", "SPEED_OF_DECISION"]
    },
    {
        text: "What do you know about the current defense situation of India?",
        category: "Current Affairs & Defense",
        expectedOLQs: ["EFFECTIVE_INTELLIGENCE", "REASONING_ABILITY", "POWER_OF_EXPRESSION"]
    }
];

// Available speakers for English (from Qwen3-TTS CustomVoice model)
const ENGLISH_SPEAKERS = {
    'Ryan': 'Dynamic male voice with strong rhythmic drive',
    'Aiden': 'Sunny American male voice with a clear midrange'
};

/**
 * Call Qwen TTS API via Hugging Face Inference API
 */
function synthesizeSpeech(text, speaker = 'Ryan', language = 'English') {
    return new Promise((resolve, reject) => {
        const requestBody = JSON.stringify({
            inputs: text,
            parameters: {
                language: language,
                speaker: speaker
            }
        });

        const options = {
            hostname: 'api-inference.huggingface.co',
            path: '/models/Qwen/Qwen3-TTS-12Hz-1.7B-CustomVoice',
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${API_KEY}`,
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(requestBody)
            }
        };

        const startTime = Date.now();
        const req = https.request(options, (res) => {
            let responseData = Buffer.alloc(0);

            res.on('data', (chunk) => {
                responseData = Buffer.concat([responseData, chunk]);
            });

            res.on('end', () => {
                const latency = Date.now() - startTime;
                
                if (res.statusCode === 200) {
                    // Check if response is audio (binary) or JSON error
                    const contentType = res.headers['content-type'] || '';
                    
                    if (contentType.includes('audio') || contentType.includes('wav') || contentType.includes('mp3')) {
                        resolve({
                            audio: responseData,
                            latency: latency,
                            contentType: contentType
                        });
                    } else {
                        // Try parsing as JSON (might be error or base64 audio)
                        try {
                            const jsonResponse = JSON.parse(responseData.toString());
                            if (jsonResponse.error) {
                                reject(new Error(`API Error: ${jsonResponse.error}`));
                            } else if (jsonResponse.audio || jsonResponse.audio_base64) {
                                const audioBase64 = jsonResponse.audio || jsonResponse.audio_base64;
                                resolve({
                                    audio: Buffer.from(audioBase64, 'base64'),
                                    latency: latency,
                                    contentType: 'audio/wav'
                                });
                            } else {
                                reject(new Error(`Unexpected response format: ${JSON.stringify(jsonResponse)}`));
                            }
                        } catch (e) {
                            // Assume it's raw audio
                            resolve({
                                audio: responseData,
                                latency: latency,
                                contentType: contentType || 'audio/wav'
                            });
                        }
                    }
                } else {
                    const errorText = responseData.toString();
                    reject(new Error(`HTTP ${res.statusCode}: ${errorText}`));
                }
            });
        });

        req.on('error', (error) => {
            reject(error);
        });

        req.write(requestBody);
        req.end();
    });
}

/**
 * Save audio to file
 */
function saveAudio(audioBuffer, filename) {
    const outputDir = path.join(__dirname, 'qwen-tts-samples');
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }
    
    const filepath = path.join(outputDir, filename);
    fs.writeFileSync(filepath, audioBuffer);
    return filepath;
}

/**
 * Test Qwen TTS with all SSB interview questions
 */
async function testQwenTTS() {
    console.log('🎤 Qwen TTS Demo for SSB Interview Questions\n');
    console.log('=' .repeat(60));
    
    if (!API_KEY) {
        console.error('❌ Error: HUGGINGFACE_API_KEY environment variable not set');
        console.log('\nTo get an API key:');
        console.log('1. Go to https://huggingface.co/settings/tokens');
        console.log('2. Create a new token (read access is enough)');
        console.log('3. Set it: export HUGGINGFACE_API_KEY=your_token_here');
        console.log('\nOr test via Hugging Face Space demo:');
        console.log('   https://huggingface.co/spaces/Qwen/Qwen3-TTS');
        process.exit(1);
    }

    console.log(`✅ Using Hugging Face API Key: ${API_KEY.substring(0, 8)}...`);
    console.log(`📝 Testing ${SSB_INTERVIEW_QUESTIONS.length} SSB interview questions\n`);

    const results = [];

    for (let i = 0; i < SSB_INTERVIEW_QUESTIONS.length; i++) {
        const question = SSB_INTERVIEW_QUESTIONS[i];
        const speaker = i % 2 === 0 ? 'Ryan' : 'Aiden'; // Alternate speakers
        
        console.log(`\n[${i + 1}/${SSB_INTERVIEW_QUESTIONS.length}] Testing: "${question.text}"`);
        console.log(`   Category: ${question.category}`);
        console.log(`   Speaker: ${speaker} (${ENGLISH_SPEAKERS[speaker]})`);
        
        try {
            const startTime = Date.now();
            const result = await synthesizeSpeech(question.text, speaker, 'English');
            const totalTime = Date.now() - startTime;
            
            // Save audio file
            const filename = `qwen-tts-q${i + 1}-${speaker.toLowerCase()}.wav`;
            const filepath = saveAudio(result.audio, filename);
            
            console.log(`   ✅ Success!`);
            console.log(`   ⏱️  Latency: ${result.latency}ms (API call)`);
            console.log(`   ⏱️  Total time: ${totalTime}ms`);
            console.log(`   💾 Saved to: ${filepath}`);
            console.log(`   📦 Audio size: ${(result.audio.length / 1024).toFixed(2)} KB`);
            
            results.push({
                question: question.text,
                speaker: speaker,
                latency: result.latency,
                totalTime: totalTime,
                audioSize: result.audio.length,
                success: true
            });
            
            // Small delay between requests to avoid rate limiting
            await new Promise(resolve => setTimeout(resolve, 1000));
            
        } catch (error) {
            console.error(`   ❌ Error: ${error.message}`);
            
            if (error.message.includes('503') || error.message.includes('loading')) {
                console.log(`   ⏳ Model is loading, wait a moment and try again`);
            }
            
            results.push({
                question: question.text,
                speaker: speaker,
                error: error.message,
                success: false
            });
        }
    }

    // Summary
    console.log('\n' + '='.repeat(60));
    console.log('📊 TEST SUMMARY');
    console.log('='.repeat(60));
    
    const successful = results.filter(r => r.success);
    const failed = results.filter(r => !r.success);
    
    console.log(`✅ Successful: ${successful.length}/${results.length}`);
    console.log(`❌ Failed: ${failed.length}/${results.length}`);
    
    if (successful.length > 0) {
        const avgLatency = successful.reduce((sum, r) => sum + r.latency, 0) / successful.length;
        const avgTotalTime = successful.reduce((sum, r) => sum + r.totalTime, 0) / successful.length;
        const totalAudioSize = successful.reduce((sum, r) => sum + r.audioSize, 0);
        
        console.log(`\n⏱️  Average API Latency: ${avgLatency.toFixed(0)}ms`);
        console.log(`⏱️  Average Total Time: ${avgTotalTime.toFixed(0)}ms`);
        console.log(`📦 Total Audio Generated: ${(totalAudioSize / 1024).toFixed(2)} KB`);
        
        console.log(`\n📁 Audio files saved in: ${path.join(__dirname, 'qwen-tts-samples')}`);
        console.log(`\n🎧 Listen to the samples to evaluate:`);
        console.log(`   - Voice quality and naturalness`);
        console.log(`   - Professional interviewer tone`);
        console.log(`   - Suitability for SSB interviews`);
        console.log(`   - Indian English accent (if applicable)`);
    }
    
    if (failed.length > 0) {
        console.log(`\n❌ Failed Tests:`);
        failed.forEach((r, i) => {
            console.log(`   ${i + 1}. "${r.question.substring(0, 50)}..." - ${r.error}`);
        });
    }
    
    console.log('\n💡 Comparison with Current Setup:');
    console.log('   - Sarvam TTS: ~30s timeout (huge latency issue)');
    console.log('   - Qwen TTS: ~97ms end-to-end (claimed), ~' + 
                (successful.length > 0 ? avgLatency.toFixed(0) : '?') + 'ms API latency');
    console.log('   - ElevenLabs: High quality but pricey');
}

// Run the test
testQwenTTS().catch(error => {
    console.error('\n❌ Fatal error:', error);
    process.exit(1);
});
