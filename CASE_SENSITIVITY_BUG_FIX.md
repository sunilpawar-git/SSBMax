# Case Sensitivity Bug Fix

## 🐛 The Real Bug Found!

### Evidence from Logs
```
Feature Flag Check for: oir         ← LOWERCASE from navigation!
isTopicCloudEnabled(oir): false     ← Lookup failed

BUT:

Enabled Topics: OIR                 ← UPPERCASE in feature flags map!
```

### Root Cause
**Case sensitivity mismatch** between navigation and feature flags:

1. **Navigation passes lowercase**:
   ```kotlin
   // PhaseProgressRibbon.kt
   onTopicClick("oir")  ← lowercase
   
   // DrawerContent.kt
   onNavigateToTopic("oir")  ← lowercase
   ```

2. **Feature flags store uppercase**:
   ```kotlin
   // ContentFeatureFlags.kt
   private val topicFlags = mutableMapOf("OIR" to true)  ← uppercase
   ```

3. **Map lookup fails**:
   ```kotlin
   topicFlags["oir"]  → returns null (not found)
   topicFlags["OIR"]  → returns true (found!)
   ```

---

## ✅ Fix Applied

Made feature flag lookups **case-insensitive** by normalizing to uppercase:

### File: `ContentFeatureFlags.kt`

```kotlin
fun isTopicCloudEnabled(topicType: String): Boolean {
    if (!useCloudContent) return false
    // Normalize to uppercase for consistent lookup
    return topicFlags[topicType.uppercase()] ?: false  // ← FIX
}

fun enableTopicCloud(topicType: String) {
    topicFlags[topicType.uppercase()] = true  // ← FIX
}

fun disableTopicCloud(topicType: String) {
    topicFlags[topicType.uppercase()] = false  // ← FIX
}
```

---

## 🧪 Testing

### Install Updated APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Expected Logs (SUCCESS)
```
TopicViewModel: Feature Flag Check for: oir
TopicViewModel: useCloudContent (master): true
TopicViewModel: isTopicCloudEnabled(oir): true  ← NOW TRUE!
TopicViewModel: Enabled Topics: OIR
TopicViewModel: ✓ Loading oir from CLOUD (Firestore)  ← SUCCESS!
TopicViewModel: ✓ Loaded oir from cloud: 7 materials
```

The key change: `isTopicCloudEnabled(oir): false` → `true`

---

## 🎯 Why This Happened

### Inconsistent Naming Convention
- **Navigation**: Uses lowercase route IDs (`"oir"`, `"ppdt"`)
- **Feature Flags**: Stored uppercase topic types (`"OIR"`, `"PPDT"`)
- **No normalization**: Direct map lookup failed silently

### Previous Attempts
1. ✅ Firestore working (health check passed)
2. ✅ Migration successful (OIR data exists)
3. ✅ Security rules correct (read/write allowed)
4. ✅ Feature flags enabled (master + topic flags set)
5. ❌ **Case mismatch** (overlooked until detailed logs)

---

## 📝 Lessons Learned

### Debug Strategy That Worked
Adding comprehensive logging revealed the exact issue:
```kotlin
Log.d(TAG, "Feature Flag Check for: $topicType")  // Showed "oir"
Log.d(TAG, "Enabled Topics: ${topicFlags.keys}")   // Showed "OIR"
```

The mismatch was immediately visible!

### Best Practice Going Forward
**Always normalize string keys** in maps/lookups:
- Use `.uppercase()` or `.lowercase()` consistently
- Store keys in one case, lookup in same case
- Add validation/tests for case sensitivity

---

## 🚀 Expected Behavior After Fix

1. **Navigate to OIR** (with lowercase `"oir"`)
2. **Feature flag lookup** normalizes to `"OIR"`
3. **Map returns** `true`
4. **Loads from Firestore** ✓

---

## 📊 Build Info

**APK**: `app/build/outputs/apk/debug/app-debug.apk`  
**Build Time**: 6 seconds  
**Status**: ✅ BUILD SUCCESSFUL

---

## ✅ Verification Steps

1. Install updated APK
2. Open SSBMax (fresh start)
3. Navigate to OIR
4. Check logcat for:
   - `isTopicCloudEnabled(oir): true` (was false)
   - `✓ Loading oir from CLOUD`
   - `✓ Loaded oir from cloud: 7 materials`

---

**This should finally work!** 🎉

The bug was subtle but the logs made it obvious once we added detailed debugging.

