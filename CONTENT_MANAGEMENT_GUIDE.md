# 📝 SSBMax Content Management Guide

**Version**: 1.0  
**Last Updated**: October 29, 2025  
**Purpose**: Guide for managing cloud content on Firestore

---

## 🎯 Quick Reference

### Content Locations
- **Primary**: Firebase Firestore (Cloud)
- **Fallback**: Kotlin files (Local)
- **Firebase Console**: https://console.firebase.google.com/project/ssbmax-49e68/firestore

### Collections
- `topic_content`: Topic-level data (9 documents)
- `study_materials`: Study material content (51+ documents)

---

## 📝 Editing Existing Content

### Via Firebase Console (Recommended for Quick Edits)

#### Step-by-Step:

1. **Open Firebase Console**
   ```
   https://console.firebase.google.com/project/ssbmax-49e68/firestore
   ```

2. **Navigate to Material**
   ```
   Firestore Database → study_materials → Find material ID
   Example: oir_1, ppdt_1, psy_1, etc.
   ```

3. **Edit Content**
   - Click on the material document
   - Find `contentMarkdown` field
   - Click edit icon (pencil)
   - Modify content (supports Markdown)
   - Click "Update"

4. **Verify Changes**
   - Open SSBMax app
   - Navigate to the topic
   - Open the material
   - See updated content instantly! ✅

#### Editable Fields:
```javascript
{
  "id": "oir_1",                    // ❌ Don't change
  "topicType": "OIR",               // ❌ Don't change
  "title": "Material Title",        // ✅ Can edit
  "contentMarkdown": "# Content",   // ✅ Edit this!
  "author": "SSB Expert",           // ✅ Can edit
  "readTime": "10 min read",        // ✅ Can edit
  "isPremium": false,               // ✅ Can toggle
  "tags": ["OIR", "Tests"],         // ✅ Can modify
  "displayOrder": 1,                // ⚠️ Careful!
  "lastUpdated": 1234567890,        // ✅ Auto-updates
}
```

### Via Admin Script (Bulk Updates)

Create this admin tool in Android Studio:

```kotlin
// File: AdminContentManager.kt
package com.ssbmax.admin

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AdminContentManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    /**
     * Update a single material's content
     */
    suspend fun updateMaterialContent(
        materialId: String,
        newContent: String
    ): Result<Unit> {
        return try {
            firestore.collection("study_materials")
                .document(materialId)
                .update(
                    mapOf(
                        "contentMarkdown" to newContent,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bulk update multiple materials
     */
    suspend fun bulkUpdateContent(
        updates: Map<String, String>
    ): Result<Int> {
        return try {
            var successCount = 0
            
            updates.forEach { (materialId, newContent) ->
                updateMaterialContent(materialId, newContent)
                    .onSuccess { successCount++ }
            }
            
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update material metadata (title, readTime, etc.)
     */
    suspend fun updateMaterialMetadata(
        materialId: String,
        title: String? = null,
        readTime: String? = null,
        isPremium: Boolean? = null,
        author: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "lastUpdated" to System.currentTimeMillis()
            )
            
            title?.let { updates["title"] = it }
            readTime?.let { updates["readTime"] = it }
            isPremium?.let { updates["isPremium"] = it }
            author?.let { updates["author"] = it }
            
            firestore.collection("study_materials")
                .document(materialId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current content for editing
     */
    suspend fun getMaterialContent(materialId: String): Result<String> {
        return try {
            val doc = firestore.collection("study_materials")
                .document(materialId)
                .get()
                .await()
            
            val content = doc.getString("contentMarkdown") 
                ?: throw Exception("Content not found")
            
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Usage:**
```kotlin
// In a ViewModel or Admin screen
class AdminViewModel @Inject constructor(
    private val adminManager: AdminContentManager
) : ViewModel() {
    
    fun updateOIRContent() {
        viewModelScope.launch {
            val newContent = """
                # Updated OIR Test Pattern - 2025
                
                ## What's New
                - Updated question patterns
                - New tips and tricks
                - Enhanced strategies
                
                ## Complete Guide
                [Your enhanced content here...]
            """.trimIndent()
            
            adminManager.updateMaterialContent(
                materialId = "oir_1",
                newContent = newContent
            ).onSuccess {
                // Show success message
            }.onFailure { error ->
                // Show error
            }
        }
    }
}
```

---

## ➕ Adding New Content

### Option 1: Direct Firestore Entry

#### Add New Material:
```javascript
// In Firebase Console
Collection: study_materials
Document ID: new_material_id

Fields:
{
  "id": "new_material_id",
  "topicType": "OIR",              // Must match topic
  "title": "New Material Title",
  "contentMarkdown": "# Full markdown content here...",
  "displayOrder": 8,               // Next in sequence
  "category": "OIR Tests",
  "author": "SSB Expert",
  "readTime": "10 min read",
  "isPremium": false,
  "version": 1,
  "lastUpdated": [timestamp],
  "tags": ["OIR", "Tests"],
  "relatedMaterials": [],
  "metadata": {
    "publishedDate": "Oct 29, 2025",
    "addedBy": "Admin"
  }
}
```

#### Add New Topic:
```javascript
// In Firebase Console
Collection: topic_content
Document ID: NEW_TOPIC

