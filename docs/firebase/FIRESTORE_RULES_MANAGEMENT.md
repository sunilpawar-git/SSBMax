# Firestore Rules Management

## Active Rules File

**Location**: `/firestore.rules` (project root)

This is the **ONLY** active firestore rules file that Firebase uses.

## Why Root Location?

Firebase CLI expects `firestore.rules` to be in the **project root** (same directory as `firebase.json`).

## Backup Rules

Historical rule backups are stored in `/docs/firebase/rules_backups/`:
- `firestore.rules.CORRECT` - Previous working version
- `firestore.rules.updated` - Another backup version

## Deploying Rules

```bash
firebase deploy --only firestore:rules
```

See full documentation in this directory for security details.
