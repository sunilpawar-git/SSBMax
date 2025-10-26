#!/bin/bash

# Script to update google-services.json after adding SHA-1 to Firebase

echo "🔄 Updating google-services.json..."

# Backup existing file
if [ -f "app/google-services.json" ]; then
    cp app/google-services.json app/google-services.json.backup.$(date +%Y%m%d_%H%M%S)
    echo "✅ Backed up existing google-services.json"
fi

# Check if new file exists in Downloads
if [ -f "$HOME/Downloads/google-services.json" ]; then
    cp "$HOME/Downloads/google-services.json" app/google-services.json
    echo "✅ Copied new google-services.json from Downloads"
    echo ""
    echo "📋 Next steps:"
    echo "1. Clean build: ./gradle.sh clean"
    echo "2. Install app: ./gradle.sh installDebug"
    echo "3. Clear app data: adb shell pm clear com.ssbmax"
    echo "4. Test sign-in!"
else
    echo "❌ Could not find google-services.json in ~/Downloads"
    echo ""
    echo "📥 Please download google-services.json from Firebase Console:"
    echo "1. Go to: https://console.firebase.google.com"
    echo "2. Project Settings → Your App"
    echo "3. Download google-services.json"
    echo "4. Run this script again"
fi

