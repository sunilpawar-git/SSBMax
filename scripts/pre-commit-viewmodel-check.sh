#!/bin/bash
# Pre-commit hook: ViewModel Architecture Validation
# Blocks commits that violate ViewModel state management patterns
#
# Installation:
#   cp scripts/pre-commit-viewmodel-check.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit
#
# Or symlink it:
#   ln -s ../../scripts/pre-commit-viewmodel-check.sh .git/hooks/pre-commit

echo "🔍 Checking ViewModel architecture patterns..."

# Get list of staged ViewModel files
VIEWMODEL_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep "ViewModel.kt$")

if [ -z "$VIEWMODEL_FILES" ]; then
    echo "✅ No ViewModel files changed"
    exit 0
fi

VIOLATIONS=()

for file in $VIEWMODEL_FILES; do
    # Skip test files
    if [[ $file == *"/test/"* ]]; then
        continue
    fi
    
    # Check if file exists (could be deleted)
    if [ ! -f "$file" ]; then
        continue
    fi
    
    # Check 1: Detect "private var X: Job?" pattern
    if grep -q "private var.*: Job?" "$file"; then
        VIOLATIONS+=("❌ $file")
        VIOLATIONS+=("   Contains: private var X: Job?")
        VIOLATIONS+=("   Problem: Memory leak risk")
        VIOLATIONS+=("   Fix: Use viewModelScope.launch directly without storing Job")
        VIOLATIONS+=("")
    fi
    
    # Check 2: Detect nullable vars (except allowed types)
    while IFS= read -r line; do
        # Skip comments and allowed patterns
        if [[ $line == *//* ]] || \
           [[ $line == *"StateFlow"* ]] || \
           [[ $line == *"LiveData"* ]] || \
           [[ $line == *"Flow<"* ]] || \
           [[ $line == *"// DEPRECATED"* ]] || \
           [[ $line == *"// PHASE"* ]]; then
            continue
        fi
        
        # Check for nullable var pattern
        if [[ $line =~ private\ var\ [a-zA-Z0-9_]+:.*\? ]]; then
            VIOLATIONS+=("❌ $file")
            VIOLATIONS+=("   Line: $(echo "$line" | xargs)")
            VIOLATIONS+=("   Problem: Nullable mutable var in ViewModel")
            VIOLATIONS+=("   Fix: Move to StateFlow<UiState> for lifecycle safety")
            VIOLATIONS+=("")
            break  # One violation per file is enough for hook
        fi
    done < "$file"
done

if [ ${#VIOLATIONS[@]} -gt 0 ]; then
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════╗"
    echo "║  ❌ COMMIT BLOCKED: ViewModel Architecture Violations Detected    ║"
    echo "╚═══════════════════════════════════════════════════════════════════╝"
    echo ""
    printf '%s\n' "${VIOLATIONS[@]}"
    echo ""
    echo "📚 Required Pattern:"
    echo ""
    echo "❌ BAD:"
    echo "  private var timerJob: Job? = null"
    echo "  private var sessionId: String? = null"
    echo ""
    echo "✅ GOOD:"
    echo "  data class UiState("
    echo "      val sessionId: String? = null,"
    echo "      val isTimerActive: Boolean = false"
    echo "  )"
    echo "  private val _uiState = MutableStateFlow(UiState())"
    echo ""
    echo "🔧 To fix:"
    echo "  1. Move nullable vars to StateFlow<UiState>"
    echo "  2. Use viewModelScope.launch (don't store Job)"
    echo "  3. Run: ./gradle.sh lint (to see all violations)"
    echo ""
    echo "ℹ️  To bypass (NOT RECOMMENDED): git commit --no-verify"
    echo ""
    exit 1
fi

echo "✅ ViewModel architecture checks passed"
exit 0


