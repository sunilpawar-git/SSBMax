# Drawer Profile "?" Issue - Debugging

## Issue
User profile shows "?" in drawer even after profile is created and saved successfully.

## Evidence
- ✅ Profile save works (no permission errors)
- ✅ Home screen shows "Welcome, Sunil"
- ✅ Profile edit screen shows "S" avatar with full data
- ❌ Drawer shows "?" with "Complete Your Profile"

## Hypothesis

### Possibility 1: Data Not Loading
The drawer's `UserProfileViewModel` isn't receiving the profile data from Firestore.

**Test**: Check logcat for "DrawerHeader" logs showing profile data.

### Possibility 2: Timing Issue
Profile loads after the drawer has already rendered with empty state.

**Test**: Check if drawer updates after a delay or after reopening.

### Possibility 3: Validation Logic
The condition `userProfile != null && userProfile.fullName.isNotBlank()` is failing.

**Test**: Logs will show the actual values.

### Possibility 4: ViewModel Scope
The drawer's ViewModel is a different instance than the profile screen's ViewModel.

**Test**: Verify if data persists across navigation.

## Changes Made for Debugging

### 1. Added Logging to DrawerHeader.kt
```kotlin
Log.d("DrawerHeader", "userProfile: $userProfile")
Log.d("DrawerHeader", "isLoading: $isLoading")
Log.d("DrawerHeader", "fullName: ${userProfile?.fullName}")
Log.d("DrawerHeader", "isNotBlank: ${userProfile?.fullName?.isNotBlank()}")
```

### 2. Updated saveProfile() in UserProfileViewModel
```kotlin
result.fold(
    onSuccess = {
        _uiState.update { 
            it.copy(
                profile = profile,  // ← Added this line
                isLoading = false,
                isSaved = true,
                error = null
            ) 
        }
    },
    // ...
)
```

This ensures the `profile` field in UI state is updated after successful save.

## Testing Steps

### Step 1: Install New APK
```bash
cd /Users/sunil/Downloads/SSBMax
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Monitor Logs
```bash
adb logcat -s DrawerHeader:D
```

### Step 3: Test Scenarios

#### Scenario A: Fresh Launch
1. Launch app
2. Open drawer immediately
3. **Check logs**: What does `userProfile` show?

#### Scenario B: After Profile Edit
1. Edit profile
2. Save profile
3. Go back
4. Open drawer
5. **Check logs**: Did profile update?

#### Scenario C: Force Refresh
1. Close drawer
2. Navigate away
3. Come back
4. Open drawer again
5. **Check logs**: Does it load now?

## Expected Log Output

### If Profile Loads Correctly:
```
D/DrawerHeader: userProfile: UserProfile(userId=abc123, fullName=Sunil, age=18, ...)
D/DrawerHeader: isLoading: false
D/DrawerHeader: fullName: Sunil
D/DrawerHeader: isNotBlank: true
```
→ Should show "SU" avatar with name

### If Profile is Null:
```
D/DrawerHeader: userProfile: null
D/DrawerHeader: isLoading: false
D/DrawerHeader: fullName: null
D/DrawerHeader: isNotBlank: false
```
→ Shows "?" with "Complete Your Profile" (current behavior)

### If Profile is Loading:
```
D/DrawerHeader: userProfile: null
D/DrawerHeader: isLoading: true
D/DrawerHeader: fullName: null
D/DrawerHeader: isNotBlank: false
```
→ Should show loading spinner

## Potential Issues & Solutions

### Issue: Profile is Null
**Root Cause**: Firestore query failing or path mismatch

**Solution**:
- Verify Firestore path: `users/{userId}/data/profile`
- Check Firestore rules allow read access
- Verify user is authenticated

### Issue: Profile Loads But Empty fullName
**Root Cause**: Data structure mismatch or incomplete save

**Solution**:
- Check Firestore document structure
- Verify all fields are saved correctly
- Check mapper functions (toMap/toUserProfile)

### Issue: isLoading Always True
**Root Cause**: Flow collection not completing or error not handled

**Solution**:
- Check for exceptions in loadProfile()
- Verify Flow is emitting values
- Check ViewModel lifecycle

### Issue: ViewModel Instance Different
**Root Cause**: Hilt scope issue or navigation clearing ViewModels

**Solution**:
- Use ActivityRetainedScoped ViewModel
- Share ViewModel across navigation
- Use a shared StateFlow

## Next Steps Based on Logs

### If logs show `userProfile: null`:
1. Check if AuthRepository.currentUser is working
2. Verify Firestore read permissions
3. Check if document exists in Firestore console
4. Test getUserProfile() function directly

### If logs show `userProfile: UserProfile(...) but isNotBlank: false`:
1. Check why fullName is empty despite data existing
2. Verify data mapping in repository
3. Check Firestore document structure

### If logs show correct data but UI still shows "?":
1. Recomposition issue - UI not updating
2. Check conditional logic in DrawerHeader
3. Verify State is being collected properly

## Files Modified

1. `DrawerHeader.kt` - Added debug logging
2. `UserProfileViewModel.kt` - Updated saveProfile to set profile field

---

**Status**: Awaiting logcat output to diagnose further  
**Build**: Successful  
**Next**: Run app, open drawer, check logs

