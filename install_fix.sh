#!/bin/bash

# SSBMax - Install Fixed Build
# This script builds and installs the fixed version to your connected device

echo "🚀 SSBMax - Installing Fixed Build"
echo "===================================="
echo ""

# Set Java Home
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Navigate to project directory
cd "$(dirname "$0")"

echo "📱 Checking for connected devices..."
adb devices

echo ""
echo "🔨 Building and installing..."
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Installation successful!"
    echo ""
    echo "📱 Next steps:"
    echo "1. Open SSBMax app on your device"
    echo "2. Navigate to: Profile → My Submissions"
    echo "3. You should see 'No Submissions Yet' with 'Take a Test' button"
    echo "4. Click 'Take a Test' to start!"
    echo ""
    echo "🎉 Enjoy your improved app!"
else
    echo ""
    echo "❌ Installation failed!"
    echo "Please check:"
    echo "- Is your device connected?"
    echo "- Is USB debugging enabled?"
    echo "- Did you accept the installation prompt?"
fi

