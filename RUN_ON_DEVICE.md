# Running SSBMax on Pixel 9 via Android Studio

## ✅ Device Status
**Device Detected:** Pixel 9 (Wireless ADB Connection)  
**Connection Type:** adb-tls-connect (WiFi Debugging)  
**Status:** Ready to deploy

## 🚀 Quick Start - Run from Android Studio

### Method 1: Using Android Studio GUI (Recommended)

1. **Open the Project**
   - Launch Android Studio
   - Open the SSBMax project folder: `/Users/sunil/Downloads/SSBMax`

2. **Select Your Device**
   - Look at the top toolbar in Android Studio
   - Click the device dropdown menu (next to the run button ▶️)
   - You should see your Pixel 9 listed as:
     - "Pixel 9" or "4A231VDAQ0001D" (your device ID)
   - Select it

3. **Run the App**
   - Click the green **Run** button ▶️ (or press `Ctrl+R` / `Cmd+R`)
   - Android Studio will:
     - Build the app (if needed)
     - Install it on your Pixel 9
     - Launch the app automatically

4. **View the Dashboard**
   - The SSBMax dashboard will appear on your Pixel 9!
   - You'll see all the features we just implemented

### Method 2: Using Command Line

If you prefer the command line, you can use:

```bash
# From the project root directory
./gradlew installDebug

# The app will install on your connected Pixel 9
# Then manually open it from your phone's app drawer
```

### Method 3: Direct APK Installation

```bash
# Build the APK
./gradlew assembleDebug

# Install on device
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
~/Library/Android/sdk/platform-tools/adb shell am start -n com.ssbmax/.MainActivity
```

## 📱 Expected Behavior

When the app launches on your Pixel 9, you should see:

1. **Splash/Loading** (if configured)
2. **Dashboard Screen** with:
   - Welcome card showing "Welcome back, Aspirant!"
   - Study streak: 7 days
   - Tests completed: 21
   - Study hours: 34.5h
   - SSB category cards (Psychology, GTO, Interview, Conference)
   - Quick action buttons
   - Psychology tests list (TAT, WAT, SRT, SD)
   - Daily tip
   - Progress overview
   - Recent activity feed

## 🔧 Troubleshooting

### Device Not Showing in Android Studio?

1. **Check ADB Connection:**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb devices
   ```
   - Should show your device as "device" (not "unauthorized")

2. **If Device Shows "Unauthorized":**
   - Check your Pixel 9 screen for an authorization prompt
   - Tap "Allow" and check "Always allow from this computer"

3. **Reconnect Device:**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb kill-server
   ~/Library/Android/sdk/platform-tools/adb start-server
   ~/Library/Android/sdk/platform-tools/adb devices
   ```

4. **Switch to USB Connection:**
   - Connect Pixel 9 via USB-C cable
   - Enable USB Debugging in Developer Options
   - Device should appear immediately

### App Won't Install?

1. **Uninstall Previous Version:**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb uninstall com.ssbmax
   ```

2. **Clear Android Studio Cache:**
   - In Android Studio: File → Invalidate Caches → Invalidate and Restart

3. **Clean and Rebuild:**
   ```bash
   ./gradlew clean assembleDebug
   ```

### App Crashes on Launch?

1. **Check Logcat in Android Studio:**
   - View → Tool Windows → Logcat
   - Filter by package name: `com.ssbmax`

2. **View Crash Logs:**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb logcat -s AndroidRuntime:E
   ```

## 🎯 Development Workflow

### Hot Reload (Live Changes)

Android Studio supports:
- **Apply Changes** (⌘⇧R / Ctrl+Shift+R) - Quick updates without restart
- **Apply Code Changes** - Updates running code
- **Run** - Full rebuild and install

### Debugging

1. **Set Breakpoints:**
   - Click on the left margin of code lines in Android Studio
   - Red dots indicate breakpoints

2. **Debug Mode:**
   - Click the **Debug** button 🐛 (next to Run)
   - App will pause at breakpoints
   - Inspect variables and step through code

### Viewing Logs

In Android Studio's Logcat, filter by:
- **Package:** `com.ssbmax`
- **Tag:** Custom tags like `DashboardViewModel`, `MainActivity`
- **Log Level:** Verbose, Debug, Info, Warn, Error

## 📊 Device Information

Your Pixel 9 specifications:
- **Display:** High refresh rate OLED
- **Android Version:** Latest (Android 14+)
- **Resolution:** 1080 x 2424 pixels
- **Perfect for testing:** Material Design 3 dynamic theming

## 🎨 Testing Features on Pixel 9

### Material You Dynamic Colors
The app supports Material You, so:
- Colors adapt to your Pixel 9's wallpaper
- Test by changing wallpaper and reopening app

### Gesture Navigation
- Swipe gestures work seamlessly
- Edge-to-edge design implemented

### Dark Mode
- Toggle dark mode in Pixel 9 settings
- App should adapt automatically (if theme supports it)

## 🔄 Wireless Debugging

Your device is already connected wirelessly! To set up manually:

1. **On Pixel 9:**
   - Settings → Developer Options → Wireless Debugging
   - Enable it
   - Tap "Pair device with pairing code"

2. **On Mac:**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb pair <IP>:<PORT>
   # Enter pairing code from device
   ```

3. **Connect:**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb connect <IP>:<PORT>
   ```

## 📱 Quick Commands Reference

```bash
# Check device connection
~/Library/Android/sdk/platform-tools/adb devices

# Install app
./gradlew installDebug

# Uninstall app
~/Library/Android/sdk/platform-tools/adb uninstall com.ssbmax

# Launch app
~/Library/Android/sdk/platform-tools/adb shell am start -n com.ssbmax/.MainActivity

# View logs
~/Library/Android/sdk/platform-tools/adb logcat | grep SSBMax

# Take screenshot
~/Library/Android/sdk/platform-tools/adb exec-out screencap -p > screenshot.png

# Record screen
~/Library/Android/sdk/platform-tools/adb shell screenrecord /sdcard/demo.mp4
```

## ✅ Current Status

- ✅ Project built successfully
- ✅ APK generated (16 MB)
- ✅ Device connected (Pixel 9)
- ✅ Ready to install and run!

---

**Next Step:** Open Android Studio and click the Run button! 🚀

