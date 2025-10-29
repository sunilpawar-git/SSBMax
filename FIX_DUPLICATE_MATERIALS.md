# 🔧 Fix Duplicate Materials Issue

## 🔴 Problem: Multiple Copies of Same Materials

**What Happened**: You ran the PPDT migration **3 times**, and each time it created **new documents** in Firestore instead of updating existing ones.

**Result**: 
- PPDT Test Overview × 3
- Story Writing Techniques × 3
- Group Discussion Strategies × 3
- etc.

**Total**: 18 documents instead of 6

---

## 🎯 Root Cause

The migration code uses `.add()` which **creates a new document** every time:

```kotlin
// In MigratePPDTUseCase.kt (line 143)
firestore.collection("study_materials")
    .add(materialDocument)  // ← Creates NEW document every time
    .await()
```

**Should use** `.document(id).set()` to **replace** existing documents.

---

## ✅ Solution: Clean Up Firestore

### Option 1: Delete Duplicates Manually (Quick)

1. Open Firebase Console: https://console.firebase.google.com/project/ssbmax-49e68/firestore
2. Go to `study_materials` collection
3. Filter: `topicType == PPDT`
4. **Delete ALL PPDT materials** (click each document → Delete)
5. Run migration **once more** (will create clean 6 materials)

---

### Option 2: Delete via Firebase Console Query

1. Open `study_materials` collection
2. Add filter: `topicType` == `PPDT`
3. Select all visible documents
4. Delete them (click trash icon)
5. Repeat until all PPDT materials are gone
6. Run migration once to recreate clean data

---

### Option 3: Fix the Migration Code (Best)

Update `MigratePPDTUseCase.kt` to use document IDs:

```kotlin
// Replace this (line 143):
firestore.collection("study_materials")
    .add(materialDocument)
    .await()

// With this:
firestore.collection("study_materials")
    .document(materialItem.id)  // Use material ID as document ID
    .set(materialDocument)      // Replaces if exists
    .await()
```

**Benefits**:
- ✅ Running migration multiple times won't create duplicates
- ✅ Updates existing materials instead of creating new ones
- ✅ Consistent document IDs

---

## 🔧 Quick Fix Steps

### Step 1: Clean Firestore
```
1. Firebase Console → study_materials
2. Filter: topicType == PPDT
3. Delete all (should be 18 documents)
4. Verify: Filter should show "No results"
```

### Step 2: Fix Migration Code

I'll update the code to prevent future duplicates:

