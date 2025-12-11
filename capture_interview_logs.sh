#!/bin/bash
# Comprehensive Interview Flow Log Capture
# Catches all relevant tags including inferred ErrorLogger tags

echo "🎯 Starting interview log capture..."
echo "📱 Device: $(adb devices | grep -v "List" | cut -f1)"
echo "⏰ Started at: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
echo "🔥 Press Ctrl+C to stop logging"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Clear old logs
adb logcat -c

# Capture with comprehensive filters
adb logcat -v threadtime | grep -E \
  "(SSBMax|ErrorLogger|Interview|Gemini|AIService|Firestore|AudioRecorder|\
InterviewSessionViewModel|InterviewRepository|StartInterviewViewModel|\
GeminiAIService|FirestoreInterviewRepository|QuestionCache|\
AndroidRuntime|System\.err)" \
  | tee "interview_logs_$(date +%Y%m%d_%H%M%S).txt"
