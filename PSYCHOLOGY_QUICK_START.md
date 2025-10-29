# Psychology Migration - Quick Start Guide

**⏱️ Time Required**: 10 minutes  
**Status**: Ready to migrate

---

## 🚀 Quick Migration (5 Commands)

```bash
# 1. Build
./gradle.sh assembleDebug

# 2. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Start logging (optional, in separate terminal)
adb logcat -s PsychologyMigration:D TopicViewModel:D -v time > psychology.log

# 4. Open app → Settings → Developer Options → Tap "Migrate Psychology to Firestore"

# 5. Verify in Firebase Console
# https://console.firebase.google.com/project/ssbmax-49e68/firestore
```

---

## ✅ Expected Results

**Migration Dialog**:
```
✓ Psychology migration successful!
Migrated 8/8 materials

Topic: ✓ Migrated
Materials: 8/8
Duration: ~2000ms
```

**Firebase Console**:
- `topic_content/PSYCHOLOGY` document exists ✓
- 8 materials with `topicType == PSYCHOLOGY` ✓

**App Behavior**:
- Navigate to Psychology topic
- Shows 8 materials (not 7 from local)
- Content loads from Firestore
- Works offline after first load

---

## 🎯 What Changed

### Files Created (1):
- `MigratePsychologyUseCase.kt` - Migration logic

### Files Modified (3):
- `SettingsViewModel.kt` - Added migration function
- `SettingsScreen.kt` - Added migration button & dialog
- `ContentFeatureFlags.kt` - Enabled PSYCHOLOGY flag

---

## 📊 Progress Update

**Before**: 2/9 topics (OIR, PPDT)  
**After**: 3/9 topics (OIR, PPDT, Psychology)  

**Materials**: 21/51 (41%)

---

## 🔜 Next: PIQ_FORM

**Why**: Only 3 materials, quick win!  
**Time**: 15 minutes  
**Pattern**: Same as Psychology

---

**Documentation**: See `PSYCHOLOGY_MIGRATION_COMPLETE.md` for full details

