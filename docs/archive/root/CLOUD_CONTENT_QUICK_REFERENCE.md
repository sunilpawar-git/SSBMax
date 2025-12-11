# Cloud Content Architecture - Quick Reference

## 🚀 Quick Start Commands

### Enable Cloud Content for OIR (Pilot)
```kotlin
ContentFeatureFlags.useCloudContent = true
ContentFeatureFlags.enableTopicCloud("OIR")
```

### Check Health
```kotlin
val health = healthCheck.checkHealth()
println(health.toDisplayString())
```

### Migrate OIR
```kotlin
val result = migrationTool.migrateSingleTopic("OIR")
println(result.toDisplayString())
```

### Check Costs
```kotlin
val metrics = contentMetrics.getSnapshot()
println(metrics.toDisplayString())
```

### Emergency Rollback
```kotlin
ContentFeatureFlags.disableAllCloud()
```

---

## 📂 File Locations

```
core/domain/src/main/kotlin/com/ssbmax/core/domain/
├── config/ContentFeatureFlags.kt
└── model/CloudContent.kt

core/data/src/main/kotlin/com/ssbmax/core/data/
├── metrics/ContentMetrics.kt
├── health/FirebaseHealthCheck.kt
├── source/
│   ├── FirestoreContentSource.kt
│   └── LocalContentSource.kt
└── migration/FirestoreMigrationTool.kt
```

---

## 🔧 Feature Flag Reference

| Flag | Default | Purpose |
|------|---------|---------|
| `useCloudContent` | `false` | Master switch |
| `fallbackToLocalOnError` | `true` | Safety net |
| `enableOfflinePersistence` | `true` | Caching |
| `cacheExpiryDays` | `7` | Cache TTL |

### Methods
```kotlin
ContentFeatureFlags.isTopicCloudEnabled("OIR")  // Check status
ContentFeatureFlags.enableTopicCloud("OIR")      // Enable topic
ContentFeatureFlags.disableTopicCloud("OIR")     // Disable topic
ContentFeatureFlags.disableAllCloud()            // Kill switch
ContentFeatureFlags.enableAllTopics()            // Enable all
ContentFeatureFlags.getStatus()                  // Debug info
```

---

## 💰 Cost Reference

### Free Tier Limits
- **Firestore**: 50k reads/day (1.5M/month)
- **Storage**: 10GB egress/month

### Projected Costs
| Users | Cost/Month |
|-------|------------|
| 1,000 | $0.00 |
| 10,000 | $0.00 |
| 50,000 | $0.05 |
| 100,000 | $0.13 |

### Cost per Operation
- Firestore read: $0.00006
- Firestore write: $0.00006
- Storage egress (per GB): $0.026

---

## 🏥 Health Check Reference

### Check Health
```kotlin
val health = healthCheck.checkHealth()
// Returns: HealthStatus(isFirestoreHealthy, isStorageHealthy, ...)
```

### Quick Check (Firestore only)
```kotlin
val isHealthy = healthCheck.quickCheck()
// Returns: Boolean
```

### Verify Offline Persistence
```kotlin
val result = healthCheck.verifyOfflinePersistence()
// Returns: Result<Boolean>
```

---

## 📊 Metrics Reference

### Record Operations
```kotlin
metrics.recordFirestoreRead()      // After Firestore fetch
metrics.recordCacheHit()            // When serving from cache
metrics.recordStorageDownload()     // When fetching Storage file
metrics.recordFallback()            // When falling back to local
```

### Get Snapshot
```kotlin
val snapshot = metrics.getSnapshot()

// Access fields
snapshot.firestoreReads           // Int
snapshot.cacheHits                // Int
snapshot.cacheHitRate             // Float (0.0 - 1.0)
snapshot.estimatedMonthlyCost     // CostEstimate object
snapshot.estimatedMonthlyCost.totalCost  // Float
snapshot.estimatedMonthlyCost.withinFreeTier  // Boolean
```

### Display
```kotlin
println(snapshot.toDisplayString())
```

---

## 🗄️ Firestore Reference

### Collections
```
/topic_content/{topicId}           # 9 documents
/study_materials/{auto_id}         # ~50 documents
/content_versions/global           # 1 document
/health_check/test                 # 1 document
```

### Required Indexes
1. **study_materials**: `topicType` (Asc) + `displayOrder` (Asc)

### Fetch Topic
```kotlin
val result = firestoreSource.getTopicContent("OIR")
// Cost: 1 read (then cached for 7 days)
```

### Fetch Materials
```kotlin
val result = firestoreSource.getStudyMaterials("OIR")
// Cost: N reads (where N = materials for OIR, e.g., 7)
```

### Get Download URL
```kotlin
val result = firestoreSource.getDownloadUrl("study_materials/oir/image1.jpg")
// Cost: Storage egress when file downloaded
```

---

## 🔄 Migration Reference

### Test Migration (Dry Run)
```kotlin
val preview = migrationTool.testMigration()
println(preview)
// Shows: topics count, materials count, estimated cost
```

### Migrate Single Topic
```kotlin
val result = migrationTool.migrateSingleTopic("OIR")
println(result.toDisplayString())
// Migrates: 1 topic + materials for OIR
```

