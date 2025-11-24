#!/bin/bash
# Manual security validation script
# Run this script to perform a comprehensive security audit of the codebase

set -e

echo "🔒 SSBMax Security Validation Script"
echo "===================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# ============================================
# 1. Check for API keys in tracked files
# ============================================
echo -e "${BLUE}[1/8] Checking for API keys in tracked files...${NC}"

TRACKED_FILES=$(git ls-files)

if echo "$TRACKED_FILES" | xargs grep -l "AIza[A-Za-z0-9_-]\{35\}" 2>/dev/null; then
    echo -e "${RED}❌ CRITICAL: Gemini API key found in tracked files!${NC}"
    echo ""
    echo "Files containing API keys:"
    echo "$TRACKED_FILES" | xargs grep -n "AIza[A-Za-z0-9_-]\{35\}" 2>/dev/null
    echo ""
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ No API keys in tracked files${NC}"
fi

echo ""

# ============================================
# 2. Check for sensitive files in git
# ============================================
echo -e "${BLUE}[2/8] Checking for sensitive files in repository...${NC}"

SENSITIVE_FILES=(
    "local.properties"
    "functions/.env"
    "functions/.env.local"
    ".env"
    "google-services.json"
    "app/google-services.json"
    "service-account.json"
    "firebase-admin-key.json"
)

FOUND_SENSITIVE=0
for file in "${SENSITIVE_FILES[@]}"; do
    if git ls-files | grep -q "^${file}$"; then
        echo -e "${RED}❌ Sensitive file is tracked: $file${NC}"
        FOUND_SENSITIVE=1
        ERRORS=$((ERRORS + 1))
    fi
done

if [ $FOUND_SENSITIVE -eq 0 ]; then
    echo -e "${GREEN}✅ No sensitive files tracked${NC}"
fi

echo ""

# ============================================
# 3. Validate .gitignore
# ============================================
echo -e "${BLUE}[3/8] Validating .gitignore configuration...${NC}"

if [ ! -f ".gitignore" ]; then
    echo -e "${RED}❌ .gitignore file not found!${NC}"
    ERRORS=$((ERRORS + 1))
else
    REQUIRED_PATTERNS=(
        "local.properties"
        ".env"
        "google-services.json"
        "service-account.json"
    )

    MISSING=0
    for pattern in "${REQUIRED_PATTERNS[@]}"; do
        if ! grep -q "$pattern" .gitignore; then
            echo -e "${YELLOW}⚠️  .gitignore missing pattern: $pattern${NC}"
            MISSING=1
            WARNINGS=$((WARNINGS + 1))
        fi
    done

    if [ $MISSING -eq 0 ]; then
        echo -e "${GREEN}✅ .gitignore properly configured${NC}"
    fi
fi

echo ""

# ============================================
# 4. Check local files for API keys (not tracked)
# ============================================
echo -e "${BLUE}[4/8] Checking local configuration files...${NC}"

LOCAL_FILES=(
    "local.properties"
    "functions/.env"
)

FOUND_LOCAL_KEYS=0
for file in "${LOCAL_FILES[@]}"; do
    if [ -f "$file" ]; then
        if grep -q "AIza[A-Za-z0-9_-]\{35\}" "$file" 2>/dev/null; then
            # Check if it's using a placeholder
            if grep -q "YOUR_GEMINI_API_KEY_HERE\|your_gemini_api_key_here\|test_key" "$file" 2>/dev/null; then
                echo -e "${YELLOW}⚠️  $file uses placeholder API key${NC}"
                WARNINGS=$((WARNINGS + 1))
            else
                echo -e "${GREEN}✅ $file contains API key (git-ignored)${NC}"
                FOUND_LOCAL_KEYS=1
            fi
        else
            echo -e "${YELLOW}⚠️  $file exists but has no API key${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
    fi
done

if [ $FOUND_LOCAL_KEYS -eq 0 ]; then
    echo -e "${YELLOW}ℹ️  No local API keys found (this is OK if using Firebase config)${NC}"
