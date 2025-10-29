# Firestore Migration Roadmap - Remaining Topics

**Current Progress**: 3/9 topics complete (33%)  
**Date**: October 29, 2025

---

## ✅ Completed Topics

| Topic | Materials | Status | Date Completed |
|-------|-----------|--------|----------------|
| OIR | 7 | ✅ Migrated | Oct 28, 2025 |
| PPDT | 6 | ✅ Migrated | Oct 29, 2025 |
| Psychology | 8 | ✅ Ready* | Oct 29, 2025 |

*Psychology: Code complete, ready for migration

**Total Migrated**: 21 materials

---

## 🎯 Recommended Migration Order

### Phase 1: Quick Wins (1 day)
**Objective**: Build confidence, test scalability

| Topic | Materials | Complexity | Time | Priority |
|-------|-----------|------------|------|----------|
| **PIQ_FORM** | 3 | Low | 15 mins | 🔥 NEXT |
| **SSB_OVERVIEW** | 4 | Low | 20 mins | Medium |
| **MEDICALS** | 5 | Low | 20 mins | Medium |

**Why These First?**
- Small material counts (easy to verify)
- Low complexity (straightforward content)
- Build momentum
- Test batch migration flow

---

### Phase 2: Core Topics (2 days)
**Objective**: Migrate high-usage content

| Topic | Materials | Complexity | Time | Priority |
|-------|-----------|------------|------|----------|
| **CONFERENCE** | 4 | Medium | 25 mins | High |
| **INTERVIEW** | 7 | Medium | 30 mins | High |

**Why These Next?**
- Important for user experience
- Medium complexity (good learning opportunity)
- High usage topics

---

### Phase 3: Complex Topics (1 day)
**Objective**: Complete migration

| Topic | Materials | Complexity | Time | Priority |
|-------|-----------|------------|------|----------|
| **GTO** | 7 | Medium-High | 35 mins | Medium |

**Why Last?**
- Multiple subtests (GPE, PGT, HGT, Command Task)
- More complex content structure
- Benefit from all previous learnings

---

## 📊 Effort Breakdown

### Time Estimates (Per Topic)

| Task | Time | Notes |
|------|------|-------|
| Create Use Case | 5 mins | Copy Psychology pattern |
| Update ViewModel | 3 mins | Add functions & state |
| Update UI | 5 mins | Button + dialog |
| Enable Feature Flag | 1 min | One line change |
| Build & Install | 2 mins | Gradle + ADB |
| Run Migration | 1 min | Tap button |
| Verify Firebase | 2 mins | Check Console |
| Test in App | 2 mins | Navigate & verify |
| **Total** | **~20 mins** | **Per topic average** |

---

## 🚀 Next Topic: PIQ_FORM

### Why PIQ_FORM is Perfect Next:

1. **Smallest Scope**: Only 3 materials
2. **Quick Win**: 15 minute total time
3. **Simple Content**: Straightforward form guidance
4. **High Confidence**: After 3 successful migrations
5. **Pattern Match**: Exactly same as Psychology

### PIQ_FORM Materials:
1. piq_1: Understanding PIQ Form (8 min)
2. piq_2: Filling PIQ Correctly (12 min)
3. piq_3: Common PIQ Mistakes (10 min)

### Implementation Steps:
```bash
# 1. Create MigratePIQFormUseCase.kt
#    - Copy MigratePsychologyUseCase.kt
#    - Replace "PSYCHOLOGY" → "PIQ_FORM"
#    - Update material count: 8 → 3

# 2. Update SettingsViewModel.kt
#    - Inject migratePIQFormUseCase
#    - Add migratePIQForm() function
#    - Add piqFormMigrationResult to state

# 3. Update SettingsScreen.kt
#    - Add "Migrate PIQ Form" button
#    - Add PIQFormMigrationResultDialog

# 4. Update ContentFeatureFlags.kt
#    - Add "PIQ_FORM" to true

# 5. Test!
```

---

## 🎯 Weekly Migration Plan

### Week 1 (Current):
- ✅ Day 1: OIR (Complete)
- ✅ Day 2: PPDT (Complete)
- ✅ Day 3: Psychology (Code Complete)
- 📅 Day 4: PIQ_FORM + SSB_OVERVIEW + MEDICALS
- 📅 Day 5: Testing & verification

### Week 2:
- 📅 Day 1: CONFERENCE + INTERVIEW
- 📅 Day 2: GTO
- 📅 Day 3: Final testing & documentation
- 📅 Day 4: Monitor & optimize
- 📅 Day 5: Production deployment planning

---

## 💡 Batch Migration Strategy

After PIQ_FORM success, consider batch migration:

### Option A: Sequential (Safer)
- Migrate one topic at a time
- Verify each thoroughly
- **Time**: ~3 hours for 6 remaining topics

### Option B: Batch (Faster)
- Implement 3 topics at once
- Migrate together
- Verify as batch
- **Time**: ~1.5 hours for 6 remaining topics

**Recommendation**: Sequential for next 2 topics, then batch for final 4

---

## 📈 Success Metrics

After completing all migrations, you'll have:

- ✅ **9/9 topics** in Firestore (100%)
- ✅ **51 study materials** migrated
- ✅ **Cloud-first architecture** implemented
- ✅ **Offline persistence** for all content
- ✅ **Instant updates** without app releases
- ✅ **Cost-optimized** with caching
- ✅ **Fallback safety** for reliability

---

## 🎉 Completion Checklist

When all 9 topics are migrated:

- [ ] All topics load from Firestore
- [ ] Material counts match expected
- [ ] Offline mode works for all topics
- [ ] Fallback to local tested
- [ ] Firebase Console verified
- [ ] Logs show cloud loading
- [ ] No duplicate materials
- [ ] All feature flags enabled
- [ ] Documentation updated
- [ ] Monitoring dashboard created

---

## 📞 Quick Reference

**Template Files**:
- Use Case: `MigratePsychologyUseCase.kt`
- ViewModel: `SettingsViewModel.kt` (migratePsychology section)
- UI: `SettingsScreen.kt` (Psychology button & dialog)

**Key Files to Update** (Per Topic):
1. Create: `Migrate[Topic]UseCase.kt`
2. Modify: `SettingsViewModel.kt`
3. Modify: `SettingsScreen.kt`
4. Modify: `ContentFeatureFlags.kt`

**Testing Commands**:
```bash
./gradle.sh assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s [Topic]Migration:D TopicViewModel:D
```

**Firebase Console**:
- Topics: https://console.firebase.google.com/.../topic_content
- Materials: https://console.firebase.google.com/.../study_materials

---

## 🚀 Ready to Continue!

**Current Status**: Psychology code complete, ready to migrate  
**Next Action**: Run Psychology migration  
**After That**: Implement PIQ_FORM (15 mins)

**Estimated Time to 100%**: 3-4 hours of focused work

---

**Let's finish the migration! 🎯**

