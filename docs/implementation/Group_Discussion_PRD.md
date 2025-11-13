# 📋 Product Requirements Document: Group Discussion Feature

## 📅 Document Information
- **Version:** 1.0
- **Date:** November 11, 2025
- **Author:** SSBMax Development Team
- **Status:** Ready for Development

## 🎯 Executive Summary

The Group Discussion feature is a core component of SSBMax's GTO (Group Testing Officer) test suite. It provides users with an immersive, AI-powered simulation of SSB Group Discussions, enabling them to practice essential communication, leadership, and collaborative skills in a realistic virtual environment.

## 🎯 Product Objectives

### Primary Goals
- **Authentic SSB Experience**: Simulate real SSB Group Discussion conditions with accurate timing, topics, and evaluation criteria
- **Skill Development**: Help users improve communication, leadership, and group dynamics skills
- **Immediate Feedback**: Provide AI-powered analysis and personalized recommendations
- **Accessibility**: Make SSB preparation accessible to candidates nationwide

### Success Metrics
- **User Engagement**: 70% of premium users complete at least 2 group discussions per month
- **Feature Adoption**: 60% of new users try the Group Discussion feature within first week
- **User Satisfaction**: 4.5+ star rating for the feature
- **Technical Performance**: <2 second load times, 99.5% uptime

## 👥 User Stories

### Core User Journeys

#### User Story 1: First-Time Group Discussion
```
As a new SSB aspirant,
I want to experience a realistic group discussion simulation,
So that I can understand what to expect in actual SSB tests.
```
**Acceptance Criteria:**
- User sees topic introduction screen
- 8 participant circles displayed randomly
- User assigned random position
- Clear instructions provided
- Discussion starts within 30 seconds

#### User Story 2: Active Participation
```
As an SSB candidate,
I want to contribute meaningfully to the discussion,
So that I can practice my communication and leadership skills.
```
**Acceptance Criteria:**
- Clear visual indication when it's user's turn (glowing circle)
- 45-second speaking time per turn
- Real-time transcription display
- Audio recording with visual feedback
- Graceful handling of turn transitions

#### User Story 3: Performance Analysis
```
As a serious SSB aspirant,
I want detailed feedback on my discussion performance,
So that I can identify areas for improvement.
```
**Acceptance Criteria:**
- Comprehensive scoring (Communication, Content, Leadership, Group Harmony)
- AI-generated personalized feedback
- Comparison with peer benchmarks
- Actionable improvement suggestions
- Historical progress tracking

## 🏗️ Technical Requirements

### Core Components

#### 1. Discussion Engine
- **Real-time State Management**: Track speaking order, timing, and participant states
- **Audio Processing**: Record, transcribe, and analyze user speech
- **AI Orchestration**: Manage virtual participant responses and behaviors
- **Timer System**: Accurate timing for discussion phases and speaking turns

#### 2. AI Integration
- **Speech-to-Text**: AWS Transcribe for accurate transcription
- **LLM Analysis**: Google Gemini for performance evaluation
- **Voice Generation**: ElevenLabs/Google TTS for virtual participants
- **Response Caching**: Intelligent caching to reduce API costs

#### 3. Data Management
- **Firestore Storage**: Secure storage of discussions, transcriptions, and results
- **Real-time Sync**: Live updates during active discussions
- **Offline Capability**: Basic functionality without internet
- **Data Privacy**: GDPR-compliant data handling

### Performance Requirements
- **Load Time**: <3 seconds for discussion start
- **Audio Latency**: <500ms for speech processing
- **UI Responsiveness**: 60fps animations and transitions
- **Memory Usage**: <200MB during active discussion

## 🎨 UI/UX Specifications

### Screen Flow

#### Screen 1: Topic Introduction (5 seconds)
```
┌─────────────────────────────────────────┐
│           GROUP DISCUSSION TEST          │
├─────────────────────────────────────────┤
│                                         │
│  📢 TOPIC:                              │
│  "Should Social Media Platforms         │
│   Be Regulated by Government?"          │
│                                         │
│  ⏱️  Discussion Time: 15 minutes         │
│  👥  Participants: 8                    │
│                                         │
│  [Preparing Discussion...]              │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  100%     │
│                                         │
│              [START]                     │
└─────────────────────────────────────────┘
```

#### Screen 2: Discussion Arena
```
┌─────────────────────────────────────────┐
│  TOPIC: Social Media Regulation         │
│  ⏱️ 14:58 remaining                     │
├─────────────────────────────────────────┤
│                                         │
│        ○       ○       ○       ○        │
│                                         │
│   ○         ███████         ○           │
│             │ YOU │                      │
│        ○       ○       ○       ○        │
│                                         │
│  🔴 Recording Active                    │
│  💬 Speak when it's your turn           │
│                                         │
│  [Current Speaker: Participant 3]       │
└─────────────────────────────────────────┘
```

