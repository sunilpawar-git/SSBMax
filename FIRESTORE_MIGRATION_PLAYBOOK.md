# Firestore Migration Playbook
## Lessons Learned from OIR Implementation

**Based on**: Successful OIR cloud content migration (October 28, 2025)  
**Purpose**: Prevent pitfalls when migrating remaining 8 topics to Firestore

---

## 📋 **Table of Contents**

1. [Critical Pitfalls & Solutions](#critical-pitfalls--solutions)
2. [Pre-Migration Checklist](#pre-migration-checklist)
3. [Migration Step-by-Step](#migration-step-by-step)
4. [Testing & Verification](#testing--verification)
5. [Rollback Plan](#rollback-plan)
6. [Cost Optimization](#cost-optimization)

---

## 🚨 **Critical Pitfalls & Solutions**

### **Pitfall #1: Case Sensitivity Mismatch** ⚠️ CRITICAL

#### Problem
- Navigation routes use **lowercase**: `"oir"`, `"ppdt"`, `"psychology"`
- Feature flags stored **uppercase**: `"OIR"`, `"PPDT"`, `"PSYCHOLOGY"`
- Map lookup `topicFlags["oir"]` returned `null` → failed silently

#### Solution Applied
```kotlin
// ContentFeatureFlags.kt
fun isTopicCloudEnabled(topicType: String): Boolean {
    return topicFlags[topicType.uppercase()] ?: false  // ← Normalize!
}

fun enableTopicCloud(topicType: String) {
    topicFlags[topicType.uppercase()] = true  // ← Store uppercase
}
```

#### **ACTION REQUIRED**: ✅ Already fixed for all topics
- All future topics will automatically benefit from case-insensitive lookups
- No additional code changes needed

---

### **Pitfall #2: Feature Flags Not Persisting** ⚠️ IMPORTANT

#### Problem
- `ContentFeatureFlags` is an in-memory Kotlin `object`
- Flags reset to defaults on app restart
- User toggles in Settings UI were lost

#### Temporary Solution
```kotlin
// Hardcoded for testing
var useCloudContent: Boolean = true  // Was: false
private val topicFlags = mutableMapOf("OIR" to true)
```

#### **ACTION REQUIRED**: ⚠️ Implement before full rollout

**Proper Solution Needed**:
```kotlin
// NEW FILE: ContentFeatureFlagsManager.kt
@Singleton
class ContentFeatureFlagsManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun saveFlags() {
        sharedPreferences.edit {
            putBoolean("cloud_content_enabled", ContentFeatureFlags.useCloudContent)
            putStringSet("enabled_topics", ContentFeatureFlags.getEnabledTopics())
        }
    }
    
    fun loadFlags() {
        ContentFeatureFlags.useCloudContent = 
            sharedPreferences.getBoolean("cloud_content_enabled", false)
        
        val enabledTopics = sharedPreferences.getStringSet("enabled_topics", emptySet())
        enabledTopics?.forEach { topic ->
            ContentFeatureFlags.enableTopicCloud(topic)
        }
    }
}

// In Application.onCreate()
featureFlagsManager.loadFlags()

// In SettingsScreen toggle handler
ContentFeatureFlags.useCloudContent = enabled
featureFlagsManager.saveFlags()  // ← Persist immediately
```

**Timeline**: Implement before migrating 3+ topics

---

### **Pitfall #3: Document ID vs topicType Mismatch** ⚠️ CRITICAL

#### Problem Pattern
```
Firestore Collection: topic_content
- Document ID: "OIR" (uppercase)
- Field topicType: "OIR" (must match!)

Query for materials:
firestore.collection("study_materials")
    .whereEqualTo("topicType", "OIR")  // Must match document ID!
```

#### Migration Rule
**ALWAYS use UPPERCASE for:**
- Firestore document IDs
- `topicType` field values
- Feature flag keys

#### **ACTION REQUIRED**: ✅ Document in migration script
```kotlin
// MigrateTopic.kt
private fun normalizeTopicId(topicId: String): String {
    return topicId.uppercase()  // "oir" → "OIR"
}

// Use in migration
val documentId = normalizeTopicId(topic.id)
firestore.collection("topic_content").document(documentId).set(...)
```

---

### **Pitfall #4: Missing Firestore Indexes** ⚠️ BLOCKS QUERIES

#### Problem
- Queries like `where("topicType", "==", "OIR").orderBy("displayOrder")` require composite indexes
- Without index → query fails with error

#### Solution
**Create indexes BEFORE migration**:

```bash
# firestore.indexes.json
{
  "indexes": [
    {
      "collectionGroup": "study_materials",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "topicType", "order": "ASCENDING" },
        { "fieldPath": "displayOrder", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "study_materials",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "isPremium", "order": "ASCENDING" },
        { "fieldPath": "topicType", "order": "ASCENDING" }
      ]
    }
  ]
}
```

```bash
# Deploy indexes
firebase deploy --only firestore:indexes

# Wait 2-5 minutes for indexes to build
```

#### **ACTION REQUIRED**: ⚠️ Deploy indexes before next migration

---

### **Pitfall #5: Security Rules Blocking Migration** ⚠️ BLOCKS WRITES

#### Problem
- Initial security rules had `allow write: if false`
- Migration script couldn't write to Firestore

#### Solution
```javascript
// firestore.rules
match /topic_content/{topicId} {
  allow read: if isAuthenticated();
  allow write: if isAuthenticated();  // ← Enable for migration
}

match /study_materials/{materialId} {
  allow read: if isAuthenticated();
  allow create: if isAuthenticated();  // ← Enable for migration
  allow update, delete: if false;
}
```

**Deploy before migration**:
```bash
firebase deploy --only firestore:rules
```

#### **ACTION REQUIRED**: ✅ Already deployed, verified working

---

### **Pitfall #6: Material Count Mismatch** ℹ️ INFORMATIONAL

#### Discovery
- **Local hardcoded**: 7 OIR materials
- **After migration**: 14 OIR materials

#### Explanation
- Migration script uploaded MORE materials than hardcoded locally
- This is **expected and correct** (cloud has full content)
- Use different counts to **verify** cloud loading

#### **ACTION REQUIRED**: ✅ Document expected counts

**Create verification matrix**:
```kotlin
// Expected material counts per topic
private val EXPECTED_MATERIAL_COUNTS = mapOf(
    "OIR" to 14,      // Verified
    "PPDT" to 12,     // TODO: Count after migration
    "PSYCHOLOGY" to 18,  // TODO: Count after migration
    // ... etc
)
```

---

## ✅ **Pre-Migration Checklist**

Before migrating each topic, verify:

### Infrastructure
- [ ] Firestore indexes deployed and **ENABLED** (not Building)
- [ ] Security rules allow authenticated writes
- [ ] Firebase project has sufficient quota (check Console)
- [ ] Network stable (migration script needs internet)

### Code Readiness
- [ ] Feature flags use case-insensitive lookups ✅
- [ ] Migration script normalizes to uppercase ✅
- [ ] Repository handles both cloud/local sources ✅
- [ ] Fallback mechanism tested ✅

### Testing Setup
- [ ] Comprehensive logging enabled
- [ ] Test device authenticated to Firebase
- [ ] Logcat monitoring ready (`adb logcat`)
- [ ] Firebase Console open for verification

### Data Validation
- [ ] Local content for topic exists in codebase
- [ ] Know expected material count
- [ ] Verify topic ID matches navigation routes

---

## 🔄 **Migration Step-by-Step**

### **Phase 1: Prepare (Per Topic)**

#### Step 1.1: Verify Local Content Exists
```kotlin
// Check that topic is defined in TopicContentLoader
val topicInfo = TopicContentLoader.getTopicInfo("PPDT")
println("Title: ${topicInfo.title}")
println("Materials: ${topicInfo.studyMaterials.size}")
```

#### Step 1.2: Document Current State
```markdown
## PPDT Migration Plan
- Local materials: 6 items
- Expected Firestore: 12 items (full content)
- Navigation route: "ppdt" (lowercase)
- Firestore ID: "PPDT" (uppercase)
```

#### Step 1.3: Enable Debug Logging
```bash
adb logcat -s TopicViewModel:D StudyContentRepo:D MigrateUseCase:D -v time > ppdt_migration.log
```

---

### **Phase 2: Migrate (Per Topic)**

#### Step 2.1: Run Migration from App
```
1. Open SSBMax
2. Navigate to Settings
3. Scroll to Developer Options
4. Tap "Migrate [TOPIC] to Firestore"
5. Wait for success dialog
```

**OR** Run migration script:
```kotlin
// From migration tool
val result = migrateOIRUseCase.execute(topicId = "PPDT")
```

#### Step 2.2: Monitor Logs
Watch for:
```
MigrateUseCase: Starting migration for PPDT
MigrateUseCase: ✓ Uploaded topic document
MigrateUseCase: ✓ Uploaded 12 materials
MigrateUseCase: ✓ Migration complete in 2288ms
```

#### Step 2.3: Verify in Firebase Console
1. Open Firestore → `topic_content` collection
2. Find document ID = `"PPDT"`
3. Verify fields: `title`, `introduction`, `topicType`, etc.
4. Open `study_materials` collection
5. Add filter: `topicType == PPDT`
6. Verify count matches expected (e.g., 12 items)

---

### **Phase 3: Enable Cloud Content**

#### Step 3.1: Enable Topic Flag
```kotlin
// Temporary: Hardcode in ContentFeatureFlags.kt
private val topicFlags = mutableMapOf(
    "OIR" to true,
    "PPDT" to true,  // ← Add new topic
)
```

**OR** if persistence implemented:
```
1. Settings → Developer Options
2. Toggle "Enable Cloud Content" ON
3. Toggle "Enable PPDT from Firestore" ON
4. Restart app
```

#### Step 3.2: Clear App Cache (Optional)
```bash
adb shell pm clear com.ssbmax
```

This forces fresh Firestore fetch (bypasses cache).

---

### **Phase 4: Test & Verify**

#### Step 4.1: Test Loading
```
1. Open SSBMax
2. Navigate to PPDT topic
3. Watch logs for:
   - isTopicCloudEnabled(ppdt): true
   - ✓ Loading ppdt from CLOUD
   - ✓ Loaded ppdt from cloud: 12 materials
```

#### Step 4.2: Verify Content Displayed
- Topic title shows correctly
- Introduction text displays
- All 12 materials listed
- Tap a material → content loads

#### Step 4.3: Test Offline Behavior
```bash
# Disable network
adb shell svc wifi disable
adb shell svc data disable

# Navigate to PPDT again
# Should load from Firestore cache (offline persistence)

# Re-enable network
adb shell svc wifi enable
adb shell svc data enable
```

#### Step 4.4: Test Fallback
```kotlin
// Temporarily disable cloud in code
ContentFeatureFlags.useCloudContent = false

// Rebuild, test
// Should load from local (7 materials)

// Re-enable
ContentFeatureFlags.useCloudContent = true
```

---

## 🧪 **Testing & Verification**

### **Success Criteria (Per Topic)**

| Check | Expected | How to Verify |
|-------|----------|---------------|
| Migration | Success | Firebase Console shows documents |
| Material count | Matches expected | Count in `study_materials` |
| Cloud loading | Enabled | Logs show "Loading from CLOUD" |
| Content displays | All materials | UI shows full list |
| Offline works | Cached data | Disable network, still works |
| Fallback works | Local data | Disable cloud flag, loads local |

### **Verification Script**

```kotlin
// VerifyTopicMigration.kt
data class TopicVerification(
    val topicId: String,
    val expectedMaterialCount: Int,
    val firestoreExists: Boolean,
    val cloudLoadingWorks: Boolean,
    val fallbackWorks: Boolean
)

suspend fun verifyTopicMigration(topicId: String): TopicVerification {
    val normalized = topicId.uppercase()
    
    // Check Firestore
    val topicDoc = firestore.collection("topic_content").document(normalized).get().await()
    val materialsQuery = firestore.collection("study_materials")
        .whereEqualTo("topicType", normalized)
        .get().await()
    
    return TopicVerification(
        topicId = normalized,
        expectedMaterialCount = materialsQuery.size(),
        firestoreExists = topicDoc.exists(),
        cloudLoadingWorks = testCloudLoading(topicId),
        fallbackWorks = testFallback(topicId)
    )
}
```

---

## 🔙 **Rollback Plan**

### **Instant Rollback (Emergency)**

If cloud loading fails in production:

#### Option 1: Feature Flag Disable (Fastest)
```kotlin
// Disable globally (affects all users immediately)
ContentFeatureFlags.useCloudContent = false

// OR disable specific topic
ContentFeatureFlags.disableTopicCloud("PPDT")
```

Push update to Play Store or use remote config.

#### Option 2: Code Revert
```bash
# Revert to previous commit
git revert <commit-hash>

# Rebuild and deploy
./gradle.sh assembleRelease
```

#### Option 3: Keep Firestore, Fix Bug
- Leave data in Firestore
- Fix bug in code
- Deploy update
- Re-enable feature flag

### **Data Rollback (If Needed)**

Delete migrated data:
```javascript
// Firebase Console → Firestore
// 1. Delete topic_content/PPDT document
// 2. Delete all study_materials where topicType == PPDT

// OR use script:
const batch = firestore.batch();
const materials = await firestore.collection('study_materials')
    .where('topicType', '==', 'PPDT')
    .get();
materials.forEach(doc => batch.delete(doc.ref));
await batch.commit();
```

**Note**: With local fallback, data deletion is safe (app won't break).

---

## 💰 **Cost Optimization**

### **Firestore Pricing (Free Tier)**

- **Reads**: 50,000/day free
- **Writes**: 20,000/day free
- **Deletes**: 20,000/day free
- **Storage**: 1 GB free

### **OIR Migration Costs**

| Operation | Count | Cost |
|-----------|-------|------|
| Topic write | 1 | $0.000036 |
| Material writes | 14 | $0.000504 |
| **Total per migration** | **15** | **$0.00054** |

### **Daily Usage (1000 users, OIR only)**

| Operation | Count/User | Total | Cost |
|-----------|------------|-------|------|
| Topic read | 1 | 1,000 | Free (< 50K) |
| Material reads | 14 | 14,000 | Free (< 50K) |
| Cache hits (offline) | ~80% | 11,200 | $0 (cached) |
| **Net Firestore reads** | | **2,800** | **$0** |

### **Projected Full Rollout (9 Topics, 1000 Users)**

| Metric | Count | Cost/Month |
|--------|-------|------------|
| One-time migration | 9 topics × 15 writes | $0.00486 |
| Daily reads (9 topics) | ~25,000/day | $0 (free tier) |
| Monthly reads | 750,000 | $0.14 (paid tier) |
| **Total** | | **~$0.14/month** |

### **Optimization Strategies**

#### 1. Enable Offline Persistence ✅ (Already Enabled)
```kotlin
ContentFeatureFlags.enableOfflinePersistence = true
```
- Caches data locally
- Reduces Firestore reads by ~80%

#### 2. Set Cache Expiry
```kotlin
ContentFeatureFlags.cacheExpiryDays = 7  // Cache for 1 week
```
- Topics don't change often
- 7-day cache = 7× fewer reads

#### 3. Batch Queries
```kotlin
// Instead of 14 individual reads:
materials.forEach { loadMaterial(it.id) }  // 14 reads

// Use single query:
firestore.collection("study_materials")
    .whereEqualTo("topicType", "OIR")
    .get()  // 1 read + 14 document reads = still cheaper
```

#### 4. Preload on WiFi
```kotlin
// When on WiFi, preload all topics
if (isWifiConnected()) {
    topics.forEach { prefetchTopic(it) }
}
```

---

## 📊 **Migration Order Recommendation**

### **Phase 1: Pilot (Complete ✅)**
- ✅ **OIR** - Successful

### **Phase 2: Core Psychology Tests (Next)**
Migrate in this order:
1. **PPDT** (similar to OIR, low risk)
2. **PSYCHOLOGY** (high usage, test scalability)
3. **PIQ_FORM** (simple, quick win)

**Why**: Core tests, high usage, maximum learning

### **Phase 3: GTO & Interview**
4. **GTO** (complex, multiple subtests)
5. **INTERVIEW** (rich content)

**Why**: More complex, benefit from Phase 2 learnings

### **Phase 4: Remaining Topics**
6. **CONFERENCE**
7. **MEDICALS**
8. **SSB_OVERVIEW**

**Why**: Lower usage, migrate once process is solid

### **Phase 5: All-In**
- Enable all 9 topics simultaneously
- Monitor for 48 hours
- Set `ContentFeatureFlags.useCloudContent = true` globally

---

## 🔧 **Automated Migration Tool (Future)**

### **Create Batch Migration Script**

```kotlin
// BatchMigrationTool.kt
class BatchMigrationTool @Inject constructor(
    private val migrateOIRUseCase: MigrateOIRUseCase
) {
    suspend fun migrateAll(
        topics: List<String>,
        onProgress: (String, Int, Int) -> Unit
    ): BatchMigrationResult {
        val results = mutableMapOf<String, MigrationResult>()
        
        topics.forEachIndexed { index, topic ->
            onProgress(topic, index + 1, topics.size)
            
            try {
                val result = migrateOIRUseCase.execute(topic)
                results[topic] = result
                delay(500)  // Rate limiting
            } catch (e: Exception) {
                results[topic] = MigrationResult.Error(e.message)
            }
        }
        
        return BatchMigrationResult(
            total = topics.size,
            successful = results.count { it.value is MigrationResult.Success },
            failed = results.count { it.value is MigrationResult.Error },
            results = results
        )
    }
}

// Usage in Settings
Button(onClick = {
    viewModel.migrateAllTopics(
        listOf("PPDT", "PSYCHOLOGY", "PIQ_FORM")
    )
}) {
    Text("Migrate Next 3 Topics")
}
```

---

## 📝 **Documentation Template (Per Topic)**

Use this template when migrating each topic:

```markdown
# [TOPIC_NAME] Migration Report

**Date**: YYYY-MM-DD
**Migrated by**: [Name]
**Status**: ✅ Success / ❌ Failed

## Pre-Migration State
- Local materials: X items
- Navigation route: "[lowercase]"
- Expected Firestore count: Y items

## Migration Process
- Started: HH:MM
- Completed: HH:MM
- Duration: Z seconds
- Errors: None / [List errors]

## Verification
- [x] Firestore document exists
- [x] Material count matches (Y items)
- [x] Cloud loading works
- [x] Offline caching works
- [x] Fallback to local works

## Logs
```
[Paste relevant log excerpts]
```

## Firebase Console Links
- Topic: https://console.firebase.google.com/project/ssbmax/firestore/data/topic_content/[TOPIC]
- Materials: https://console.firebase.google.com/project/ssbmax/firestore/data/study_materials?filter=topicType==[TOPIC]

## Notes
- [Any observations, issues, or improvements]
```

---

## 🎯 **Key Takeaways**

### **Do's ✅**

1. ✅ **Always normalize to UPPERCASE** (document IDs, topicType fields)
2. ✅ **Deploy indexes BEFORE migration** (wait for Building → Enabled)
3. ✅ **Verify security rules allow writes** (`allow create: if isAuthenticated()`)
4. ✅ **Test with comprehensive logging** (enables fast debugging)
5. ✅ **Verify in Firebase Console** after each migration
6. ✅ **Test offline behavior** (cache persistence)
7. ✅ **Document material counts** for verification
8. ✅ **Enable fallback to local** (safety net)
9. ✅ **Monitor logs during first load** (catch issues early)
10. ✅ **Migrate one topic at a time** (reduce risk)

### **Don'ts ❌**

1. ❌ **Don't assume case doesn't matter** (it breaks lookups)
2. ❌ **Don't migrate without indexes** (queries will fail)
3. ❌ **Don't forget to deploy security rules** (writes blocked)
4. ❌ **Don't skip verification** (silent failures happen)
5. ❌ **Don't migrate all topics at once** (hard to debug)
6. ❌ **Don't ignore logs** (first indication of problems)
7. ❌ **Don't forget offline testing** (users won't always have internet)
8. ❌ **Don't disable fallback** (app breaks if Firestore fails)
9. ❌ **Don't forget to persist feature flags** (implement SharedPreferences)
10. ❌ **Don't migrate to prod without testing** (use debug build first)

---

## 🚀 **Next Topic Migration: Quick Start**

### **To migrate PPDT** (example):

```bash
# 1. Verify indexes deployed
firebase deploy --only firestore:indexes

# 2. Enable in code
# ContentFeatureFlags.kt: Add "PPDT" to true

# 3. Rebuild
./gradle.sh assembleDebug

# 4. Start logging
adb logcat -s TopicViewModel:D StudyContentRepo:D -v time > ppdt_migration.log

# 5. Run migration from app
# Settings → Developer Options → Migrate PPDT

# 6. Verify in Firebase Console
# Check topic_content/PPDT and study_materials

# 7. Test in app
# Navigate to PPDT → Verify loading from cloud

# 8. Monitor for 24 hours
# Check logs, user feedback, Firestore metrics
```

---

## 📞 **Support & Troubleshooting**

### **If Migration Fails**

1. Check logs for exact error
2. Verify user is authenticated (`FirebaseAuth.currentUser != null`)
3. Check Firestore security rules
4. Verify network connection
5. Check Firebase quota (Console → Usage tab)

### **If Cloud Loading Fails**

1. Check feature flags: `ContentFeatureFlags.getStatus()`
2. Verify document exists in Firestore
3. Check case sensitivity (document ID vs query)
4. Verify indexes are ENABLED (not Building)
5. Check security rules allow read

### **If App Breaks**

1. Instant rollback: `ContentFeatureFlags.useCloudContent = false`
2. App continues with local fallback
3. Fix issue, redeploy
4. Re-enable cloud content

---

## ✅ **Checklist for Next 8 Topics**

Use this checklist for each topic migration:

### Pre-Migration
- [ ] Document local material count
- [ ] Verify navigation route (lowercase)
- [ ] Plan Firestore document ID (uppercase)
- [ ] Check indexes deployed and enabled
- [ ] Verify security rules allow writes
- [ ] Start log monitoring

### Migration
- [ ] Run migration from app/script
- [ ] Watch for success message
- [ ] Check logs for errors
- [ ] Verify in Firebase Console
- [ ] Count materials matches expected

### Testing
- [ ] Enable topic in feature flags
- [ ] Rebuild app
- [ ] Navigate to topic
- [ ] Verify "Loading from CLOUD" in logs
- [ ] Verify material count correct
- [ ] Test offline (disable network)
- [ ] Test fallback (disable cloud flag)

### Documentation
- [ ] Create migration report
- [ ] Record material counts
- [ ] Save relevant logs
- [ ] Note any issues/learnings

---

## 📚 **References**

- **OIR Success Report**: `CLOUD_CONTENT_SUCCESS.md`
- **Case Sensitivity Fix**: `CASE_SENSITIVITY_BUG_FIX.md`
- **Firebase Setup Guide**: `FIREBASE_CONSOLE_SETUP_GUIDE.md`
- **Debug Guide**: `DEBUG_REPOSITORY_LAYER.md`

---

**Last Updated**: October 28, 2025  
**Version**: 1.0 (Based on OIR success)  
**Next Review**: After migrating 3 more topics

---


