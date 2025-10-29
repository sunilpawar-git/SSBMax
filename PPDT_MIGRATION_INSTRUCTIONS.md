# PPDT Migration - Step-by-Step Instructions

**Status**: ✅ Build Complete - Ready for Migration  
**Date**: October 29, 2025

---

## ✅ Completed Steps

### 1. Feature Flag Enabled ✅
**File**: `ContentFeatureFlags.kt`
```kotlin
private val topicFlags = mutableMapOf<String, Boolean>(
    "OIR" to true,
    "PPDT" to true  // ← Added
)
```

### 2. Build Successful ✅
**APK Location**: `/Users/sunil/Downloads/SSBMax/app/build/outputs/apk/debug/app-debug.apk`

**Build Output**: 
```
BUILD SUCCESSFUL in 21s
163 actionable tasks: 25 executed, 138 up-to-date
```

---

## 🚀 Next Steps (Manual Execution Required)

### Step 1: Install APK on Device

```bash
# Connect your Android device or start emulator
# Then run:
adb install -r /Users/sunil/Downloads/SSBMax/app/build/outputs/apk/debug/app-debug.apk
```

**Expected Output**:
```
Performing Streamed Install
Success
```

---

### Step 2: Start Logging (Before Opening App)

Open a new terminal and start comprehensive logging:

```bash
adb logcat -s TopicViewModel:D StudyContentRepo:D MigrateUseCase:D FirestoreContentSource:D -v time > ppdt_migration.log
```

Leave this running in the background to capture all migration logs.

---

### Step 3: Run Migration from App

1. **Open SSBMax app** on your device
2. **Navigate to**: Settings (gear icon in top-right or drawer)
3. **Scroll down** to "Developer Options" section
4. **Find button**: "Migrate PPDT to Firestore"
5. **Tap the button**
6. **Wait** for success dialog (typically 2-3 seconds)

**Expected Dialog**:
```
✓ Success
PPDT content migrated successfully!
12 materials uploaded to Firestore.
```

**Expected Logs** (in terminal):
```
10-29 XX:XX:XX.XXX D MigrateUseCase: Starting migration for PPDT
10-29 XX:XX:XX.XXX D MigrateUseCase: Normalized topic ID: PPDT
10-29 XX:XX:XX.XXX D MigrateUseCase: Loading local content for PPDT
10-29 XX:XX:XX.XXX D MigrateUseCase: ✓ Uploading topic document to Firestore
10-29 XX:XX:XX.XXX D MigrateUseCase: ✓ Topic document uploaded successfully
10-29 XX:XX:XX.XXX D MigrateUseCase: ✓ Uploading 12 materials...
10-29 XX:XX:XX.XXX D MigrateUseCase: ✓ All materials uploaded
10-29 XX:XX:XX.XXX D MigrateUseCase: ✓ Migration complete in 2XXXms
```

---

### Step 4: Verify in Firebase Console

1. **Open**: https://console.firebase.google.com/project/ssbmax-49e68/firestore
2. **Navigate to**: `topic_content` collection
3. **Check**: Document ID = `PPDT` exists
4. **Verify fields**:
   - `topicType`: "PPDT"
   - `title`: "Picture Perception & Description Test"
   - `introduction`: [Full text]
   - `materialsCount`: 12 (or similar)

5. **Navigate to**: `study_materials` collection
6. **Add filter**: `topicType == PPDT`
7. **Count materials**: Should show 12+ documents

**Visual Verification**:
- Each material should have:
  - `id`: "ppdt_1", "ppdt_2", etc.
  - `title`: Meaningful titles
  - `content`: Full markdown content
  - `topicType`: "PPDT" (uppercase)
  - `displayOrder`: 0, 1, 2, etc.

---

### Step 5: Test Cloud Loading

1. **In the app**, navigate back to home
2. **Go to**: Phase 1 → Screening Tests
3. **Tap on**: PPDT card
4. **Watch logs** (in your terminal):

**Expected Logs**:
```
10-29 XX:XX:XX.XXX D TopicViewModel: Loading topic: ppdt
10-29 XX:XX:XX.XXX D TopicViewModel: isTopicCloudEnabled(ppdt): true
10-29 XX:XX:XX.XXX D TopicViewModel: ✓ Loading ppdt from CLOUD (Firestore)
10-29 XX:XX:XX.XXX D FirestoreContentSource: Fetching topic from Firestore: PPDT
10-29 XX:XX:XX.XXX D FirestoreContentSource: ✓ Topic loaded: 12 materials
10-29 XX:XX:XX.XXX D TopicViewModel: ✓ Loaded ppdt from cloud: 12 materials
```

