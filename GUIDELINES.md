# GUIDELINES.md — Using and Maintaining CLAUDE.md Files

**Process guide:** How to use, maintain, and evolve the CLAUDE.md architecture guidance system.

---

## 📖 What is this system?

The CLAUDE.md hierarchy is a **living documentation system** for development patterns:

- **NOT** a style guide (Ktlint handles code style)
- **NOT** API docs (KDoc handles that)
- **IS** a patterns library: "here's HOW we do things"
- **IS** enforced by lint + git hooks: patterns matter

13 files across 3 hierarchy levels guide decisions at every layer:

1. **Root** — 12 core rules (apply everywhere)
2. **Modules** — 7 files covering app, domain, data, designsystem, lint, functions, scripts
3. **Sub-modules** — 6 files with specialized patterns (UI, DI, Navigation, AI, Local DB, Remote DB)

---

## 🔍 How to Use CLAUDE.md Files

### During Development

1. **Starting a task?** Open the relevant CLAUDE.md:
   - Building a feature screen → [app/ui/CLAUDE.md](app/ui/CLAUDE.md)
   - Implementing a use case → [core/domain/CLAUDE.md](core/domain/CLAUDE.md)
   - Adding a database → [core/data/local/CLAUDE.md](core/data/local/CLAUDE.md)
   - Building a component → [core/designsystem/CLAUDE.md](core/designsystem/CLAUDE.md)

2. **Stuck on a pattern?** Check the relevant file's code examples:
   ```kotlin
   // Good: From StateFlow patterns
   @HiltViewModel
   class MyViewModel @Inject constructor(...) : ViewModel() {
     private val _uiState = MutableStateFlow<UiState>(...)
     val uiState: StateFlow<UiState> = _uiState.asStateFlow()
   }
   
   // Bad: Direct assignment (not thread-safe)
   _uiState = UiState(...) // ❌ Breaks StateFlow contract
   ```

3. **Before committing?** Pre-commit hook checks module-specific rules automatically:
   ```bash
   $ git commit
   # Hook checks:
   # - core:domain has no Android imports
   # - app layer avoids Firebase (except Auth)
   # - designsystem Composables have @Preview
   # - No hardcoded secrets
   ```

### During Code Review

1. **Spot a pattern violation?** Reference the CLAUDE.md file:
   > "This imports Firebase directly in the ViewModel. See [app/CLAUDE.md — Rule 6: No Firebase in UI](app/CLAUDE.md#rule-6-no-firebase-in-ui-layer). Use repository pattern instead."

2. **Unsure if a pattern is correct?** Check the relevant file's "Best Practices" section
   ```markdown
   # Best Practices
   1. Use structured prompts → consistent outputs
   2. Parse JSON robustly → handle markdown wrappers
   3. Wrap errors in Result<T> → no exceptions leaking
   ```

3. **Testing strategy unclear?** Each CLAUDE.md has a "Testing" section:
   - Mock strategies
   - Test patterns (JUnit 4, Turbine, etc.)
   - InMemoryDb testing
   - LintTestCase for detectors

---

## 🛠️ How to Maintain CLAUDE.md Files

### Adding a New Pattern

**When:** You discover a repeatable pattern that 2+ developers ask about.

**Steps:**

1. **Identify which CLAUDE.md** needs the new pattern (use CLAUDE_HIERARCHY.md)

2. **Add to "Best Practices" section** with:
   - Problem statement (why does this matter?)
   - Code example (good + bad)
   - Explanation (what happens if violated?)

3. **Update line count** (keep files 200-300 lines):
   - If adding pattern pushes file >300 lines → create new sub-module CLAUDE.md

4. **Cross-reference** if it connects to other patterns:
   ```markdown
   # References
   - **Parent:** [core/domain/CLAUDE.md](../CLAUDE.md)
   - **Related:** [core/data/remote/CLAUDE.md](./remote/CLAUDE.md)
   ```

5. **Increment version** (in file footer):
   ```markdown
   **Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
   ```

### Proposing a New Module-Level CLAUDE.md

**When:** Adding a new module (e.g., `analytics`, `testing`)

**Steps:**

1. **Create file** in module root: `analytics/CLAUDE.md`

2. **Follow template:**
   ```markdown
   # analytics/CLAUDE.md — [Purpose]
   
   **Scope:** [What this module handles]
   **Inherits:** [Parent CLAUDE.md link]
   
   ---
   
   ## [Major Pattern 1]
   
   [Code examples + explanation]
   
   ---
   
   ## Best Practices
   
   1. [Practice 1]
   2. [Practice 2]
   
   ---
   
   ## References
   
   - **Parent:** [../claude.md](../claude.md)
   
   ---
   
   **Last Updated:** June 2026 | **Maintainer:** [Your Name]
   ```

3. **Update CLAUDE_HIERARCHY.md** to include new module

4. **Register in root claude.md** (see next section)

5. **Add lint enforcement** if applicable (new detectors in lint/ module)

### Updating Root claude.md

**When:** Major architectural change or new phases completed.

**File location:** `/Users/sunil/Downloads/SSBMax/claude.md`

**Update these sections:**

1. **"What Is SSBMax"** — if domain changed
2. **"12 Core Rules"** — if adding new rules
3. **"Guiding Principles"** — if architecture patterns change
4. **"Project Architecture"** — if module structure changed
5. **"Mandatory Lint Rules"** — if adding new lint detectors

