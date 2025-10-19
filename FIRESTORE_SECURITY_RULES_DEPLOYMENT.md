# Firestore Security Rules Deployment Guide

## Overview

The updated Firestore security rules (`firestore.rules.updated`) implement strict access control to prevent unauthorized access to test content and protect user data.

## Key Security Features

### 1. **Test Content Protection**
- Test questions can ONLY be accessed during an active test session
- Session must be valid (not expired, max 2 hours)
- Rate limiting: 100 reads per hour per user
- Prevents bulk scraping and unauthorized downloads

### 2. **Test Session Management**
- Users must create a test session before accessing questions
- Sessions automatically expire after 2 hours
- Users can only access their own sessions
- Session validation on every test content read

### 3. **User Data Privacy**
- Users can only read/write their own data
- Role field cannot be modified by users
- Separate read permissions for students vs assessors

### 4. **Submission Security**
- Students can create submissions
- Only assigned assessors can grade submissions
- Submissions cannot be deleted
- Student ID cannot be changed after submission

### 5. **Notification Security**
- Users can only read their own notifications
- Notifications can only be created by server (Cloud Functions)
- Users can mark notifications as read/delete them

## Deployment Steps

### Step 1: Backup Current Rules

```bash
# Using Firebase CLI
firebase firestore:rules:get > firestore.rules.backup
```

### Step 2: Deploy New Rules

```bash
# Copy the updated rules
cp firestore.rules.updated firestore.rules

# Deploy to Firebase
firebase deploy --only firestore:rules
```

### Step 3: Verify Deployment

```bash
# Check deployed rules
firebase firestore:rules:list
```

### Step 4: Test in Firebase Console

1. Go to Firebase Console > Firestore > Rules
2. Click "Rules Playground"
3. Test scenarios:
   - ✅ Student with active session reading test content
   - ❌ Student without session reading test content
   - ❌ Unauthenticated user reading test content
   - ✅ User reading own profile
   - ❌ User reading another user's profile

## Testing Scenarios

### Scenario 1: Valid Test Access
```
Authenticated: true
Path: /tests/oir_standard
Active Session: true
Session Expiry: In future
Result: ✅ Allow
```

### Scenario 2: No Active Session
```
Authenticated: true
Path: /tests/oir_standard
Active Session: false
Result: ❌ Deny
```

### Scenario 3: Expired Session
```
Authenticated: true
Path: /tests/oir_standard
Active Session: true
Session Expiry: In past
Result: ❌ Deny
```

### Scenario 4: Rate Limit Exceeded
```
Authenticated: true
Path: /tests/oir_standard
Active Session: true
Read Count: 101
Result: ❌ Deny
```

## Monitoring

### Enable Firestore Rules Logging

1. Go to Firebase Console > Firestore > Usage
2. Enable "Rules evaluation" logs
3. Monitor for denied requests

### Cloud Monitoring Alerts

Set up alerts for:
- High rate of denied requests (potential attack)
- Unusual patterns in test content access
- Failed session validations

## Important Notes

### Rate Limiting
- The rate limit is implemented in the rules but needs server-side tracking
- Consider implementing a more robust rate limiting solution using Cloud Functions

### Session Management
- Sessions are automatically cleaned up after expiry
- Consider running a scheduled Cloud Function to delete expired sessions

### Performance
- `exists()` and `get()` calls count towards Firestore read quotas
- Session validation adds 1 extra read per test content access
- Monitor costs in Firebase Console

## Security Best Practices

1. **Never disable these rules** - They are critical for preventing test content theft
2. **Monitor access patterns** - Set up alerts for suspicious activity
3. **Regular audits** - Review Firestore logs monthly
4. **Keep sessions short** - 2 hour max is recommended
5. **Implement backend validation** - Don't rely solely on client-side rules

## Rollback Plan

If issues occur after deployment:

```bash
# Restore previous rules
cp firestore.rules.backup firestore.rules
firebase deploy --only firestore:rules
```

## Support

For issues or questions about the security rules:
1. Check Firebase Console logs
2. Review denied request patterns
3. Test in Rules Playground
4. Contact team lead if persistent issues

## Changelog

### Version 2.0 (Current)
- Added test session validation
- Implemented rate limiting
- Enhanced role-based access control
- Added notification security
- Protected study materials

### Version 1.0 (Previous)
- Basic authentication
- Simple read/write rules
- No session management

