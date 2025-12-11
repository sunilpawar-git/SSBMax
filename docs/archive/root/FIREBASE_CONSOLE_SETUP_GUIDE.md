# Firebase Console Setup Guide

## Complete Step-by-Step Instructions for Cloud Content Architecture

**Prerequisites**:
- Firebase project already set up (you have this)
- Firebase CLI installed (for deploying rules)
- Authenticated user in the app (for testing)

---

## Step 1: Deploy Firestore Security Rules

Before creating collections, deploy the updated security rules:

```bash
cd /Users/sunil/Downloads/SSBMax

# Deploy only Firestore rules (safe, doesn't touch other services)
firebase deploy --only firestore:rules
```

**Expected Output**:
```
✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/YOUR_PROJECT/overview
```

**Troubleshooting**:
- If you get "Not authorized": Run `firebase login` first
- If wrong project: Run `firebase use YOUR_PROJECT_ID`

---

## Step 2: Create Firestore Collections Manually

### Why Manual Creation?
- Firestore creates collections automatically on first write
- BUT we need to add indexes and test documents first
- Manual creation ensures everything is set up correctly

### Open Firebase Console

1. Go to: https://console.firebase.google.com
2. Select your **SSBMax** project
3. Click **Firestore Database** in left sidebar
4. If you see "Create database", click it and choose:
   - **Production mode** (we have security rules)
   - **Location**: Choose closest to India (asia-south1)

---

## Step 3: Create `health_check` Collection

This is for testing Firebase connectivity.

### Steps:
1. In Firestore, click **Start collection**
2. Collection ID: `health_check`
3. Click **Next**
4. Add first document:
   - Document ID: `test`
   - Field 1:
     - Field name: `status`
     - Type: `string`
     - Value: `healthy`
   - Field 2:
     - Field name: `timestamp`
     - Type: `number`
     - Value: `1730102400000` (or any number)
5. Click **Save**

