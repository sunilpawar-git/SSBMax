# Firebase Firestore Index Setup Guide

## 🔥 Issue: Missing Firestore Indexes

Your app is getting an error because Firebase Firestore requires **composite indexes** for queries that filter on one field and sort by another field.

---

## 📋 Required Indexes

### 1. **User Submissions Query**
**Collection**: `submissions`  
**Fields**:
- `userId` (Ascending)
- `submittedAt` (Descending)

### 2. **User Submissions by Test Type Query**
**Collection**: `submissions`  
**Fields**:
- `userId` (Ascending)
- `testType` (Ascending)
- `submittedAt` (Descending)

### 3. **Pending Submissions for Instructor**
**Collection**: `submissions`  
**Fields**:
- `status` (Ascending)
- `submittedAt` (Ascending)

### 4. **Pending Submissions by Batch**
**Collection**: `submissions`  
**Fields**:
- `status` (Ascending)
- `batchId` (Ascending)
- `submittedAt` (Ascending)

---

## 🎯 Two Ways to Create Indexes

### **Option 1: Click the Error Link (EASIEST)**

1. Look at the error message in your app
2. Find the Firebase Console URL in the error (starts with `https://console.firebase.google.com/v1/r/project/ssbmax-49e68/firestore/indexes?create_composite=...`)
3. Click on that link
4. It will open Firebase Console with the index pre-configured
5. Click **"Create Index"**
6. Wait 2-5 minutes for the index to build

### **Option 2: Manual Creation**

#### Step-by-Step:

1. **Go to Firebase Console**
   - Visit: https://console.firebase.google.com/
   - Select your project: **ssbmax-49e68**

2. **Navigate to Firestore Indexes**
   - Click on **"Firestore Database"** in the left sidebar
   - Click on the **"Indexes"** tab at the top

3. **Create Composite Indexes**
   
   Click **"Create Index"** button and add:

   **Index 1: User Submissions**
   - Collection ID: `submissions`
   - Fields to index:
     - Field: `userId`, Order: `Ascending`
     - Field: `submittedAt`, Order: `Descending`
   - Query scope: `Collection`
   - Click **"Create"**

   **Index 2: User Submissions by Test Type**
   - Collection ID: `submissions`
   - Fields to index:
     - Field: `userId`, Order: `Ascending`
     - Field: `testType`, Order: `Ascending`
     - Field: `submittedAt`, Order: `Descending`
   - Query scope: `Collection`
   - Click **"Create"**

   **Index 3: Pending Submissions**
   - Collection ID: `submissions`
   - Fields to index:
     - Field: `status`, Order: `Ascending`
     - Field: `submittedAt`, Order: `Ascending`
   - Query scope: `Collection`
   - Click **"Create"**

   **Index 4: Pending Submissions by Batch**
   - Collection ID: `submissions`
   - Fields to index:
     - Field: `status`, Order: `Ascending`
     - Field: `batchId`, Order: `Ascending`
     - Field: `submittedAt`, Order: `Ascending`
   - Query scope: `Collection`
   - Click **"Create"**

4. **Wait for Indexes to Build**
   - Each index takes 2-5 minutes to build
   - Status will show "Building" → "Enabled"

---

## 🚀 Quick Fix (Recommended)

**Just click the link from your error message!** It's the fastest way.

The error message contains a direct link that looks like:
```
https://console.firebase.google.com/v1/r/project/ssbmax-49e68/firestore/indexes?create_composite=...
```

When you click it:
1. ✅ It pre-fills all the fields
2. ✅ It uses the correct collection name
3. ✅ It sets the right field orders
4. ✅ You just need to click "Create Index"

---

## 📱 Testing After Index Creation

1. Wait for all indexes to show **"Enabled"** status
2. Close and reopen your app
3. Navigate to "My Submissions" screen
4. The error should be gone! 🎉

---

## 🔍 Verification

You can verify indexes are created by:
1. Go to Firebase Console → Firestore Database → Indexes
2. Check that all 4 indexes show **"Enabled"** status
3. Collection should be: `submissions`
4. All fields should match the configuration above

---

## 📸 Visual Guide

### Firebase Console Path:
```
Firebase Console
  └── Project: ssbmax-49e68
      └── Firestore Database
          └── Indexes tab
              └── Create Index button
```

### Index Configuration Example:
```
Collection ID: submissions
Fields:
  ├── userId (Ascending)
  └── submittedAt (Descending)
Query scope: Collection
```

---

## ❓ Why Are Indexes Needed?

Firebase Firestore requires indexes for:
- Queries that filter on one field AND sort by another field
- Queries that filter on multiple fields
- Range queries combined with sorting

This is for performance optimization - indexes help Firestore quickly find and sort data.

---

## 🎯 After Setup

Once indexes are created, your app will:
- ✅ Load user submissions correctly
- ✅ Filter submissions by test type (TAT, WAT, SRT)
- ✅ Show graded and pending submissions
- ✅ Display submissions in correct order (newest first)

---

## 🐛 Troubleshooting

**Q: Index is still building after 10 minutes**
- Large datasets take longer
- Check Firebase Console for any errors
- Try refreshing the Indexes page

**Q: Error persists after index is enabled**
- Wait 1-2 more minutes
- Restart the app completely
- Clear app data and re-login

**Q: Can't access Firebase Console**
- Make sure you're logged in with the correct Google account
- Check that you have Owner/Editor role for the project

---

## 📝 Summary

**What you need to do:**
1. Click the error link OR manually create indexes
2. Wait 2-5 minutes for indexes to build
3. Verify indexes show "Enabled" status
4. Test the app

**Time required:** 5-10 minutes total

Once done, your submissions screen will work perfectly! 🚀

