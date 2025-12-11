#!/bin/bash
# Setup script to install Git hooks for security validation
# Run this script once after cloning the repository

set -e

echo "🔧 Setting up Git hooks for SSBMax..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOOKS_SOURCE="$PROJECT_ROOT/.githooks"
HOOKS_TARGET="$PROJECT_ROOT/.git/hooks"

echo ""
echo -e "${BLUE}Project root: $PROJECT_ROOT${NC}"
echo -e "${BLUE}Hooks source: $HOOKS_SOURCE${NC}"
echo -e "${BLUE}Hooks target: $HOOKS_TARGET${NC}"
echo ""

# Check if .git directory exists
if [ ! -d "$PROJECT_ROOT/.git" ]; then
    echo "❌ ERROR: .git directory not found!"
    echo "Make sure you're running this from the project root."
    exit 1
fi

# Check if .githooks directory exists
if [ ! -d "$HOOKS_SOURCE" ]; then
    echo "❌ ERROR: .githooks directory not found!"
    echo "Make sure .githooks directory exists with hook scripts."
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_TARGET"

# Install pre-commit hook
if [ -f "$HOOKS_SOURCE/pre-commit" ]; then
    echo "📝 Installing pre-commit hook..."
    cp "$HOOKS_SOURCE/pre-commit" "$HOOKS_TARGET/pre-commit"
    chmod +x "$HOOKS_TARGET/pre-commit"
    echo -e "${GREEN}✅ pre-commit hook installed${NC}"
else
    echo -e "${YELLOW}⚠️  pre-commit hook not found in .githooks${NC}"
fi

# Install commit-msg hook
if [ -f "$HOOKS_SOURCE/commit-msg" ]; then
    echo "📝 Installing commit-msg hook..."
    cp "$HOOKS_SOURCE/commit-msg" "$HOOKS_TARGET/commit-msg"
    chmod +x "$HOOKS_TARGET/commit-msg"
    echo -e "${GREEN}✅ commit-msg hook installed${NC}"
else
    echo -e "${YELLOW}⚠️  commit-msg hook not found in .githooks${NC}"
fi

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}✅  Git hooks setup complete!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "The following hooks are now active:"
echo "  • pre-commit  - Validates no API keys/secrets in staged files"
echo "  • commit-msg  - Validates no secrets in commit messages"
echo ""
echo "These hooks will run automatically on each commit."
echo ""
echo "To bypass hooks (NOT RECOMMENDED):"
echo "  git commit --no-verify"
echo ""
echo -e "${BLUE}Happy coding! 🚀${NC}"
