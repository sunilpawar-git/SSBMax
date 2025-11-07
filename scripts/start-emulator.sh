#!/bin/bash

# SSBMax Firebase Emulator Startup Script
# This script starts the Firebase emulators needed for testing

echo "🚀 Starting Firebase Emulators..."
echo "================================="
echo "Firestore: http://localhost:8080"
echo "Auth:      http://localhost:9099"
echo "Storage:   http://localhost:9199"
echo "UI:        http://localhost:4000"
echo "================================="
echo ""
echo "Press Ctrl+C to stop emulators"
echo ""

# Start emulators (Firestore, Auth, Storage)
firebase emulators:start --only firestore,auth,storage

