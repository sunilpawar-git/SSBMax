#!/usr/bin/env bash
# ============================================================
# upload_oir_images_to_firebase.sh
# Uploads all OIR set SVG images to Firebase Storage.
#
# Prerequisites:
#   1. Firebase CLI installed:  npm install -g firebase-tools
#   2. Logged in:               firebase login
#   3. Project set:             firebase use <your-project-id>
#      OR pass as env var:      FIREBASE_PROJECT=ssbmax-app ./upload_oir_images_to_firebase.sh
#
# Usage:
#   chmod +x upload_oir_images_to_firebase.sh
#   ./upload_oir_images_to_firebase.sh
#
# After upload, each file is publicly readable via:
#   https://firebasestorage.googleapis.com/v0/b/<PROJECT>.appspot.com/o/oir%2Fimages%2Fset_001%2F<FILE>.svg?alt=media
# ============================================================

set -euo pipefail

PROJECT="${FIREBASE_PROJECT:-}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_DIR="$SCRIPT_DIR/oir-images/set_001"
STORAGE_PATH="oir/images/set_001"

# ── Validate ────────────────────────────────────────────────
if [[ -z "$PROJECT" ]]; then
  echo "Reading Firebase project from .firebaserc …"
  PROJECT=$(python3 -c "import json; d=open('$SCRIPT_DIR/../.firebaserc').read(); print(json.loads(d)['projects']['default'])" 2>/dev/null || true)
fi

if [[ -z "$PROJECT" ]]; then
  echo "ERROR: Could not detect Firebase project."
  echo "  Set env var FIREBASE_PROJECT=your-project-id and re-run."
  exit 1
fi

BUCKET="${PROJECT}.appspot.com"
echo "Project : $PROJECT"
echo "Bucket  : gs://$BUCKET"
echo "Source  : $IMAGE_DIR"
echo "Target  : gs://$BUCKET/$STORAGE_PATH/"
echo ""

# ── Upload each SVG ─────────────────────────────────────────
SVG_FILES=("$IMAGE_DIR"/*.svg)
TOTAL=${#SVG_FILES[@]}
DONE=0

for svg in "${SVG_FILES[@]}"; do
  FILENAME=$(basename "$svg")
  DEST="gs://$BUCKET/$STORAGE_PATH/$FILENAME"
  echo "[$((DONE+1))/$TOTAL] Uploading $FILENAME …"
  gsutil -h "Content-Type:image/svg+xml" \
         -h "Cache-Control:public,max-age=31536000" \
         cp "$svg" "$DEST"
  DONE=$((DONE+1))
done

echo ""
echo "✅ Uploaded $DONE/$TOTAL SVG files to gs://$BUCKET/$STORAGE_PATH/"
echo ""

# ── Make files publicly readable ────────────────────────────
echo "Setting public read ACL …"
gsutil -m acl ch -r -u AllUsers:R "gs://$BUCKET/$STORAGE_PATH/" 2>/dev/null || \
  echo "  (ACL step skipped — bucket may use uniform access control, which is fine)"

echo ""
echo "Download URL template:"
echo "  https://firebasestorage.googleapis.com/v0/b/$BUCKET/o/oir%2Fimages%2Fset_001%2F<FILENAME>.svg?alt=media"
echo ""
echo "Example (Q1):"
echo "  https://firebasestorage.googleapis.com/v0/b/$BUCKET/o/oir%2Fimages%2Fset_001%2Foir_s01_q01_cube_classification.svg?alt=media"