Fields:
{
  "id": "NEW_TOPIC",
  "topicType": "NEW_TOPIC",
  "title": "New Topic Title",
  "introduction": "Topic introduction text...",
  "version": 1,
  "lastUpdated": [timestamp],
  "isPremium": false,
  "metadata": {
    "createdBy": "Admin",
    "createdAt": [timestamp]
  }
}
```

**Then enable in app:**
```kotlin
// In ContentFeatureFlags.kt
private val topicFlags = mutableMapOf<String, Boolean>(
    // ... existing topics
    "NEW_TOPIC" to true  // Add this line
)
```

### Option 2: Via Migration (Recommended for New Topics)

1. **Add to local Kotlin file:**
```kotlin
// In StudyMaterialsProvider.kt
"NEW_TOPIC" -> getNewTopicMaterials()

private fun getNewTopicMaterials() = listOf(
    StudyMaterialItem(
        id = "new_1",
        title = "New Material",
        duration = "10 min read",
        isPremium = false
    )
)
```

2. **Add content:**
```kotlin
// In StudyMaterialContentProvider.kt
"new_1" -> StudyMaterialContent(
    id = "new_1",
    title = "New Material",
    content = """
        # New Material Title
        
        Complete content here...
    """.trimIndent(),
    // ...
)
```

3. **Create migration use case** (copy from existing patterns)

4. **Add to Settings** → Run migration

---

## 🔄 Content Update Workflow

### Quick Edit (Single Material)
```
1. Open Firebase Console
2. Find material in study_materials
3. Edit contentMarkdown field
4. Save
5. Test in app
   ↓
   Done! (< 2 minutes)
```

### Bulk Update (Multiple Materials)
```
1. Create admin script (see above)
2. Prepare new content
3. Run bulk update function
4. Verify in Firebase Console
5. Test in app
   ↓
   Done! (< 10 minutes)
```

### New Topic (Complete)
```
1. Add to local Kotlin files
2. Create migration use case
3. Add button in Settings
4. Rebuild app
5. Run migration
6. Enable feature flag
   ↓
   Done! (< 30 minutes)
```

---

## ⚠️ Important Guidelines

### DO's ✅
- ✅ Always test changes in Firebase Console first
- ✅ Keep local files as fallback
- ✅ Use Markdown for formatting
- ✅ Update `lastUpdated` timestamp
- ✅ Verify changes in app before releasing
- ✅ Back up content before bulk edits
- ✅ Use version control for code changes

### DON'Ts ❌
- ❌ Don't change `id` or `topicType` fields
- ❌ Don't delete documents without backup
- ❌ Don't skip testing after edits
- ❌ Don't forget to update `displayOrder` for new materials
- ❌ Don't remove local fallback files
- ❌ Don't make breaking changes without migration plan

---

## 📊 Content Structure Reference

### Material Content Format (Markdown)
```markdown
# Material Title

## Introduction
Brief overview of the topic...

## Section 1: Main Content
Detailed explanation...

### Subsection
More details...

## Key Points
- Point 1
- Point 2
- Point 3

## Tips & Tricks
1. Tip 1
2. Tip 2

## Practice Questions
**Question 1**: Your question here?

**Answer**: Detailed answer...

## Summary
Quick recap of main points...

## Next Steps
What to study next...
```

### Topic Introduction Format
```markdown
Welcome to [Topic Name]!

This comprehensive guide covers:
- Topic area 1
- Topic area 2
- Topic area 3

You'll learn everything needed to excel in [Topic].
```

---

## 🛠️ Admin Tools (Future Enhancements)

### Planned Features:
1. **Content Editor UI**
   - Rich text editor in app
   - Preview before publishing
   - Version history

2. **Bulk Operations**
   - Import/export content
   - Search and replace
   - Tag management

3. **Analytics**
   - Most viewed materials
   - User engagement metrics
   - Content effectiveness

4. **Scheduling**
   - Schedule content updates
   - Time-based content visibility
   - A/B testing support

---

## 📝 Quick Commands

### Firebase CLI (Advanced)
```bash
# Export all content (backup)
firebase firestore:export gs://ssbmax-backup/$(date +%Y%m%d)

# Import content (restore)
firebase firestore:import gs://ssbmax-backup/20251029

# Query materials
firebase firestore:get study_materials --where topicType==OIR
```

### ADB Logs (Check content loading)
```bash
# Check if loading from cloud
adb logcat -s TopicViewModel:D | grep "Loading from CLOUD"

# Check material count
adb logcat -s TopicViewModel:D | grep "materials"
```

---

## 🆘 Troubleshooting

### Content not updating in app?
1. Check Firestore cache (may be cached for 7 days)
2. Clear app data: `adb shell pm clear com.ssbmax`
3. Verify changes in Firebase Console
4. Check feature flags enabled

### Can't edit in Firebase Console?
1. Verify you're logged in
2. Check Firestore security rules
3. Ensure you have write permissions
4. Try incognito/private window

### Material not showing in app?
1. Verify `topicType` matches topic ID (uppercase)
2. Check `displayOrder` is set
3. Ensure feature flag enabled
4. Check logs for errors

---

## 📞 Support

For questions or issues:
1. Check this guide first
2. Review `FIRESTORE_MIGRATION_PLAYBOOK.md`
3. Check Firebase Console logs
4. Review app logs via `adb logcat`

---

**Last Updated**: October 29, 2025  
**Version**: 1.0  
**Status**: Active - All 9 topics migrated ✅