**Example:**
```markdown
# Before (Phase 2)
Mandatory Lint Rules:
1. ErrorLogger, not printStackTrace
2. NO hardcoded strings
3. Thread-safe StateFlow updates

# After (Phase 4)
Mandatory Lint Rules:
1. ErrorLogger, not printStackTrace
2. NO hardcoded strings
3. Thread-safe StateFlow updates
4. No Firebase in app layer ← NEW
5. No Android deps in domain ← NEW
6. Designsystem Composables @Preview ← NEW
```

---

## 🔄 Review Process for CLAUDE.md Changes

### Pre-Commit
✅ Pre-commit hook validates:
- Module-specific rules (no Android in domain, etc.)
- No hardcoded secrets
- CLAUDE.md file paths exist

### Pull Request
✅ Code review checks:
1. **Does this PR follow CLAUDE.md patterns?** (Reviewer references relevant files)
2. **Should a pattern be documented?** (If this is repeatable, add to CLAUDE.md)
3. **Are there pattern violations?** (Lint should catch most; manual review for edge cases)

✅ Automated checks:
```bash
./gradlew :lint:test              # All 16 detectors pass
./gradlew lintDebug               # No linter violations
./gradlew testDebugUnitTest       # Tests pass
```

### Post-Merge
✅ Release process:
1. Run full test suite: `./gradlew check`
2. Document patterns used in this release
3. Update relevant CLAUDE.md files (if patterns added/changed)
4. Increment phase (if major patterns added)

---

## 🚨 Anti-Patterns & Violations

### Severity Levels

| Severity | Example | Detection | Action |
|----------|---------|-----------|--------|
| **CRITICAL** | Hardcoded API key | Lint (pre-commit) | Commit BLOCKED |
| **ERROR** | Android import in core:domain | Lint (pre-commit) | Commit BLOCKED |
| **WARNING** | Missing @Preview on component | Hook (advisory) | Commit ALLOWED (warning) |
| **INFO** | Unused import | Lint (info-level) | Commit ALLOWED (no check) |

### Common Violations

**❌ Importing Firebase directly in ViewModel:**
```kotlin
// WRONG
@HiltViewModel
class TATViewModel @Inject constructor() : ViewModel() {
  private val firestore = FirebaseFirestore.getInstance()
  
  fun loadQuestions() {
    firestore.collection("tat_questions").get() // ❌ Direct Firebase
  }
}

// ✅ RIGHT
@HiltViewModel
class TATViewModel @Inject constructor(
  private val tatRepository: TATRepository // ✅ Inject interface
) : ViewModel() {
  
  fun loadQuestions() {
    val result = tatRepository.getTATQuestions()
  }
}
```

**❌ Android imports in core:domain:**
```kotlin
// WRONG (core:domain)
import android.util.Log

class GetQuestionsUseCase {
  fun execute() {
    Log.d("TAG", "Getting questions") // ❌ Android dependency
  }
}

// ✅ RIGHT (core:domain)
class GetQuestionsUseCase @Inject constructor(
  private val questionRepository: QuestionRepository
) {
  suspend fun execute(): Result<List<Question>> {
    return try {
      Result.Success(questionRepository.getAll())
    } catch (e: Exception) {
      Result.Failure(e)
    }
  }
}
```

**❌ Missing @Preview on designsystem component:**
```kotlin
// WRONG (core:designsystem)
@Composable
fun CustomCard(title: String, content: String) {
  // No @Preview ❌
}

// ✅ RIGHT
@Composable
fun CustomCard(title: String, content: String) {
  // Component body
}

@Preview
@Composable
fun CustomCardPreview() {
  CustomCard(title = "Title", content = "Content")
}
```

---

## 🎯 Decision-Making Framework

When facing a design decision:

1. **Check relevant CLAUDE.md** — is there a pattern?
2. **If yes** — follow it (consistency > personal preference)
3. **If no** — document your decision + add new pattern
4. **If conflict** — escalate to Tech Lead (patterns evolve)

**Example:**
```
Q: Should I pass the entire User object to the next screen, or just userId?

A: Check [app/navigation/CLAUDE.md](app/navigation/CLAUDE.md)
   → See "ID-based navigation only" pattern
   → Pass userId, not User object
   → Why? Keeps screens decoupled, reduces bundle size, easier testing
```

---

## 📋 Checklist: Before Creating a PR

- [ ] I read the relevant CLAUDE.md file(s)
- [ ] My code follows the documented patterns
- [ ] No linter violations (`./gradlew lintDebug`)
- [ ] Tests pass (`./gradlew testDebugUnitTest`)
- [ ] Pre-commit hook passes (run `git commit`)
- [ ] If I found a missing pattern, I documented it
- [ ] I updated CLAUDE.md if adding a new pattern
- [ ] I linked to relevant CLAUDE.md sections in my PR description

---

## 🔗 Quick Links

| Document | Purpose |
|----------|---------|
| [CLAUDE_HIERARCHY.md](CLAUDE_HIERARCHY.md) | Navigation guide (find the right file) |
| [claude.md](claude.md) | Root rules & architecture |
| [app/CLAUDE.md](app/CLAUDE.md) | UI layer patterns |
| [core/domain/CLAUDE.md](core/domain/CLAUDE.md) | Business logic SSOT |
| [core/data/CLAUDE.md](core/data/CLAUDE.md) | Data layer patterns |
| [lint/CLAUDE.md](lint/CLAUDE.md) | Custom lint detector patterns |
| [functions/CLAUDE.md](functions/CLAUDE.md) | Backend Cloud Functions |

---

## 📞 Questions?

- **Pattern question?** Ask in PR comments (reference CLAUDE.md file)
- **Should I document this?** Open a discussion with the Tech Lead
- **CLAUDE.md needs update?** File an issue or submit a PR with the change

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar | **Next Review:** Q3 2026