**Test**: Run health check from app (you'll add this in debug panel)

---

## Step 4: Create `content_versions` Collection

This tracks content versions for cache invalidation.

### Steps:
1. Click **Start collection**
2. Collection ID: `content_versions`
3. Click **Next**
4. Add first document:
   - Document ID: `global`
   - Field 1:
     - Field name: `topicsVersion`
     - Type: `number`
     - Value: `1`
   - Field 2:
     - Field name: `materialsVersion`
     - Type: `number`
     - Value: `1`
   - Field 3:
     - Field name: `lastUpdated`
     - Type: `number`
     - Value: `1730102400000`
5. Click **Save**

---

## Step 5: Create `topic_content` Collection

This will store topic introductions. We'll create ONE example, then use the migration tool for the rest.

### Steps:
1. Click **Start collection**
2. Collection ID: `topic_content`
3. Click **Next**
4. Add test document:
   - Document ID: `TEST`
   - Field 1:
     - Field name: `id`
     - Type: `string`
     - Value: `TEST`
   - Field 2:
     - Field name: `topicType`
     - Type: `string`
     - Value: `TEST`
   - Field 3:
     - Field name: `title`
     - Type: `string`
     - Value: `Test Topic`
   - Field 4:
     - Field name: `introduction`
     - Type: `string`
     - Value: `This is a test topic.`
   - Field 5:
     - Field name: `version`
     - Type: `number`
     - Value: `1`
   - Field 6:
     - Field name: `lastUpdated`
     - Type: `number`
     - Value: `1730102400000`
   - Field 7:
     - Field name: `isPremium`
     - Type: `boolean`
     - Value: `false`
5. Click **Save**

**Note**: This is just a test document. You'll delete it after migration.

---

## Step 6: Create `study_materials` Collection

This will store all study materials. Create ONE test document.

### Steps:
1. Click **Start collection**
2. Collection ID: `study_materials`
3. Click **Next**
4. Add test document:
   - Document ID: **Auto-ID** (let Firebase generate)
   - Field 1:
     - Field name: `id`
     - Type: `string`
     - Value: `test_1`
   - Field 2:
     - Field name: `topicType`
     - Type: `string`
     - Value: `TEST`
   - Field 3:
     - Field name: `title`
     - Type: `string`
     - Value: `Test Material`
   - Field 4:
     - Field name: `displayOrder`
     - Type: `number`
     - Value: `1`
   - Field 5:
     - Field name: `category`
     - Type: `string`
     - Value: `Test Category`
   - Field 6:
     - Field name: `contentMarkdown`
     - Type: `string`
     - Value: `# Test\n\nThis is test content.`
   - Field 7:
     - Field name: `author`
     - Type: `string`
     - Value: `Test Author`
   - Field 8:
     - Field name: `readTime`
     - Type: `string`
     - Value: `5 min read`
   - Field 9:
     - Field name: `isPremium`
     - Type: `boolean`
     - Value: `false`
   - Field 10:
     - Field name: `version`
     - Type: `number`
     - Value: `1`
   - Field 11:
     - Field name: `lastUpdated`
     - Type: `number`
     - Value: `1730102400000`
   - Field 12:
     - Field name: `tags`
     - Type: `array`
     - Value: (leave empty for now)
   - Field 13:
     - Field name: `relatedMaterials`
     - Type: `array`
     - Value: (leave empty)
   - Field 14:
     - Field name: `attachments`
     - Type: `array`
     - Value: (leave empty)
5. Click **Save**

---

## Step 7: Create Firestore Indexes

Indexes are CRITICAL for query performance. Without them, queries will fail.

### Create Index #1: Topic Materials Query

1. In Firestore, click **Indexes** tab
2. Click **Create Index**
3. Collection ID: `study_materials`
4. Add fields:
   - Field 1: `topicType` → Ascending
   - Field 2: `displayOrder` → Ascending
5. Query scope: **Collection**
6. Click **Create Index**

**Wait**: Index creation takes 2-5 minutes. Status will show "Building..." then "Enabled".

### Create Index #2: Premium Materials Query (Optional)

1. Click **Create Index**
2. Collection ID: `study_materials`
3. Add fields:
   - Field 1: `isPremium` → Ascending
   - Field 2: `topicType` → Ascending
4. Query scope: **Collection**
5. Click **Create Index**

---

## Step 8: Set Up Cloud Storage (Optional - For Images)

If you plan to store images/PDFs:

1. In Firebase Console, click **Storage** in left sidebar
2. Click **Get Started**
3. Choose **Production mode**
4. Location: Same as Firestore
5. Click **Done**

### Create Test Folder:

1. Click **Files** tab
2. Click **Create folder**
3. Folder name: `health_check`
4. Upload a small test file (any .txt file)
   - Filename: `test.txt`
   - Content: "Health check file"

---

## Step 9: Verify Setup from App

Now test everything from your Android app!

### Test 1: Health Check

Run this from your debug admin panel (we'll create this next):

```kotlin
val healthCheck = // inject FirebaseHealthCheck
val health = healthCheck.checkHealth()

println(health.toDisplayString())
```

**Expected Output**:
```
Firebase Health Check
=====================
Firestore: ✓ Healthy
Cloud Storage: ✓ Healthy

Status: ✓ All systems operational
```

### Test 2: Read Test Document

```kotlin
val firestoreSource = // inject FirestoreContentSource
val topic = firestoreSource.getTopicContent("TEST")

topic.onSuccess { 
    println("✓ Successfully read from Firestore: ${it.title}")
}.onFailure {
    println("✗ Failed to read: ${it.message}")
}
```

**Expected Output**: `✓ Successfully read from Firestore: Test Topic`

---

## Step 10: Run Migration Tool

Once health checks pass, migrate your actual content:

### Test Migration First (Dry Run):

```kotlin
val migrationTool = // inject FirestoreMigrationTool
val preview = migrationTool.testMigration()

println(preview)
```

**Expected Output**:
```
=== Migration Preview ===
Topics to migrate: 9
  OIR: 7 materials
  PPDT: 6 materials
  PSYCHOLOGY: 8 materials
  ...
  
Total materials: 50
Estimated writes: 59
Estimated cost: ~$0.00354
```

### Migrate OIR Only (Pilot):

```kotlin
val result = migrationTool.migrateSingleTopic("OIR")

println(result.toDisplayString())
```

**Expected Output**:
```
Migration Complete
==================
Topics Migrated: 1
Materials Migrated: 7
Errors: 0
Status: ✓ SUCCESS
```

### Verify in Console:

1. Go to Firestore console
2. Check `topic_content` collection
3. You should see `OIR` document
4. Check `study_materials` collection
5. You should see 7 documents with `topicType: "OIR"`

---

## Step 11: Enable Cloud Content for OIR

After successful migration:

```kotlin
ContentFeatureFlags.useCloudContent = true
ContentFeatureFlags.enableTopicCloud("OIR")

println(ContentFeatureFlags.getStatus())
```

**Expected Output**:
```
Cloud Content: ENABLED
Fallback to Local: ENABLED
Offline Persistence: ENABLED
Cache Expiry: 7 days
Enabled Topics: OIR
```

---

## Step 12: Test OIR Topic from Cloud

```kotlin
// This should now fetch from Firestore (not local)
val topic = topicViewModel.loadTopic("OIR")

// Check metrics
val metrics = contentMetrics.getSnapshot()
println(metrics.toDisplayString())
```

**Expected Output**:
```
Firestore Reads: 8 (1 topic + 7 materials)
Cache Hits: 0
Estimated Monthly Cost: $0.00 (within free tier)
```

---

## Troubleshooting

### Error: "PERMISSION_DENIED"

**Cause**: Security rules not deployed or user not authenticated

**Fix**:
1. Check user is signed in: `FirebaseAuth.getInstance().currentUser != null`
2. Redeploy rules: `firebase deploy --only firestore:rules`
3. Check rules in Console: Firestore → Rules tab

### Error: "Collection not found"

**Cause**: Collection doesn't exist yet

**Fix**: Create collection manually (Steps 3-6)

### Error: "Index not found"

**Cause**: Query requires index that doesn't exist

**Fix**:
1. Look at error message for index link
2. Click the link to auto-create index
3. Wait 2-5 minutes for index to build

### Error: "Failed to enable persistence"

**Cause**: Persistence already enabled or app crashed during init

**Fix**: Clear app data and restart

---

## Success Criteria

Setup is complete when:

- [x] Security rules deployed
- [x] 4 collections created (health_check, content_versions, topic_content, study_materials)
- [x] Indexes created and enabled
- [x] Health check passes from app
- [x] Test document readable from app
- [x] OIR migration successful
- [x] OIR topic loads from Firestore
- [x] Cost metrics show $0/month

**Next Step**: Monitor OIR for 2 days, then migrate remaining topics!

---

## Quick Reference

### Firebase Console URLs

- **Project Overview**: https://console.firebase.google.com/project/YOUR_PROJECT/overview
- **Firestore**: https://console.firebase.google.com/project/YOUR_PROJECT/firestore
- **Storage**: https://console.firebase.google.com/project/YOUR_PROJECT/storage
- **Rules**: https://console.firebase.google.com/project/YOUR_PROJECT/firestore/rules

### CLI Commands

```bash
# Login to Firebase
firebase login

# Check current project
firebase projects:list

# Switch project
firebase use YOUR_PROJECT_ID

# Deploy rules only
firebase deploy --only firestore:rules

# Check deployment status
firebase deploy:status
```

---

## Ready to Proceed?

Once you complete these steps:

1. ✓ All collections created
2. ✓ Indexes built
3. ✓ Health check passes
4. ✓ Test migration works

You're ready for **Phase 1**: Pilot rollout with OIR topic! 🚀

