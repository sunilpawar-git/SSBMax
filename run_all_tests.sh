#!/bin/bash

# SSBMax Test Runner Script
# Runs all tests across all modules and reports exact counts per module

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test results storage (using arrays compatible with bash 3.2+ and zsh)
MODULE_NAMES=()
MODULE_PASSED=()
MODULE_FAILED=()
MODULE_IGNORED=()
MODULE_TOTAL=()
MODULE_TIME=()

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}SSBMax Test Suite Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if --clean flag is provided
CLEAN_BUILD=false
if [[ "$1" == "--clean" ]] || [[ "$1" == "-c" ]]; then
    CLEAN_BUILD=true
fi

# Clean build if requested
if [ "$CLEAN_BUILD" = true ]; then
    echo -e "${CYAN}Cleaning build cache...${NC}"
    ./gradlew clean --no-daemon > /dev/null 2>&1 || true
    echo ""
fi

# Function to extract test counts from Gradle output
extract_test_counts() {
    local output="$1"
    
    # Try multiple patterns to extract test counts
    local passed=0
    local failed=0
    local ignored=0
    local total=0
    
    # Pattern 1: "X tests completed, Y failed, Z skipped"
    local completed_line=$(echo "$output" | grep -E "[0-9]+ tests? completed" | tail -1)
    if [ -n "$completed_line" ]; then
        total=$(echo "$completed_line" | grep -oE "^[0-9]+" || echo "0")
        failed=$(echo "$completed_line" | grep -oE "[0-9]+ failed" | grep -oE "[0-9]+" || echo "0")
        ignored=$(echo "$completed_line" | grep -oE "[0-9]+ (skipped|ignored)" | grep -oE "[0-9]+" || echo "0")
        passed=$((total - failed - ignored))
    fi
    
    # Pattern 2: "X tests passed, Y failed, Z ignored"
    if [ "$total" == "0" ]; then
        local summary_line=$(echo "$output" | grep -E "[0-9]+ tests? (passed|failed|ignored)" | tail -1)
        if [ -n "$summary_line" ]; then
            passed=$(echo "$summary_line" | grep -oE "[0-9]+ tests? passed" | grep -oE "[0-9]+" || echo "0")
            failed=$(echo "$summary_line" | grep -oE "[0-9]+ tests? failed" | grep -oE "[0-9]+" || echo "0")
            ignored=$(echo "$summary_line" | grep -oE "[0-9]+ tests? (skipped|ignored)" | grep -oE "[0-9]+" || echo "0")
            total=$((passed + failed + ignored))
        fi
    fi
    
    # Pattern 3: Look for test report summary
    if [ "$total" == "0" ]; then
        local report_summary=$(echo "$output" | grep -E "tests? (completed|passed|failed)" | tail -1)
        if [ -n "$report_summary" ]; then
            # Try to extract numbers
            local numbers=($(echo "$report_summary" | grep -oE "[0-9]+"))
            if [ ${#numbers[@]} -ge 1 ]; then
                total=${numbers[0]}
                if [ ${#numbers[@]} -ge 2 ]; then
                    failed=${numbers[1]}
                fi
                if [ ${#numbers[@]} -ge 3 ]; then
                    ignored=${numbers[2]}
                fi
                passed=$((total - failed - ignored))
            fi
        fi
    fi
    
    echo "$passed|$failed|$ignored|$total"
}

# Function to run tests for a module
run_module_tests() {
    local task="$1"
    local module_name="$2"
    
    echo -e "${YELLOW}Running: ${module_name}${NC}"
    echo -e "${CYAN}  Command: ./gradlew ${task}${NC}"
    
    local start_time=$(date +%s)
    local output=$(./gradlew ${task} --no-daemon 2>&1 || true)
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # Check if task was UP-TO-DATE (cached)
    if echo "$output" | grep -q "UP-TO-DATE"; then
        echo -e "${CYAN}  ⚠ Tests were cached (UP-TO-DATE). Use --clean to force fresh run.${NC}"
        echo ""
        return
    fi
    
    local counts=$(extract_test_counts "$output")
    IFS='|' read -r passed failed ignored total <<< "$counts"
    
    # Store results in arrays
    MODULE_NAMES+=("$module_name")
    MODULE_PASSED+=($passed)
    MODULE_FAILED+=($failed)
    MODULE_IGNORED+=($ignored)
    MODULE_TOTAL+=($total)
    MODULE_TIME+=("${duration}s")
    
    if [ "$total" -eq 0 ]; then
        echo -e "${YELLOW}  ⚠ No test results found (may be cached or no tests exist)${NC}"
    elif [ "$failed" -gt 0 ]; then
        echo -e "${RED}  ✗ ${total} tests: ${passed} passed, ${failed} failed, ${ignored} ignored (${duration}s)${NC}"
    else
        echo -e "${GREEN}  ✓ ${total} tests: ${passed} passed, ${failed} failed, ${ignored} ignored (${duration}s)${NC}"
    fi
    echo ""
}

# Run tests for each module
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Running Unit Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# App module
run_module_tests ":app:testDebugUnitTest" "app (debug unit)"
run_module_tests ":app:testReleaseUnitTest" "app (release unit)"

# Core: Data module
run_module_tests ":core:data:testDebugUnitTest" "core:data (debug unit)"
run_module_tests ":core:data:testReleaseUnitTest" "core:data (release unit)"

# Core: Domain module
run_module_tests ":core:domain:testDebugUnitTest" "core:domain (debug unit)"
run_module_tests ":core:domain:testReleaseUnitTest" "core:domain (release unit)"

# Core: DesignSystem module
run_module_tests ":core:designsystem:testDebugUnitTest" "core:designsystem (debug unit)"
run_module_tests ":core:designsystem:testReleaseUnitTest" "core:designsystem (release unit)"

# Core: Common module
run_module_tests ":core:common:testDebugUnitTest" "core:common (debug unit)"
run_module_tests ":core:common:testReleaseUnitTest" "core:common (release unit)"

# Lint module
run_module_tests ":lint:test" "lint"

# AndroidTest (if device connected)
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Android Instrumentation Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if command -v adb > /dev/null 2>&1 && adb devices 2>/dev/null | grep -q "device$"; then
    echo -e "${GREEN}Android device detected, running instrumentation tests...${NC}"
    run_module_tests ":app:connectedAndroidTest" "app (androidTest)"
else
    echo -e "${YELLOW}No Android device connected, skipping instrumentation tests${NC}"
    echo ""
fi

# Print summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

total_passed=0
total_failed=0
total_ignored=0
total_tests=0
modules_with_tests=0

printf "%-45s %8s %8s %8s %8s %10s\n" "Module" "Passed" "Failed" "Ignored" "Total" "Time"
echo "--------------------------------------------------------------------------------"

# Iterate through arrays by index
for i in "${!MODULE_NAMES[@]}"; do
    module="${MODULE_NAMES[$i]}"
    passed="${MODULE_PASSED[$i]:-0}"
    failed="${MODULE_FAILED[$i]:-0}"
    ignored="${MODULE_IGNORED[$i]:-0}"
    total="${MODULE_TOTAL[$i]:-0}"
    time="${MODULE_TIME[$i]:-N/A}"
    
    if [ "$total" -gt 0 ]; then
        modules_with_tests=$((modules_with_tests + 1))
        if [ "$failed" -gt 0 ]; then
            printf "${RED}%-45s %8s %8s %8s %8s %10s${NC}\n" "$module" "$passed" "$failed" "$ignored" "$total" "$time"
        else
            printf "${GREEN}%-45s %8s %8s %8s %8s %10s${NC}\n" "$module" "$passed" "$failed" "$ignored" "$total" "$time"
        fi
        
        total_passed=$((total_passed + passed))
        total_failed=$((total_failed + failed))
        total_ignored=$((total_ignored + ignored))
        total_tests=$((total_tests + total))
    fi
done

echo "--------------------------------------------------------------------------------"
if [ "$total_tests" -gt 0 ]; then
    if [ "$total_failed" -gt 0 ]; then
        printf "${RED}%-45s %8s %8s %8s %8s${NC}\n" "TOTAL" "$total_passed" "$total_failed" "$total_ignored" "$total_tests"
    else
        printf "${GREEN}%-45s %8s %8s %8s %8s${NC}\n" "TOTAL" "$total_passed" "$total_failed" "$total_ignored" "$total_tests"
    fi
else
    echo -e "${YELLOW}No test results found. Tests may be cached.${NC}"
    echo -e "${YELLOW}Run with --clean flag to force fresh test execution:${NC}"
    echo -e "${CYAN}  ./run_all_tests.sh --clean${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
if [ "$total_tests" -eq 0 ]; then
    echo -e "${YELLOW}No tests executed (likely cached). Use --clean to force run.${NC}"
elif [ "$total_failed" -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo -e "${GREEN}  Total: ${total_tests} tests (${total_passed} passed, ${total_ignored} ignored)${NC}"
else
    echo -e "${RED}✗ Some tests failed!${NC}"
    echo -e "${RED}  Total: ${total_tests} tests (${total_passed} passed, ${total_failed} failed, ${total_ignored} ignored)${NC}"
fi
echo -e "${BLUE}========================================${NC}"

# Exit with error code if any tests failed
if [ "$total_failed" -gt 0 ]; then
    exit 1
fi

exit 0