#### Screen 3: Active Speaking State
```
┌─────────────────────────────────────────┐
│  TOPIC: Social Media Regulation         │
│  ⏱️ 12:34 remaining                     │
├─────────────────────────────────────────┤
│                                         │
│        ○       ○     ⚡⚡⚡⚡⚡⚡⚡     ○        │
│                          │              │
│   ○         ███████       ⚡ Participant 3 ⚡   │
│             │ YOU │       ⚡   Speaking   ⚡   │
│        ○       ○     ⚡⚡⚡⚡⚡⚡⚡     ○        │
│                                         │
│  🎙️  "I believe government regulation...│
│      ...is necessary because..."         │
│                                         │
│  [Your turn in: 3 participants]          │
└─────────────────────────────────────────┘
```

### Design System
- **Material Design 3**: Consistent with app theme
- **Color Coding**: Distinct colors for different participant states
- **Animations**: Smooth transitions and visual feedback
- **Typography**: Clear hierarchy for topic and instructions
- **Accessibility**: Screen reader support and high contrast modes

## 📊 Data Models

### Core Entities

#### Discussion Session
```kotlin
data class DiscussionSession(
    val id: String,
    val userId: String,
    val topicId: String,
    val startTime: Long,
    val endTime: Long?,
    val status: DiscussionStatus,
    val participantPosition: Int, // 0-7
    val totalParticipants: Int = 8,
    val settings: DiscussionSettings
)

enum class DiscussionStatus {
    PREPARING,
    ACTIVE,
    COMPLETED,
    ANALYZING,
    FAILED
}
```

#### Discussion Settings
```kotlin
data class DiscussionSettings(
    val durationMinutes: Int = 15,
    val speakingTimeSeconds: Int = 45,
    val enableVoiceGeneration: Boolean = true,
    val analysisDepth: AnalysisTier = AnalysisTier.STANDARD
)

enum class AnalysisTier {
    BASIC,      // Template feedback
    STANDARD,   // Personalized analysis
    PREMIUM     // Detailed breakdown
}
```

#### Participant State
```kotlin
data class ParticipantState(
    val position: Int,
    val isUser: Boolean,
    val isSpeaking: Boolean,
    val speakingStartTime: Long?,
    val totalSpeakingTime: Long = 0,
    val avatarColor: Color
)
```

#### Transcription Data
```kotlin
data class TranscriptionSegment(
    val participantId: String,
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val confidence: Float,
    val isUser: Boolean
)

data class DiscussionTranscription(
    val sessionId: String,
    val segments: List<TranscriptionSegment>,
    val fullTranscript: String,
    val wordCount: Int,
    val speakingTimeDistribution: Map<String, Long>
)
```

#### Performance Analysis
```kotlin
data class DiscussionAnalysis(
    val sessionId: String,
    val overallScore: Float, // 0-10
    val categoryScores: Map<AnalysisCategory, Float>,
    val aiFeedback: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val benchmarkComparison: BenchmarkComparison
)

enum class AnalysisCategory {
    COMMUNICATION,
    CONTENT_QUALITY,
    LEADERSHIP,
    GROUP_HARMONY,
    TIME_MANAGEMENT
}

data class BenchmarkComparison(
    val percentileRank: Float,
    val peerAverage: Float,
    val topPerformers: Float
)
```

## 🔧 API Specifications

### Firestore Collections

#### discussions/{sessionId}
```json
{
  "userId": "user123",
  "topicId": "social_media_regulation",
  "startTime": 1636723456789,
  "status": "ACTIVE",
  "participantPosition": 3,
  "settings": {
    "durationMinutes": 15,
    "analysisDepth": "STANDARD"
  }
}
```

#### transcriptions/{sessionId}
```json
{
  "segments": [
    {
      "participantId": "user123",
      "startTime": 1636723500000,
      "text": "I believe regulation is necessary...",
      "isUser": true
    }
  ],
  "fullTranscript": "Complete discussion text...",
  "speakingTimeDistribution": {
    "user123": 180000
  }
}
```

#### analysis/{sessionId}
```json
{
  "overallScore": 8.5,
  "categoryScores": {
    "COMMUNICATION": 8.7,
    "CONTENT_QUALITY": 8.2,
    "LEADERSHIP": 8.8,
    "GROUP_HARMONY": 8.9
  },
  "aiFeedback": "Excellent listening skills...",
  "strengths": ["Clear articulation", "Active listening"],
  "improvements": ["Show more initiative"]
}
```

### AI Service Integration

