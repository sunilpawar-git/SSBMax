# SSBMax Technical Debt Reduction Plan

## 📋 Current Technical Debt (Post-Firestore Migration)

### File Size Violations (Critical Files Only)
- SettingsScreen.kt (1,748 lines) - Core settings UI
- PPDTTestScreen.kt (746 lines) - Test interface
- TATTestScreen.kt (723 lines) - Test interface  
- OIRTestScreen.kt (603 lines) - Test interface
- SharedNavGraph.kt (595 lines) - Navigation logic
- DashboardScreen.kt (743 lines) - Main dashboard

### Build Configuration Issues
- Release build: minifyEnabled = false (SECURITY RISK)
- Debug subscription bypass enabled (PRODUCTION EXPLOITABLE)
- TODO: Real billing implementation missing

### Architecture Violations
- ViewModels directly inject repositories (violates clean architecture)
- Billing logic at app level (should be data layer)
- Use cases mixed in UI layer

### Incomplete Features
- 45+ TODO/FIXME comments across codebase
- Mock data providers in production code
- Missing analytics: streak calculation, study patterns
- Incomplete security implementations

### Security Issues
- Lint warning: Missing Firebase Analytics permissions
- Firebase rules allow writes during migration
- Debug subscription bypass could be exploited

### Testing Gaps
- 64 test files but incomplete coverage for core features
- Large test files (920+ lines) indicate complex scenarios
- Missing integration tests for critical flows

### Performance Concerns
- Large files with complex logic
- Compose recomposition risks in oversized components

---

## 🎯 Revised Phase Implementation Plan (6 Weeks Total)

### 🚨 PHASE 1: PRODUCTION BLOCKERS (Week 1)

#### Security & Build Fixes (Day 1-2)
- [ ] Enable minifyEnabled = true in release build (CRITICAL)
- [ ] Remove debug subscription bypass flags (SECURITY)
- [ ] Fix Firebase Analytics permission warnings
- [ ] Secure Firebase rules post-migration

#### File Size Critical Fixes (Day 3-5)
- [ ] Break down SettingsScreen.kt into SettingsScreen, SubscriptionSection, ThemeSection (<300 lines each)
- [ ] Split SharedNavGraph.kt into AuthNavGraph, StudentNavGraph, InstructorNavGraph
- [ ] Refactor DashboardScreen.kt using composition pattern

**Rollback Plan**: Keep backup branches for each file; can revert individual components if issues arise.

### 🏗️ PHASE 2: ARCHITECTURE CLEANUP (Week 2)

#### Clean Architecture Implementation
- [ ] Create use cases: ObserveCurrentUserUseCase, GetUserTestsUseCase, UpdateSubscriptionUseCase
- [ ] Refactor MainViewModel to inject use cases instead of AuthRepository + UserProfileRepository
- [ ] Refactor StudentHomeViewModel to inject use cases instead of repositories
- [ ] Refactor SettingsViewModel to inject use cases instead of NotificationRepository
- [ ] Move billing logic from app/src/main/kotlin/com/ssbmax/billing/ to core/data/

#### Repository Layer Cleanup
- [ ] Remove mock data providers from production builds
- [ ] Update deprecated APIs in AuthRepositoryImpl and TestRepositoryImpl

**Rollback Plan**: Use feature flags to toggle between old/new architecture during transition.

### ⚡ PHASE 3: FEATURE COMPLETION (Weeks 3-4)

#### TODO/FIXME Resolution
- [ ] Implement streak calculation in AnalyticsRepositoryImpl
- [ ] Add study pattern tracking in UserProgress tracking
- [ ] Complete security implementations in ViewModels (TAT, GTO, IO phases)
- [ ] Remove all production TODO comments or convert to GitHub issues

#### Remaining File Size Fixes
- [ ] Break down PPDTTestScreen.kt, TATTestScreen.kt, OIRTestScreen.kt (<400 lines each)
- [ ] Split large test ViewModels using composition
- [ ] Refactor oversized test files with shared components

**Rollback Plan**: Each file refactoring in separate PR; can rollback individual features.

### 🧪 PHASE 4: QUALITY ASSURANCE (Weeks 5-6)

#### Testing Enhancement
- [ ] Audit test coverage for TAT, WAT, SRT, PPDT flows (target: 85%+)
- [ ] Add integration tests for authentication → test flow
- [ ] Add integration tests for subscription → test access flow
- [ ] Break down large test files (>500 lines) into focused test classes
- [ ] Add UI tests for critical screens (Login, Test Taking, Results)

#### Performance Optimization
- [ ] Profile SettingsScreen and Dashboard for recomposition issues
- [ ] Implement proper remember patterns in large components
- [ ] Optimize memory usage in test screens
- [ ] Verify LeakCanary effectiveness and fix any leaks found

#### Code Quality Gates
- [ ] Implement automated 300-line limit checks in CI/CD
- [ ] Add architecture violation detection (repository injection checks)
- [ ] Create automated TODO tracking script
- [ ] Set up code quality metrics dashboard

**Rollback Plan**: Performance optimizations can be disabled via build flags if regressions occur.

---

## 📈 Success Metrics

- ✅ 0 files exceeding 300-line limit
- ✅ 0 TODO/FIXME comments in production code
- ✅ 100% clean architecture compliance (use cases mediate all data access)
- ✅ 0 critical security issues (permissions, bypasses resolved)
- ✅ 85%+ test coverage for core features
- ✅ No performance regressions in key user flows
- ✅ Production-ready build configuration

## 🎯 Completion Criteria

**100% Tech Debt Free** when all checkboxes are marked and:
- All lint warnings resolved
- All architectural patterns enforced
- All tests passing (unit + integration)
- Performance benchmarks met (startup <3s, test load <2s)
- Security audit passed
- Code review standards updated

## ⚠️ Risk Mitigation

- **Daily standups** to track progress and blockers
- **Feature flags** for architectural changes
- **Staged rollouts** for performance optimizations
- **Automated testing** gates prevent regressions
- **Backup branches** for all major refactoring

---

*Last Updated: November 13, 2025*
*Timeline: 6 weeks (reduced from 12)*
*Content files excluded (deleted via Firestore migration)*
