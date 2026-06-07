# CLAUDE_HIERARCHY.md — Architecture Guidance Navigation

**Quick reference:** Find the CLAUDE.md file relevant to your work.

---

## 📋 Full Module Hierarchy

```
SSBMax Project (Root)
│
├── 🌱 Root: [claude.md](claude.md)
│   └── Purpose: 12 core rules, architecture overview, global patterns
│
├── 📦 App Module: [app/CLAUDE.md](app/CLAUDE.md)
│   ├── UI/Composable Patterns
│   ├── ViewModel Architecture
│   ├── Memory Leak Prevention
│   └── Sub-Modules:
│       ├── [app/ui/CLAUDE.md](app/ui/CLAUDE.md) — Feature screens + state management
│       ├── [app/di/CLAUDE.md](app/di/CLAUDE.md) — Hilt dependency injection
│       └── [app/navigation/CLAUDE.md](app/navigation/CLAUDE.md) — Type-safe routing
│
├── 📚 Core Domain Module: [core/domain/CLAUDE.md](core/domain/CLAUDE.md)
│   ├── Use Case Patterns
│   ├── Repository Interfaces (SSOT)
│   ├── Result<T> Error Handling
│   ├── Data Models (Immutable)
│   └── **Zero Android Dependencies**
│
├── 🔌 Core Data Module: [core/data/CLAUDE.md](core/data/CLAUDE.md)
│   ├── Repository Implementations
│   ├── Secret Management
│   ├── Error Wrapping (Result<T>)
│   └── Sub-Modules:
│       ├── [core/data/ai/CLAUDE.md](core/data/ai/CLAUDE.md) — Gemini API integration
│       ├── [core/data/local/CLAUDE.md](core/data/local/CLAUDE.md) — Room database patterns
│       └── [core/data/remote/CLAUDE.md](core/data/remote/CLAUDE.md) — Firebase integration
│
├── 🎨 Core Design System: [core/designsystem/CLAUDE.md](core/designsystem/CLAUDE.md)
│   ├── Reusable Components
│   ├── Material3 Theming
│   ├── Accessibility (WCAG)
│   ├── Composable Size Limits
│   └── **Requires @Preview on all components**
│
├── 🛠 Lint Module: [lint/CLAUDE.md](lint/CLAUDE.md)
│   ├── Custom Detector Development
│   ├── Phase 2-4 Security Detectors (14 total)
│   ├── Testing Detectors
│   └── Build Integration
│
├── ☁️ Firebase Functions: [functions/CLAUDE.md](functions/CLAUDE.md)
│   ├── Cloud Function Patterns
│   ├── Security-First Approach
│   ├── Gemini Backend Integration
│   ├── Firestore Transactions
│   └── Rate Limiting
│
└── 📄 Scripts/Data Layer: [scripts/CLAUDE.md](scripts/CLAUDE.md)
    ├── Deterministic Data Extraction (No LLM for correctness)
    ├── Batch Operations (500-doc limits)
    ├── Error Recovery
    └── Data Quality Validation

```

---

## 🎯 Quick Lookup by Scenario

### "I'm building a new feature screen"
**Start here:** [app/ui/CLAUDE.md](app/ui/CLAUDE.md)
- Feature state definition (sealed class UiState)
- ViewModel + repository injection pattern
- StateFlow<UiState> management
- UI composable decomposition
- Then read: [app/CLAUDE.md](app/CLAUDE.md) (ViewModel basics), [app/navigation/CLAUDE.md](app/navigation/CLAUDE.md) (routing)

### "I'm implementing a new use case"
**Start here:** [core/domain/CLAUDE.md](core/domain/CLAUDE.md)
- Use case structure (suspend function → Result<T>)
- Repository interfaces (SSOT)
- Error handling patterns
- Then read: [core/data/CLAUDE.md](core/data/CLAUDE.md) (repository impl)

### "I'm storing data (database or cache)"
**Start here:** [core/data/local/CLAUDE.md](core/data/local/CLAUDE.md) (for Room) OR [core/data/remote/CLAUDE.md](core/data/remote/CLAUDE.md) (for Firestore)
- Entity design & migrations
- DAO query patterns
- Caching with TTL
- Firebase security rules
- Transaction patterns
- Batch operations
- Then read: [core/data/CLAUDE.md](core/data/CLAUDE.md) (error handling)

### "I'm integrating AI (Gemini evaluation)"
**Start here:** [core/data/ai/CLAUDE.md](core/data/ai/CLAUDE.md)
- Gemini service setup (Hilt provider)
- Structured prompt engineering
- Response parsing & validation
- Rate limiting
- Error handling
- Testing with mocks
- Then read: [functions/CLAUDE.md](functions/CLAUDE.md) (backend scoring)