#### Speech-to-Text Service
```kotlin
interface SpeechToTextService {
    suspend fun transcribeAudio(audioData: ByteArray): TranscriptionResult
    suspend fun streamTranscription(audioStream: Flow<ByteArray>): Flow<TranscriptionSegment>
}

data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val segments: List<TranscriptionSegment>
)
```

#### LLM Analysis Service
```kotlin
interface DiscussionAnalysisService {
    suspend fun analyzeDiscussion(
        transcript: DiscussionTranscription,
        topic: DiscussionTopic
    ): DiscussionAnalysis

    suspend fun generateFeedback(
        analysis: DiscussionAnalysis,
        userHistory: List<DiscussionAnalysis>
    ): PersonalizedFeedback
}
```

## 🔒 Security & Privacy

## 💰 Cost Analysis & Projections

### Cost Breakdown per Discussion (15 minutes)

#### Core Components Cost (INR)
| Component | Cost per Discussion | Monthly (1K users, 2 discussions) | Optimization Potential |
|-----------|-------------------|-----------------------------------|----------------------|
| **Speech-to-Text** (AWS Transcribe) | ₹0.45 | ₹900 | 40% (local processing) |
| **LLM Analysis** (Google Gemini) | ₹0.04 | ₹80 | 60% (batch processing) |
| **Voice Generation** (Optional) | ₹7.00 | ₹14,000 | 70% (caching) |
| **Firestore Storage** | ₹0.67 | ₹1,340 | 30% (compression) |
| **Firebase Functions** | ₹0.25 | ₹500 | 50% (optimization) |
| **TOTAL** | **₹8.41** | **₹16,820** | **55% reduction possible** |

#### Cost Optimization Strategies

##### 1. Smart Caching System
```kotlin
// Cache AI responses for common discussion patterns
val responseCache = mapOf(
    "social_media" to pregeneratedResponses,
    "environment" to pregeneratedResponses
)

// Reuse analysis prompts for similar topics
val analysisTemplates = mapOf(
    "communication" to "Analyze communication skills...",
    "leadership" to "Evaluate leadership qualities..."
)
```
**Impact:** 40-50% reduction in LLM costs

##### 2. Batch Processing
```kotlin
suspend fun batchAnalyzeDiscussions(discussions: List<Discussion>) {
    // Single LLM call for multiple analyses
    val batchPrompt = discussions.joinToString("\n---\n") { 
        "Analyze: ${it.transcription}" 
    }
}
```
**Impact:** 60-70% reduction in API calls

##### 3. Selective Analysis
```kotlin
// Only analyze key segments, not entire transcriptions
enum class AnalysisDepth {
    OPENING_ONLY,      // First 2 minutes
    KEY_MOMENTS,       // High-impact segments
    COMPREHENSIVE      // Full analysis
}
```
**Impact:** 50-60% reduction in processing

##### 4. Tiered Service Levels
- **Free Tier:** Template feedback, cached responses → ₹2.51/discussion
- **Premium Tier:** Full AI analysis → ₹12.53/discussion
- **Enterprise:** Custom models → ₹25.06/discussion

### Projected Costs with Optimization

#### Monthly Cost Projections (INR)

| User Base | Discussions/User | Base Cost | Optimized Cost | Savings |
|-----------|-----------------|-----------|----------------|---------|
| **1,000 users** | 2/month | ₹16,820 | ₹7,530 | ₹9,290 (55%) |
| **5,000 users** | 2/month | ₹84,100 | ₹37,650 | ₹46,450 (55%) |
| **10,000 users** | 2/month | ₹1,68,200 | ₹75,300 | ₹92,900 (55%) |
| **25,000 users** | 1.5/month | ₹3,15,788 | ₹1,41,338 | ₹1,74,450 (55%) |

#### Break-even Analysis
- **Subscription Price:** ₹417/month (unlimited discussions)
- **Cost per User:** ₹7.53/month (optimized)
- **Break-even Users:** ~55 users/month
- **With 20% conversion:** Profitable at ~275 active users

### Revenue Model Alignment

#### Tiered Pricing Strategy
```kotlin
enum class SubscriptionTier(val pricePerMonth: Int, val costPerDiscussion: Double) {
    FREE(pricePerMonth = 0, costPerDiscussion = 2.51),
    PREMIUM(pricePerMonth = 417, costPerDiscussion = 8.41),
    ENTERPRISE(pricePerMonth = 833, costPerDiscussion = 16.82)
}
```

#### Profitability Projections
- **Paying Users:** 20% of total users
- **Average Discussions:** 3 per paying user/month
- **Revenue per User:** ₹417/month
- **Cost per User:** ₹25.23/month (3 discussions × ₹8.41)
- **Profit Margin:** 94%

### Cost Monitoring & Controls

#### Usage Limits
```kotlin
data class UsageLimits(
    val maxDiscussionsPerDay: Int = 5,
    val maxAnalysisRequestsPerHour: Int = 10,
    val cacheHitRatio: Double = 0.7 // Target 70% cache hits
)
```

