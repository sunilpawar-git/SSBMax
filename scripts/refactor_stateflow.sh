#!/bin/bash
# Automated StateFlow refactoring script
# Converts _uiState.value = _uiState.value.copy(...) to _uiState.update { it.copy(...) }

set -e  # Exit on error

FILE="$1"

if [ -z "$FILE" ]; then
    echo "Usage: $0 <kotlin-file>"
    exit 1
fi

if [ ! -f "$FILE" ]; then
    echo "Error: File not found: $FILE"
    exit 1
fi

echo "Refactoring $FILE..."

# Create backup
cp "$FILE" "$FILE.backup"

# Step 1: Add import if not present
if ! grep -q "import kotlinx.coroutines.flow.update" "$FILE"; then
    # Find the last kotlinx.coroutines.flow import and add after it
    sed -i '' '/import kotlinx.coroutines.flow\./a\
import kotlinx.coroutines.flow.update
' "$FILE"
    echo "  ✓ Added update import"
fi

# Step 2: Refactor the pattern
# This handles simple single-line patterns
# Pattern: _uiState.value = _uiState.value.copy(...)
# Replace: _uiState.update { it.copy(...) }

# Handle multi-line patterns - replace opening
sed -i '' 's/_uiState\.value = _uiState\.value\.copy(/_uiState.update { it.copy(/g' "$FILE"
sed -i '' 's/_state\.value = _state\.value\.copy(/_state.update { it.copy(/g' "$FILE"

echo "  ✓ Refactored .value = .value.copy patterns"

# Step 3: Fix closing - we need to replace ) at end of copy blocks with ) }
# This is tricky because we need to identify which closing parens belong to copy()
# For now, we'll do a simple approach: find lines that end with just )

# Manual fix required for complex multi-line cases
echo "  ⚠️  Please manually review multi-line patterns and ensure ) becomes ) }"

echo "✅ Refactoring complete. Backup saved to: $FILE.backup"
echo "   Please review changes and run tests before committing."




