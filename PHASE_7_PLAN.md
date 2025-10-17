# Phase 7: Psychology Tests (TAT, WAT, SRT) + Firebase Integration

**Status:** 🚧 **IN PROGRESS**  
**Started:** October 17, 2025

---

## 📋 Overview

Phase 7 implements the three major **Phase 2 Psychology Tests** (TAT, WAT, SRT) and integrates Firebase for backend services (authentication, database, storage).

---

## ✅ Completed

### 1. **Data Models** ✅
- ✅ `TATTest.kt` - TAT data models (12 pictures, story responses, AI/instructor scoring)
- ✅ `WATTest.kt` - WAT data models (60 words, rapid responses, sentiment analysis)
- ✅ `SRTTest.kt` - SRT data models (60 situations, practical responses, leadership assessment)

---

## 🎯 Psychology Tests Overview

### **TAT (Thematic Apperception Test)**

**Format:**
- 12 ambiguous pictures shown one at a time
- Each picture: 30 seconds viewing + 4 minutes writing
- Student writes a story about what they see
- Total time: ~50 minutes

**Scoring Criteria (AI + Instructor):**
- Thematic Perception (0-20): Understanding of scene
- Imagination (0-20): Creativity and originality
- Character Depiction (0-20): Depth of characters
- Emotional Tone (0-20): Positivity and maturity
- Narrative Structure (0-20): Story flow and coherence
- **Total: 100 points**

**UI Flow:**
1. Instructions → View Picture → Write Story → Next Picture
2. Progress bar showing X/12 completed
3. Review all stories before final submission
4. AI score shown immediately after submission
5. Wait for instructor review

---

### **WAT (Word Association Test)**

**Format:**
- 60 words shown one at a time
- 15 seconds per word to write first association
- Auto-advances to next word after timeout
- Total time: 15 minutes

**Scoring Criteria (AI + Instructor):**
- Positivity (0-20): Positive vs negative associations
- Creativity (0-20): Unique and original responses
- Speed (0-20): Response time analysis
- Relevance (0-20): How relevant responses are
- Emotional Maturity (0-20): Maturity of associations
- **Total: 100 points**

**UI Flow:**
1. Instructions → Rapid word display (fullscreen)
2. Countdown timer per word (15s)
3. Text input (auto-focus, enter to submit)
4. Progress: X/60 completed
5. No review (immediate submission after 60th word)
6. AI analysis: positive/negative/neutral breakdown

---

### **SRT (Situation Reaction Test)**

**Format:**
- 60 practical situations presented one at a time
- ~30 seconds per situation to write response
- Describes how they would react
- Total time: 30 minutes

**Scoring Criteria (AI + Instructor):**
- Leadership (0-20): Initiative and command
- Decision Making (0-20): Quality of decisions
- Practicality (0-20): Realistic solutions
- Initiative (0-20): Proactive vs reactive
- Social Responsibility (0-20): Ethics and responsibility
- **Total: 100 points**

**UI Flow:**
1. Instructions → Situation display
2. Text area for response (20-200 chars)
3. "Next" button (not auto-advance, allow thinking)
4. Progress: X/60 completed
5. Review responses before final submit
6. AI analysis: leadership traits, red flags

---

## 🎨 UI/UX Design

### **Common Features Across All 3 Tests:**
- Material Design 3 cards and components
- Real-time progress indicator (linear + text: "12/60")
- Timer display (countdown or elapsed)
- Character counter (for text responses)
- Auto-save functionality (local storage)
- Exit confirmation dialog
- Pause/resume capability (WAT exception: no pause during test)
- Submission confirmation dialog

### **TAT Specific UI:**
- Full-screen image viewer with zoom capability (Coil)
- Phase indicator: "Viewing" vs "Writing"
- Story editor with rich text support
- "Previous/Next" navigation to review stories
- Story list with thumbnails before final submit

### **WAT Specific UI:**
- Minimalist fullscreen word display (large typography)
- Small input field at bottom (Material 3 TextField)
- Rapid auto-advance (no manual "next" button)
- Skip button (if blank, count as skipped)
- Response list shown after completion

### **SRT Specific UI:**
- Situation card at top (scrollable if long)
- Response textarea below
- "Skip" + "Next" buttons
- Category labels hidden (to avoid bias)
- Response review screen before submit

---

## 🔥 Firebase Integration

### **1. Firebase Authentication**

**Files to Create:**
- `core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirebaseAuthService.kt`

**Features:**
- Google Sign-In integration
- User profile creation on first login
- Role assignment (Student/Instructor)
- Token management for API calls
- Session persistence

