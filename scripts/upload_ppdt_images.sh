#!/bin/bash

# Configuration
IMAGES_DIR="/Users/sunil/Downloads/ppdt_images"
PROJECT_ID="ssbmax-56e16"  # Update if different

echo "📤 PPDT Images Upload Script"
echo "=============================="
echo ""

# Check if images directory exists
if [ ! -d "$IMAGES_DIR" ]; then
    echo "❌ Error: Images directory not found: $IMAGES_DIR"
    echo "📝 Please create the directory and place your PPDT images there"
    echo "   Images should be named: ppdt_001.jpg, ppdt_002.jpg, ..., ppdt_030.jpg"
    exit 1
fi

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI not found"
    echo "📝 Install it with: npm install -g firebase-tools"
    exit 1
fi

# Check if logged in
echo "🔐 Checking Firebase authentication..."
if ! firebase projects:list &> /dev/null; then
    echo "❌ Not logged in to Firebase"
    echo "📝 Run: firebase login"
    exit 1
fi

echo "✅ Firebase CLI ready"
echo ""

# Count images
IMAGE_COUNT=$(ls -1 "$IMAGES_DIR"/ppdt_*.jpg 2>/dev/null | wc -l)
echo "📊 Found $IMAGE_COUNT PPDT images in $IMAGES_DIR"
echo ""

if [ "$IMAGE_COUNT" -eq 0 ]; then
    echo "⚠️  No PPDT images found!"
    echo "📝 Please add images named: ppdt_001.jpg, ppdt_002.jpg, etc."
    exit 1
fi

# Confirm upload
read -p "Upload $IMAGE_COUNT images to Firebase Storage? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Upload cancelled"
    exit 0
fi

echo ""
echo "📤 Starting upload..."
echo ""

# Upload each image
UPLOADED=0
FAILED=0

# Get all ppdt images and upload them
for LOCAL_PATH in "$IMAGES_DIR"/ppdt_*.jpg; do
    if [ -f "$LOCAL_PATH" ]; then
        IMAGE_FILE=$(basename "$LOCAL_PATH")
        STORAGE_PATH="ppdt_images/batch_001/$IMAGE_FILE"
        
        echo "⬆️  Uploading $IMAGE_FILE..."
        
        # Use Firebase CLI to upload
        if firebase storage:upload "$LOCAL_PATH" "$STORAGE_PATH" --project "$PROJECT_ID"; then
            echo "   ✅ Success"
            ((UPLOADED++))
        else
            echo "   ❌ Failed (see error above)"
            ((FAILED++))
        fi
    fi
done

echo ""
echo "=============================="
echo "📊 Upload Summary"
echo "=============================="
echo "✅ Uploaded: $UPLOADED images"
echo "❌ Failed: $FAILED images"
echo ""

if [ $UPLOADED -gt 0 ]; then
    echo "🎉 Upload complete!"
    echo ""
    echo "📝 Next steps:"
    echo "1. Update Storage rules to allow public read (see PPDT_IMAGE_UPLOAD_GUIDE.txt)"
    echo "2. Run: node update_ppdt_image_urls.js"
    echo "3. Test PPDT in app"
else
    echo "❌ No images were uploaded"
fi