### Migrate All
```kotlin
val result = migrationTool.migrateAll()
println(result.toDisplayString())
// Migrates: all 9 topics + all materials
// Cost: ~$0.004 (less than 1 cent)
```

---

## 🚨 Emergency Procedures

### Rollback to Local Content
```kotlin
// Option 1: Disable all cloud (instant)
ContentFeatureFlags.disableAllCloud()

// Option 2: Disable specific topic
ContentFeatureFlags.disableTopicCloud("OIR")

// Option 3: Set master switch off
ContentFeatureFlags.useCloudContent = false
```

### Check if Fallback Working
```kotlin
val metrics = contentMetrics.getSnapshot()
println("Fallbacks: ${metrics.fallbacksToLocal}")
// If > 0, app is falling back to local
```

### Force Refresh from Server
```kotlin
val result = firestoreSource.forceRefresh("OIR")
// Bypasses cache, fetches fresh from Firestore
// Use sparingly (incurs Firestore read cost)
```

---

## 🧪 Testing Checklist

### Phase 0 (Safety Infrastructure)
- [x] Feature flags implemented
- [x] Cost monitoring active
- [x] Health checks working
- [x] Migration tool ready

### Phase 1 (Console Setup)
- [ ] Collections created
- [ ] Indexes built
- [ ] Health check passes
- [ ] Test document readable

### Phase 2 (Pilot OIR)
- [ ] OIR migrated
- [ ] Cloud enabled for OIR
- [ ] Content loads from Firestore
- [ ] Costs within free tier

### Phase 3 (Monitor)
- [ ] Cache hit rate >80%
- [ ] No errors in logs
- [ ] Cost stays $0/month
- [ ] Performance acceptable

### Phase 4 (Scale)
- [ ] Enable all topics
- [ ] Monitor daily
- [ ] Cost stays <$5/month
- [ ] Ready for production

---

## 📱 Firebase Console URLs

Replace `YOUR_PROJECT` with your Firebase project ID:

- **Overview**: https://console.firebase.google.com/project/YOUR_PROJECT/overview
- **Firestore**: https://console.firebase.google.com/project/YOUR_PROJECT/firestore
- **Storage**: https://console.firebase.google.com/project/YOUR_PROJECT/storage
- **Rules**: https://console.firebase.google.com/project/YOUR_PROJECT/firestore/rules
- **Indexes**: https://console.firebase.google.com/project/YOUR_PROJECT/firestore/indexes

---

## 🔐 Security Rules Quick Check

### Verify Rules Deployed
```bash
firebase deploy --only firestore:rules
```

### Test Read Access
```kotlin
val canRead = healthCheck.canReadFromFirestore()
println("Can read: $canRead")
```

---

## 📖 Documentation Files

| File | Purpose |
|------|---------|
| `PHASE_0_SAFETY_INFRASTRUCTURE_COMPLETE.md` | Full Phase 0 details |
| `FIREBASE_CONSOLE_SETUP_GUIDE.md` | Step-by-step Console setup |
| `CLOUD_CONTENT_PHASE_0_SUMMARY.md` | Implementation summary |
| `CLOUD_CONTENT_QUICK_REFERENCE.md` | This file |
| `cloud-content-architecture.plan.md` | Complete architecture plan |

---

## 🎯 Success Metrics

### Cost Metrics
- Target: <$5/month for 10k users
- Free tier: Covers up to 50k users
- Current: $0/month (cloud disabled)

### Performance Metrics
- Cache hit rate: Target >80%
- Firestore reads: Target <10k/day
- Page load time: <2 seconds

### Reliability Metrics
- Uptime: 99.9% (with fallback)
- Error rate: <0.1%
- Fallback rate: <1%

---

## 🆘 Troubleshooting

| Error | Solution |
|-------|----------|
| PERMISSION_DENIED | Deploy security rules |
| Index not found | Create composite index |
| Collection not found | Create collection in Console |
| Persistence failed | Clear app data |
| Health check fails | Check network/auth |
| High costs | Check cache hit rate |

---

## 📞 Quick Debug Commands

```kotlin
// Feature flags status
println(ContentFeatureFlags.getStatus())

// Firebase health
println(healthCheck.checkHealth().toDisplayString())

// Cost metrics
println(contentMetrics.getSnapshot().toDisplayString())

// Migration preview
println(migrationTool.testMigration())

// Check if topic using cloud
val usingCloud = ContentFeatureFlags.isTopicCloudEnabled("OIR")
println("OIR using cloud: $usingCloud")
```

---

## ✅ Pre-Flight Checklist

Before enabling cloud content:

- [ ] Security rules deployed
- [ ] Collections created
- [ ] Indexes built (status: "Enabled")
- [ ] Health check passes
- [ ] Test migration successful
- [ ] Content loads from Firestore
- [ ] Fallback works
- [ ] Cost metrics within budget

---

**Status**: Phase 0 Complete ✅  
**Next**: Firebase Console Setup 🚀  
**Risk**: Zero (cloud disabled) 🟢  
**Cost**: $0/month 💰