**UI Verification**:
- Topic title displays: "Picture Perception & Description Test"
- Introduction text shows (full PPDT overview)
- Material count shows: "12 materials" (not 6!)
- All materials are clickable and load content

---

### Step 6: Test Offline Caching

**Disable Network**:
```bash
# In a new terminal
adb shell svc wifi disable
adb shell svc data disable
```

**In App**:
1. Force close SSBMax
2. Reopen app
3. Navigate to PPDT topic again

**Expected Behavior**:
- Content still loads (from Firestore offline cache)
- No loading errors
- All 12 materials accessible

**Re-enable Network**:
```bash
adb shell svc wifi enable
adb shell svc data enable
```

---

### Step 7: Test Fallback Mechanism (Optional)

To verify the fallback to local content works:

1. **Edit**: `ContentFeatureFlags.kt`
2. **Change**:
   ```kotlin
   private val topicFlags = mutableMapOf<String, Boolean>(
       "OIR" to true,
       "PPDT" to false  // ← Disable PPDT
   )
   ```
3. **Rebuild**: `./gradle.sh assembleDebug`
4. **Install**: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
5. **Test**: Navigate to PPDT → Should show 6 materials (local fallback)
6. **Re-enable**: Change back to `true` and rebuild

---

## 📊 Success Checklist

After completing all steps, verify:

- [ ] APK installed successfully
- [ ] Migration button executed without errors
- [ ] Firebase Console shows `topic_content/PPDT` document
- [ ] Firebase Console shows 12+ materials with `topicType == PPDT`
- [ ] App loads PPDT with 12 materials (proves cloud source)
- [ ] Logs show "Loading from CLOUD"
- [ ] Offline mode works (cached content)
- [ ] Fallback to local works (when disabled)

---

## 🐛 Troubleshooting

### Issue: Migration Button Shows Error

**Possible Causes**:
1. User not authenticated to Firebase
2. Network connection issue
3. Firestore security rules blocking write

**Solutions**:
```bash
# Check auth state
adb logcat -s AuthRepository:D | grep "currentUser"

# Check network
adb shell ping -c 3 8.8.8.8

# Verify security rules deployed
firebase deploy --only firestore:rules
```

---

### Issue: Shows 6 Materials (Not 12)

**Cause**: Still loading from local, not cloud

**Solutions**:
1. Check feature flag: `adb logcat -s TopicViewModel:D | grep "isTopicCloudEnabled"`
   - Should show: `isTopicCloudEnabled(ppdt): true`
2. Check migration succeeded in Firebase Console
3. Clear app cache: `adb shell pm clear com.ssbmax`
4. Restart app

---

### Issue: Offline Mode Doesn't Work

**Cause**: Firestore offline persistence not enabled

**Solution**: Already enabled in code, but verify:
```kotlin
// In FirestoreContentSource.kt
ContentFeatureFlags.enableOfflinePersistence = true  // Should be true
```

---

## 📝 After Migration

### Update Material Count Reference

Edit `MIGRATION_QUICK_REFERENCE.md`:
```markdown
| Topic | Local (Hardcoded) | Expected Firestore | Status |
|-------|-------------------|--------------------|--------|
| OIR | 7 | 14 | ✅ Migrated |
| PPDT | 6 | 12 | ✅ Migrated |  ← Update this line
```

### Document Results

Create final summary:
```markdown
# PPDT Migration - Final Results

**Date**: October 29, 2025
**Status**: ✅ Success
**Duration**: X minutes
**Materials Migrated**: 12

## Verification
- Firebase Console: ✅ Verified
- Cloud Loading: ✅ Working
- Offline Cache: ✅ Working
- Fallback: ✅ Working

## Logs Saved
- Migration logs: `ppdt_migration.log`
- Full log archive: Available on request
```

---

## 🎯 Next Topic After PPDT

After successful PPDT migration, proceed to:

**Recommended Order**:
1. ✅ OIR (Complete)
2. ✅ PPDT (In Progress)
3. 🔜 PSYCHOLOGY (8 materials) - High usage, important
4. 🔜 PIQ_FORM (3 materials) - Quick win
5. 🔜 GTO (7 materials)
6. 🔜 INTERVIEW (7 materials)
7. 🔜 CONFERENCE (4 materials)
8. 🔜 MEDICALS (5 materials)
9. 🔜 SSB_OVERVIEW (4 materials)

---

## 📞 Need Help?

- **Full Playbook**: `FIRESTORE_MIGRATION_PLAYBOOK.md`
- **Quick Reference**: `MIGRATION_QUICK_REFERENCE.md`
- **OIR Success Story**: `CLOUD_CONTENT_SUCCESS.md`

---

**Last Updated**: October 29, 2025 - Build Complete, Ready for Migration