fi

echo ""

# ============================================
# 5. Check for hardcoded secrets in code
# ============================================
echo -e "${BLUE}[5/8] Scanning source code for hardcoded secrets...${NC}"

# Kotlin/Java files
if git ls-files | grep -E '\.(kt|java)$' | xargs grep -E 'apiKey\s*=\s*["\x27]AIza' 2>/dev/null; then
    echo -e "${RED}❌ Hardcoded API key in Kotlin/Java code${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ No hardcoded API keys in Kotlin/Java${NC}"
fi

# JavaScript/TypeScript files
if git ls-files | grep -E '\.(js|ts)$' | grep -v 'node_modules' | xargs grep -E 'apiKey\s*=\s*["\x27]AIza' 2>/dev/null; then
    echo -e "${RED}❌ Hardcoded API key in JavaScript/TypeScript${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ No hardcoded API keys in JavaScript/TypeScript${NC}"
fi

echo ""

# ============================================
# 6. Check for anti-pattern files
# ============================================
echo -e "${BLUE}[6/8] Checking for anti-pattern files...${NC}"

HOLDER_FILES=$(git ls-files | grep -E 'Holder\.kt$' || true)

if [ -n "$HOLDER_FILES" ]; then
    echo -e "${RED}❌ Found *Holder.kt files (anti-pattern):${NC}"
    echo "$HOLDER_FILES"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ No anti-pattern *Holder.kt files${NC}"
fi

echo ""

# ============================================
# 7. Check Git hooks installation
# ============================================
echo -e "${BLUE}[7/8] Checking Git hooks installation...${NC}"

HOOKS=("pre-commit" "commit-msg")
ALL_HOOKS_INSTALLED=1

for hook in "${HOOKS[@]}"; do
    if [ -x ".git/hooks/$hook" ]; then
        echo -e "${GREEN}✅ $hook hook installed${NC}"
    else
        echo -e "${YELLOW}⚠️  $hook hook not installed${NC}"
        echo "   Run: ./scripts/setup-git-hooks.sh"
        ALL_HOOKS_INSTALLED=0
        WARNINGS=$((WARNINGS + 1))
    fi
done

echo ""

# ============================================
# 8. Check Firebase Functions configuration
# ============================================
echo -e "${BLUE}[8/8] Checking Firebase Functions configuration...${NC}"

if [ -d "functions" ]; then
    if [ -f "functions/.env.example" ]; then
        echo -e "${GREEN}✅ functions/.env.example exists${NC}"
    else
        echo -e "${YELLOW}⚠️  functions/.env.example not found${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi

    if [ -f "functions/.env" ]; then
        if git check-ignore -q functions/.env; then
            echo -e "${GREEN}✅ functions/.env is git-ignored${NC}"
        else
            echo -e "${RED}❌ functions/.env is NOT git-ignored!${NC}"
            ERRORS=$((ERRORS + 1))
        fi
    fi
fi

echo ""

# ============================================
# Final Summary
# ============================================
echo ""
echo "============================================"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅  PERFECT! All security checks passed!${NC}"
    echo "============================================"
    echo ""
    echo "Your codebase is secure:"
    echo "  ✅ No API keys in tracked files"
    echo "  ✅ No sensitive files in git"
    echo "  ✅ .gitignore properly configured"
    echo "  ✅ No hardcoded secrets in code"
    echo "  ✅ Git hooks installed"
    echo ""
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️   Security checks passed with $WARNINGS warning(s)${NC}"
    echo "============================================"
    echo ""
    echo "Review warnings above and fix if necessary."
    echo ""
else
    echo -e "${RED}❌  SECURITY ISSUES FOUND: $ERRORS error(s), $WARNINGS warning(s)${NC}"
    echo "============================================"
    echo ""
    echo "CRITICAL: Fix all errors before committing!"
    echo ""
    exit 1
fi

exit 0