### "I'm building a reusable component"
**Start here:** [core/designsystem/CLAUDE.md](core/designsystem/CLAUDE.md)
- Component API design
- Material3 theming
- Accessibility (WCAG)
- Size limits (50 lines)
- **Requires @Preview annotation**
- Then read: [app/CLAUDE.md](app/CLAUDE.md) (Composable limits in screens)

### "I'm setting up dependency injection"
**Start here:** [app/di/CLAUDE.md](app/di/CLAUDE.md)
- Hilt module structure (@Module, @Provides)
- @HiltViewModel pattern
- Scopes (Singleton, Activity, NavGraph)
- Testing with @HiltAndroidTest
- Multi-binding

### "I'm setting up routing/navigation"
**Start here:** [app/navigation/CLAUDE.md](app/navigation/CLAUDE.md)
- SSBMaxDestinations (SSOT for routes)
- Type-safe routing
- Route parameters
- Navigation events (Channel-based)
- Backstack management
- Testing navigation flows

### "I'm writing a custom lint detector"
**Start here:** [lint/CLAUDE.md](lint/CLAUDE.md)
- Android Lint framework (UAST, Detectors)
- Creating new detectors (template)
- Detection patterns
- Testing detectors (LintTestCase)
- Build integration
- 14 existing detectors documented (Phase 2-4)

### "I'm building backend Cloud Functions"
**Start here:** [functions/CLAUDE.md](functions/CLAUDE.md)
- Security (auth checks first)
- Gemini integration (structured prompts)
- Firestore transactions
- Batch operations (500-doc limit)
- Error handling
- Rate limiting

### "I'm doing data ingestion/PDF extraction"
**Start here:** [scripts/CLAUDE.md](scripts/CLAUDE.md)
- Deterministic extraction (text layer, NOT vision model)
- Batch validation (HTML preview gate)
- Firestore batch ops with checkpoints
- Error recovery patterns
- Data quality checks
- LLM for enrichment only

---

## 📊 Statistics

| Module | File Count | Total Lines | Purpose |
|--------|-----------|------------|---------|
| **Root** | 1 | 400+ | Global patterns & rules |
| **app** | 4 | 1,200+ | UI layer (screens, DI, routing) |
| **core:domain** | 1 | 267 | Business logic (SSOT) |
| **core:data** | 4 | 1,400+ | Data layer (repositories, AI, DB, Firebase) |
| **core:designsystem** | 1 | 258 | Component library & theming |
| **lint** | 1 | 286 | Custom detectors (16+ rules) |
| **functions** | 1 | 281 | Backend Cloud Functions |
| **scripts** | 1 | 297 | Data ingestion & batch ops |
| **TOTAL** | **13 CLAUDE.md files** | **~4,800 lines** | Full development guidance |

---

## 🔗 Cross-Reference Links

**Frequently Referenced Patterns:**

| Pattern | Location | Used By |
|---------|----------|---------|
| Result<T> (error handling) | [core/domain/CLAUDE.md](core/domain/CLAUDE.md#result--sealed-class) | All data/use case code |
| @HiltViewModel | [app/di/CLAUDE.md](app/di/CLAUDE.md) | All feature screens |
| StateFlow<UiState> | [app/CLAUDE.md](app/CLAUDE.md) | All ViewModels |
| Repository Pattern | [core/data/CLAUDE.md](core/data/CLAUDE.md) | Repositories, use cases |
| Firestore Security Rules | [core/data/remote/CLAUDE.md](core/data/remote/CLAUDE.md#security-rules-ssot) | Firebase backend |
| SSBMaxDestinations | [app/navigation/CLAUDE.md](app/navigation/CLAUDE.md) | All routing |
| Gemini Prompts | [core/data/ai/CLAUDE.md](core/data/ai/CLAUDE.md) | Evaluation features |

---

## 📝 Update Frequency

| File | Update Frequency | Who Updates |
|------|-----------------|------------|
| Root `claude.md` | Quarterly (new phases/rules) | Tech Lead |
| Module CLAUDEs | Per-release (new patterns) | Module Owner |
| Sub-Module CLAUDEs | As-needed (pattern refinements) | Feature Leads |
| `CLAUDE_HIERARCHY.md` (this file) | With each new CLAUDE.md | Tech Lead |
| `GUIDELINES.md` | Annually (process improvements) | Team Lead |

---

## 🚀 Getting Started

1. **First time contributor?** Read [Root claude.md](claude.md) (12 core rules)
2. **Building a feature?** Jump to your module's CLAUDE.md above
3. **Not sure where to start?** Use the "Quick Lookup" section
4. **Want to add a new pattern?** See [GUIDELINES.md](GUIDELINES.md)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
