# Firestore Migration - Quick Reference Card

**Quick access guide for migrating remaining 8 topics**

---

## 🎯 **The 6 Critical Rules**

| Rule | Why | Impact if Ignored |
|------|-----|-------------------|
| 1. **UPPERCASE document IDs** | Navigation uses lowercase, Firestore uses uppercase | Silent lookup failures |
| 2. **Deploy indexes FIRST** | Queries need indexes | Migration fails |
| 3. **Enable write permissions** | Security rules block by default | Permission denied errors |
| 4. **Verify in Console** | Silent failures happen | Bad data in production |
| 5. **Test offline mode** | Users won't always have internet | Poor UX |
| 6. **Monitor logs** | First indicator of problems | Late detection |

---

## ⚡ **Quick Migration (5 Minutes)**

### For Any Topic (e.g., PPDT):

```bash
# 1. Deploy indexes (one-time, if not done)
firebase deploy --only firestore:indexes

# 2. Add to feature flags (ContentFeatureFlags.kt)
private val topicFlags = mutableMapOf(
    "OIR" to true,
    "PPDT" to true,  // ← Add new line
)

# 3. Rebuild
./gradle.sh assembleDebug

# 4. Install & Run migration
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Open app → Settings → Developer Options → Migrate PPDT

# 5. Verify
# Firebase Console → Check topic_content/PPDT exists
# Check study_materials count

# 6. Test
# Navigate to PPDT topic → Should show "14 materials" or similar
```

**Done!** ✅

---

## 🚨 **Troubleshooting (Common Issues)**

### Issue 1: "PERMISSION_DENIED" during migration
```bash
# Fix: Redeploy security rules
firebase deploy --only firestore:rules
```

### Issue 2: "Index required" error
```bash
# Fix: Deploy indexes and wait 2-5 minutes
firebase deploy --only firestore:indexes
# Check status in Firebase Console → Firestore → Indexes tab
```

### Issue 3: Still loading from local after migration
```
# Check 1: Feature flag enabled?
adb logcat -s TopicViewModel:D | grep "isTopicCloudEnabled"
# Should show: isTopicCloudEnabled(ppdt): true

# Check 2: Document exists?
# Firebase Console → topic_content → Find PPDT document

# Check 3: Case mismatch?
# Document ID must be UPPERCASE ("PPDT" not "ppdt")
```

### Issue 4: Material count is 0
```
# Check: Materials have correct topicType field
# Firebase Console → study_materials → Filter: topicType == PPDT
# Should show multiple documents

# Common mistake: topicType = "ppdt" (lowercase)
# Should be: topicType = "PPDT" (uppercase)
```

---

## 📊 **Verification Checklist**

After migrating each topic, verify:

| Check | Command/Action | Expected |
|-------|----------------|----------|
| **Feature flag** | `adb logcat -s TopicViewModel:D` | `isTopicCloudEnabled: true` |
| **Loading source** | Same as above | `✓ Loading from CLOUD` |
| **Material count** | Check logs or UI | Non-zero, matches migration |
| **Firestore doc** | Firebase Console | Document exists |
| **Materials** | Firebase Console filter | Count matches |
| **Offline works** | Disable WiFi, reopen topic | Loads from cache |

---

## 🎨 **Migration Order (Recommended)**

| Phase | Topics | Complexity | Timeline |
|-------|--------|------------|----------|
| ✅ **Done** | OIR | Low | Complete |
| 🚀 **Next** | PPDT, PSYCHOLOGY, PIQ_FORM | Low-Med | 1 day |
| ⏳ **Then** | GTO, INTERVIEW | Medium | 2 days |
| 📅 **Later** | CONFERENCE, MEDICALS, SSB_OVERVIEW | Low | 1 day |

**Total**: ~4 days to migrate all 9 topics

---

## 💾 **Template: Migration Command Sequence**

Copy-paste and replace `TOPIC_NAME`:

```bash
# Replace TOPIC_NAME with: PPDT, PSYCHOLOGY, PIQ_FORM, etc.

# 1. Add to ContentFeatureFlags.kt
# "TOPIC_NAME" to true,

# 2. Rebuild
./gradle.sh assembleDebug

# 3. Start logging
adb logcat -s TopicViewModel:D StudyContentRepo:D > TOPIC_NAME_migration.log

# 4. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. Run migration (from app Settings)

# 6. Verify in Firebase Console
# topic_content/TOPIC_NAME ✓
# study_materials where topicType == TOPIC_NAME ✓

# 7. Test in app
# Navigate to topic → Check logs for "Loading from CLOUD"
```

---

## 📝 **Material Count Reference**

| Topic | Local (Hardcoded) | Expected Firestore | Status |
|-------|-------------------|--------------------|--------|
| OIR | 7 | 14 | ✅ Migrated |
| PPDT | ? | ~12 | ⏳ Pending |
| PSYCHOLOGY | ? | ~18 | ⏳ Pending |
| PIQ_FORM | ? | ~8 | ⏳ Pending |
| GTO | ? | ~15 | ⏳ Pending |
| INTERVIEW | ? | ~20 | ⏳ Pending |
| CONFERENCE | ? | ~6 | ⏳ Pending |
| MEDICALS | ? | ~5 | ⏳ Pending |
| SSB_OVERVIEW | ? | ~10 | ⏳ Pending |

**Note**: Update this table as you migrate each topic

---

## 🔙 **Emergency Rollback**

If something breaks in production:

### Instant Fix (No rebuild needed)
```kotlin
// In ContentFeatureFlags.kt
var useCloudContent: Boolean = false  // ← Change to false

// OR disable specific topic
private val topicFlags = mutableMapOf(
    "OIR" to true,
    "PPDT" to false,  // ← Disable problematic topic
)
```

Rebuild and deploy. App falls back to local immediately.

---

## 📞 **Support Links**

- **Full Playbook**: `FIRESTORE_MIGRATION_PLAYBOOK.md` (comprehensive guide)
- **OIR Success Report**: `CLOUD_CONTENT_SUCCESS.md` (reference implementation)
- **Firebase Console**: https://console.firebase.google.com/project/ssbmax-49e68/firestore
- **Firestore Rules**: `firestore.rules` (security configuration)
- **Indexes**: `firestore.indexes.json` (query optimization)

---

## 🎯 **Success Metrics**

For each migrated topic, you should see:

```
✓ isTopicCloudEnabled(topic): true
✓ Loading topic from CLOUD (Firestore)
✓ Loaded topic from cloud: X materials
✓ Content displayed in UI
✓ Works offline (cached)
✓ Fallback to local works
```

If all 6 checks pass → **Migration successful!** 🎉

---

## 💡 **Pro Tips**

1. **Migrate during low traffic hours** (minimize user impact)
2. **Test on debug build first** (catch issues before release)
3. **Keep Firebase Console open** (real-time verification)
4. **Save logs for each migration** (troubleshooting reference)
5. **Document material counts** (verify cloud loading)
6. **One topic at a time** (easier to debug)
7. **Monitor for 24 hours** (catch delayed issues)

---

**Last Updated**: October 28, 2025  
**Based on**: Successful OIR migration  
**Confidence Level**: High (battle-tested)

---

