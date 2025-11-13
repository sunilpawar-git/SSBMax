#!/bin/bash
# Tech Debt Remediation - Content File Deletion Script
# Run this AFTER Firestore migration is fully validated

echo "⚠️  WARNING: This will delete temporary content files that have been migrated to Firestore"
echo "📋 Files to be deleted:"
echo ""
echo "Study Material Content Files (10 files):"
echo "  - PsychologyMaterialContent.kt"
echo "  - PsychologyMaterialContent2.kt"
echo "  - InterviewMaterialContent.kt"
echo "  - InterviewMaterialContent2.kt"
echo "  - GTOMaterialContent.kt"
echo "  - GTOMaterialContent2.kt"
echo "  - PPDTMaterialContent.kt"
echo "  - PPDTMaterialContent2.kt"
echo "  - OIRMaterialContent.kt"
echo "  - OIRMaterialContent2.kt"
echo ""
echo "Content Provider Files (3 files):"
echo "  - StudyMaterialContentProvider.kt"
echo "  - SSBContentProvider.kt"
echo "  - FAQContentProvider.kt"
echo ""
echo "Total: 13 files (~11,032 lines of code)"
echo ""
read -p "Are you sure you want to delete these files? (yes/no): " confirmation

if [ "$confirmation" != "yes" ]; then
    echo "❌ Deletion cancelled"
    exit 1
fi

echo ""
echo "🗑️  Deleting temporary content files..."

# Study Material Content Files
rm -v app/src/main/kotlin/com/ssbmax/ui/study/PsychologyMaterialContent.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/PsychologyMaterialContent2.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/InterviewMaterialContent.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/InterviewMaterialContent2.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/GTOMaterialContent.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/GTOMaterialContent2.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/PPDTMaterialContent.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/PPDTMaterialContent2.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/OIRMaterialContent.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/study/OIRMaterialContent2.kt

# Content Provider Files
rm -v app/src/main/kotlin/com/ssbmax/ui/study/StudyMaterialContentProvider.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/ssboverview/SSBContentProvider.kt
rm -v app/src/main/kotlin/com/ssbmax/ui/faq/FAQContentProvider.kt

echo ""
echo "✅ Content files deleted successfully!"
echo "📝 Next steps:"
echo "  1. Build the project: ./gradle.sh assembleDebug"
echo "  2. Run tests: ./gradle.sh test"
echo "  3. Verify app works with Firestore data"
echo "  4. Commit: git commit -m 'chore: remove temporary content files after Firestore migration'"