### **2. Firebase Firestore**

**Collections Structure:**

```
/users/{userId}
  - name, email, photoUrl
  - role: "student" | "instructor"
  - batchIds: []
  - createdAt, lastLogin

/batches/{batchId}
  - name, inviteCode
  - instructorId
  - studentIds: []
  - createdAt

/submissions/{submissionId}
  - userId, testType, testId
  - responses: []
  - status: "submitted" | "graded"
  - aiScore: {}
  - instructorScore: {}
  - submittedAt, gradedAt

/test_configs/{testId}
  - type: "tat" | "wat" | "srt"
  - questions/words/situations: []
  - configuration: {}
```

**Files to Create:**
- `core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirestoreRepository.kt`
- `core/data/src/main/kotlin/com/ssbmax/core/data/remote/dto/SubmissionDto.kt`

**Features:**
- Real-time submission updates
- Offline support with local cache
- Batch operations for performance
- Query optimization (indexed fields)

### **3. Firebase Cloud Functions** (Optional for MVP)

- AI scoring triggers (on submission)
- Notification triggers (on grading)
- Batch invite code generation
- Analytics aggregation

---

## 📂 File Structure

### **New Files to Create:**

#### **TAT:**
1. `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestScreen.kt`
2. `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestViewModel.kt`
3. `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATSubmissionResultScreen.kt`
4. `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATSubmissionResultViewModel.kt`

#### **WAT:**
5. `app/src/main/kotlin/com/ssbmax/ui/tests/wat/WATTestScreen.kt`
6. `app/src/main/kotlin/com/ssbmax/ui/tests/wat/WATTestViewModel.kt`
7. `app/src/main/kotlin/com/ssbmax/ui/tests/wat/WATSubmissionResultScreen.kt`
8. `app/src/main/kotlin/com/ssbmax/ui/tests/wat/WATSubmissionResultViewModel.kt`

#### **SRT:**
9. `app/src/main/kotlin/com/ssbmax/ui/tests/srt/SRTTestScreen.kt`
10. `app/src/main/kotlin/com/ssbmax/ui/tests/srt/SRTTestViewModel.kt`
11. `app/src/main/kotlin/com/ssbmax/ui/tests/srt/SRTSubmissionResultScreen.kt`
12. `app/src/main/kotlin/com/ssbmax/ui/tests/srt/SRTSubmissionResultViewModel.kt`

#### **Firebase:**
13. `core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirebaseAuthService.kt`
14. `core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirestoreService.kt`
15. `core/data/src/main/kotlin/com/ssbmax/core/data/repository/SubmissionRepository.kt`
16. `core/data/src/main/kotlin/com/ssbmax/core/data/repository/UserRepository.kt`

#### **Navigation:**
17. Update `app/src/main/kotlin/com/ssbmax/navigation/SSBMaxDestinations.kt`
18. Update `app/src/main/kotlin/com/ssbmax/navigation/NavGraph.kt`

---

## 🚀 Implementation Order

### **Week 1: TAT Test**
1. ✅ Create TAT data models
2. 🔄 Build TAT Test Screen
3. Build TAT ViewModel
4. Build TAT Result Screen
5. Integrate with navigation

### **Week 2: WAT & SRT Tests**
6. Create WAT Test Screen + ViewModel
7. Create SRT Test Screen + ViewModel
8. Test all three psychology tests end-to-end

### **Week 3: Firebase Integration**
9. Setup Firebase project
10. Implement Firebase Auth
11. Implement Firestore submissions
12. Update all test flows to save to Firebase
13. Test offline support

### **Week 4: Instructor Grading for Psychology Tests**
14. Update Instructor Grading Screen to support TAT/WAT/SRT
15. Create grading UI for each test type
16. Test full student → submission → grading → notification flow

---

## 🎯 Success Criteria

- [ ] All 3 psychology tests functional with proper timing
- [ ] Students can take tests and see AI scores
- [ ] Submissions saved to Firebase
- [ ] Instructors can view and grade all psychology tests
- [ ] Offline support works (submissions queue when offline)
- [ ] No memory leaks or performance issues
- [ ] Build successful with no linter errors

---

## 📝 Notes

- TAT images need to be sourced (12 ambiguous pictures)
- WAT word list needs 60 carefully chosen words (mix of neutral, positive, challenging)
- SRT situations need 60 realistic scenarios across 8 categories
- Firebase security rules must be configured properly
- Consider rate limiting for AI scoring API calls
- Implement proper error handling for network failures

---

**Current Status:** Data models complete, starting TAT UI implementation next 🚀

