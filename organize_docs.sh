#!/bin/bash

# ================================================
# SSBMax Documentation Organization Script
# ================================================
# This script moves all scattered .md files into
# proper documentation directory structure
# ================================================

set -e

PROJECT_ROOT="/Users/sunil/Downloads/SSBMax"
DOCS_DIR="$PROJECT_ROOT/docs"

cd "$PROJECT_ROOT"

echo "📚 Starting documentation organization..."
echo "================================================"

# ============================================
# RELEASES
# ============================================
echo "📦 Moving release documentation..."
mv -f RELEASE_*.md "$DOCS_DIR/releases/" 2>/dev/null || true
mv -f DEPLOYMENT_*.md "$DOCS_DIR/releases/" 2>/dev/null || true
mv -f GIT_RELEASE_*.md "$DOCS_DIR/releases/" 2>/dev/null || true
mv -f GIT_COMMIT_*.md "$DOCS_DIR/releases/" 2>/dev/null || true

# ============================================
# IMPLEMENTATION
# ============================================
echo "🏗️  Moving implementation documentation..."
mv -f IMPLEMENTATION_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f *_COMPLETE.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f *_SUCCESS.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f FINAL_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f NAVGRAPH_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f UI_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f SUBSCRIPTION_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f MARKDOWN_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f CARD_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f STATS_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f TAGS_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f BREADCRUMB_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f TAB_STATE_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f STUDY_MATERIALS_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f LOADING_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f FORMATTING_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true

# ============================================
# TROUBLESHOOTING
# ============================================
echo "🐛 Moving troubleshooting documentation..."
mv -f *FIX*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f *ERROR*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f *DEBUG*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f AUTH_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f PROFILE_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f DRAWER_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f API_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f CHECK_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f CRITICAL_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f CORRECT_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f DUPLICATE_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f NEXT_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f ALTERNATIVE_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f TAT_TEST_KNOWN_ISSUES.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f OIR_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f CASE_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true

# ============================================
# ARCHITECTURE
# ============================================
echo "🏛️  Moving architecture documentation..."
mv -f ARCHITECTURE_*.md "$DOCS_DIR/architecture/" 2>/dev/null || true
mv -f LOCALCONTENTSOURCE_*.md "$DOCS_DIR/architecture/" 2>/dev/null || true
mv -f TECHNICAL_DEBT_*.md "$DOCS_DIR/architecture/" 2>/dev/null || true
mv -f CONTENT_MANAGEMENT_*.md "$DOCS_DIR/architecture/" 2>/dev/null || true

# ============================================
# TESTING
# ============================================
echo "🧪 Moving testing documentation..."
mv -f TEST*.md "$DOCS_DIR/testing/" 2>/dev/null || true
mv -f PHASE_*.md "$DOCS_DIR/testing/" 2>/dev/null || true
mv -f INTEGRATION_*.md "$DOCS_DIR/testing/" 2>/dev/null || true
mv -f ANDROID_STUDIO_TESTING_*.md "$DOCS_DIR/testing/" 2>/dev/null || true

# ============================================
# SECURITY
# ============================================
echo "🔒 Moving security documentation..."
mv -f SECURITY_*.md "$DOCS_DIR/security/" 2>/dev/null || true
mv -f URGENT_*.md "$DOCS_DIR/security/" 2>/dev/null || true
mv -f RULES_*.md "$DOCS_DIR/security/" 2>/dev/null || true
mv -f REQUIRED_APIS_*.md "$DOCS_DIR/security/" 2>/dev/null || true
mv -f SHA1_*.md "$DOCS_DIR/security/" 2>/dev/null || true
mv -f PRE_COMMIT_*.md "$DOCS_DIR/security/" 2>/dev/null || true

# ============================================
# FIREBASE
# ============================================
echo "🔥 Moving Firebase documentation..."
mv -f FIREBASE_*.md "$DOCS_DIR/firebase/" 2>/dev/null || true
mv -f FIRESTORE_*.md "$DOCS_DIR/firebase/" 2>/dev/null || true
mv -f CLOUD_*.md "$DOCS_DIR/firebase/" 2>/dev/null || true
mv -f GOOGLE_*.md "$DOCS_DIR/firebase/" 2>/dev/null || true

# ============================================
# MIGRATION
# ============================================
echo "🔄 Moving migration documentation..."
mv -f MIGRATION_*.md "$DOCS_DIR/migration/" 2>/dev/null || true
mv -f *_MIGRATION_*.md "$DOCS_DIR/migration/" 2>/dev/null || true
mv -f PPDT_*.md "$DOCS_DIR/migration/" 2>/dev/null || true
mv -f PSYCHOLOGY_*.md "$DOCS_DIR/migration/" 2>/dev/null || true
mv -f PIQ_*.md "$DOCS_DIR/migration/" 2>/dev/null || true

# ============================================
# STEP GUIDES
# ============================================
echo "📖 Moving step guides..."
mv -f STEP_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true

# ============================================
# BUILD DOCUMENTATION
# ============================================
echo "🔨 Moving build documentation..."
mv -f BUILD_*.md "$DOCS_DIR/implementation/" 2>/dev/null || true
mv -f IMPORTANT_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true

# ============================================
# GUIDES & REFERENCES
# ============================================
echo "📚 Moving guides and references..."
mv -f *_GUIDE.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true
mv -f *_REFERENCE.md "$DOCS_DIR/architecture/" 2>/dev/null || true
mv -f FIND_*.md "$DOCS_DIR/troubleshooting/" 2>/dev/null || true

# ============================================
# DEPRECATED DOCS FROM Docu/
# ============================================
echo "🗄️  Moving deprecated documentation..."
if [ -d "Docu" ]; then
    mv -f Docu "$DOCS_DIR/deprecated/" 2>/dev/null || true
fi

echo ""
echo "✅ Documentation organization complete!"
echo "================================================"
echo ""
echo "📊 Summary:"
echo "  - Root .md files moved to /docs/"
echo "  - Organized into categories"
echo "  - Old Docu/ folder moved to /docs/deprecated/"
echo ""
echo "📝 Next steps:"
echo "  1. Review /docs/ structure"
echo "  2. Update .gitignore if needed"
echo "  3. Commit organized documentation"
echo ""

