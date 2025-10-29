# PPDT Migration - Quick Start (5 Minutes)

**Status**: ✅ Ready to Execute  
**Prerequisites**: ✅ All Complete

---

## ⚡ Quick Commands

### 1. Install APK (30 seconds)
```bash
adb install -r /Users/sunil/Downloads/SSBMax/app/build/outputs/apk/debug/app-debug.apk
```

### 2. Start Logging (keep running)
```bash
adb logcat -s TopicViewModel:D StudyContentRepo:D MigrateUseCase:D -v time > ppdt_migration.log
```

### 3. In App (2 minutes)
- Open SSBMax
- Settings → Developer Options
- Tap "Migrate PPDT to Firestore"
- Wait for success dialog

### 4. Verify (2 minutes)
- **Firebase Console**: https://console.firebase.google.com/project/ssbmax-49e68/firestore
  - Check `topic_content/PPDT` exists
  - Check `study_materials` filtered by `topicType == PPDT` (should be 12+)
- **In App**: Navigate to PPDT topic
  - Should show "12 materials" (not 6)
  - Logs should show "Loading from CLOUD"

---

## ✅ Success Indicators

### Logs Show:
```
✓ Migration complete in ~2000ms
✓ isTopicCloudEnabled(ppdt): true
✓ Loading ppdt from CLOUD
✓ Loaded ppdt from cloud: 12 materials
```

### UI Shows:
- Material count: **12 materials** (not 6)
- Full introduction text
- All materials load correctly

### Firebase Console Shows:
- Document `topic_content/PPDT` exists
- 12+ materials with `topicType == PPDT`

---

## 🎉 After Success

Update material count in `MIGRATION_QUICK_REFERENCE.md`:
```markdown
| PPDT | 6 | 12 | ✅ Migrated |
```

Then proceed to next topic: **PSYCHOLOGY** (8 materials)

---

## 📞 Issues?

See detailed troubleshooting in `PPDT_MIGRATION_INSTRUCTIONS.md`

**Common Fix**: Clear app cache
```bash
adb shell pm clear com.ssbmax
```

---

**Ready?** Follow the 4 steps above! 🚀

