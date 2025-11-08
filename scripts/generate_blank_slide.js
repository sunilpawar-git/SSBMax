#!/usr/bin/env node

/**
 * Generate a blank white slide for TAT test (12th image)
 * This creates a 1024x768 white JPEG image
 */

const fs = require('fs');
const path = require('path');
const { createCanvas } = require('canvas');

// Output path
const OUTPUT_DIR = '/Users/sunil/Downloads/TAT_images';
const OUTPUT_FILE = path.join(OUTPUT_DIR, 'blank_slide.jpg');

async function generateBlankSlide() {
  console.log('🎨 Generating blank white slide...\n');

  try {
    // Create canvas with standard resolution
    const width = 1024;
    const height = 768;
    const canvas = createCanvas(width, height);
    const ctx = canvas.getContext('2d');

    // Fill with white
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, width, height);

    // Convert to JPEG buffer
    const buffer = canvas.toBuffer('image/jpeg', { quality: 0.95 });

    // Ensure output directory exists
    if (!fs.existsSync(OUTPUT_DIR)) {
      fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    }

    // Write to file
    fs.writeFileSync(OUTPUT_FILE, buffer);

    console.log(`✅ Created: ${OUTPUT_FILE}`);
    console.log(`   Dimensions: ${width}x${height}`);
    console.log(`   Size: ${(buffer.length / 1024).toFixed(2)} KB\n`);

    console.log('✅ Blank slide generated successfully!');
    console.log('📝 Next: Upload this along with 57 TAT images to Firebase Storage\n');

    process.exit(0);
  } catch (error) {
    console.error('❌ Error generating blank slide:', error.message);
    console.error('\n💡 Make sure "canvas" package is installed:');
    console.error('   npm install canvas\n');
    process.exit(1);
  }
}

// Run
generateBlankSlide();