#### Cost Alerts
- **Daily Budget Alert:** ₹500/day threshold
- **Monthly Budget Alert:** ₹10,000/month threshold
- **Anomaly Detection:** Unusual usage patterns

#### Scaling Considerations
- **Cost per user decreases with scale:** 1K users = ₹7.53/user, 100K users = ₹0.75/user
- **Infrastructure costs:** Mostly fixed, API costs scale with usage
- **Optimization ROI:** 55% cost reduction = ₹2,00,000+ monthly savings at scale

### Risk Mitigation

#### Cost Overrun Prevention
- **Hard Limits:** Firebase budget alerts and automatic throttling
- **Monitoring:** Real-time cost tracking dashboard
- **Fallbacks:** Cached responses when API limits reached
- **Gradual Rollout:** Beta testing with limited users first

#### Technical Debt Considerations
- **Modular Design:** Easy to swap AI providers if costs change
- **Caching Infrastructure:** Invest upfront for long-term savings
- **Batch Processing Pipeline:** Built-in from day one

---

*Cost estimates based on current AWS/Google pricing (Nov 2025). Actual costs may vary with market conditions and usage patterns.*



### Data Protection
- **Encryption**: All audio and transcription data encrypted at rest and in transit
- **Access Control**: User data isolated by Firebase Auth UID
- **Retention Policy**: Discussion data retained for 2 years, audio files for 30 days
- **GDPR Compliance**: Data portability and deletion capabilities

### Cost Optimization
- **Caching Strategy**: AI responses cached for common discussion patterns
- **Batch Processing**: Multiple discussions analyzed together
- **Selective Analysis**: Only key segments processed for detailed analysis
- **Tiered Processing**: Different analysis depth based on subscription level

## 📈 Success Metrics & KPIs

### User Engagement Metrics
- **Discussion Completion Rate**: % of started discussions completed
- **Average Discussion Length**: Target 12-15 minutes
- **Return Usage**: % of users doing multiple discussions
- **Feature Adoption**: % of users trying GD within first month

### Performance Metrics
- **Audio Quality**: <5% transcription errors
- **Response Time**: <2 seconds for AI participant responses
- **System Reliability**: 99.9% uptime
- **User Satisfaction**: 4.5+ star rating

### Business Metrics
- **Cost per Discussion**: Maintain <₹10 per discussion
- **Revenue Impact**: Track premium subscription conversions
- **User Retention**: Monitor churn rates vs feature usage

## 🚀 Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1-2)
- [ ] Basic discussion UI layout
- [ ] Participant circle animations
- [ ] Timer and state management
- [ ] Basic audio recording

### Phase 2: AI Integration (Week 3-4)
- [ ] Speech-to-text integration
- [ ] Virtual participant responses
- [ ] Basic transcription display
- [ ] Simple analysis engine

### Phase 3: Advanced Features (Week 5-6)
- [ ] Voice generation for participants
- [ ] Comprehensive AI analysis
- [ ] Performance benchmarking
- [ ] Historical tracking

### Phase 4: Optimization & Polish (Week 7-8)
- [ ] Cost optimization implementation
- [ ] Performance tuning
- [ ] UI/UX refinements
- [ ] Comprehensive testing

## 🧪 Testing Strategy

### Unit Testing
- Discussion state management
- Timer accuracy
- Audio processing components
- AI service integrations

### Integration Testing
- End-to-end discussion flow
- Audio recording and transcription
- AI analysis pipeline
- Firestore data persistence

### User Acceptance Testing
- Real user testing with various devices
- Performance testing under load
- Edge case handling (network issues, interruptions)

## 📋 Risk Assessment & Mitigation

### Technical Risks
- **AI Service Reliability**: Implement fallback to cached responses
- **Audio Quality Issues**: Graceful degradation with error handling
- **High Latency**: Optimize for Indian network conditions

### Business Risks
- **Cost Overruns**: Implement usage monitoring and throttling
- **User Adoption**: Start with beta testing and gather feedback
- **Competition**: Focus on authentic SSB experience differentiation

## 📞 Support & Maintenance

### User Support
- In-app help documentation
- Tutorial videos for first-time users
- FAQ section for common issues
- Customer support integration

### Technical Maintenance
- Regular AI model updates
- Performance monitoring and optimization
- Security updates and patches
- Cost monitoring and optimization

---

## 📝 Approval & Sign-off

**Product Manager:** ____________________ Date: __________
**Technical Lead:** ____________________ Date: __________
**Design Lead:** ____________________ Date: __________

**Document Status:** ✅ Ready for Development

---

*This PRD serves as the authoritative source for Group Discussion feature development. All implementation decisions should reference this document.*
