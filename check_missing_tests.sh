#!/bin/bash

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 VIEWMODEL TEST COVERAGE ANALYSIS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Find all ViewModels
VIEWMODELS=$(find app/src/main/kotlin -name "*ViewModel.kt" -type f | sort)
TOTAL_VMS=$(echo "$VIEWMODELS" | wc -l | tr -d ' ')

echo "Total ViewModels: $TOTAL_VMS"
echo ""

# Check for missing tests
MISSING=0
MISSING_LIST=""

while IFS= read -r vm_file; do
    # Extract ViewModel name without path and extension
    vm_name=$(basename "$vm_file" .kt)
    
    # Construct expected test path
    test_file="${vm_file/src\/main\/kotlin/src\/test\/kotlin}"
    test_file="${test_file/.kt/Test.kt}"
    
    if [ ! -f "$test_file" ]; then
        MISSING=$((MISSING + 1))
        MISSING_LIST="${MISSING_LIST}${vm_name}\n"
    fi
done <<< "$VIEWMODELS"

COVERED=$((TOTAL_VMS - MISSING))
COVERAGE=$((COVERED * 100 / TOTAL_VMS))

echo "✅ ViewModels with tests: $COVERED"
echo "❌ ViewModels missing tests: $MISSING"
echo "📊 Test coverage: ${COVERAGE}%"
echo ""

if [ $MISSING -gt 0 ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "❌ ViewModels MISSING Tests:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "$MISSING_LIST" | sort
else
    echo "🎉 ALL VIEWMODELS HAVE TESTS!"
fi
