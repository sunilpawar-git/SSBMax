package com.ssbmax.ui.faq

import com.ssbmax.core.domain.model.FAQCategory
import com.ssbmax.core.domain.model.FAQItem

/**
 * Provides FAQ content for the app
 */
object FAQContentProvider {
    
    fun getFAQItems(): List<FAQItem> = listOf(
        // GENERAL
        FAQItem(
            id = "faq_1",
            question = "What is SSBMax?",
            answer = "SSBMax is a comprehensive SSB (Services Selection Board) preparation app designed to help candidates prepare for all stages of the SSB interview process. It includes study materials, practice tests, expert tips, and access to SSB coaching institutes through our marketplace.",
            category = FAQCategory.GENERAL,
            order = 1
        ),
        FAQItem(
            id = "faq_2",
            question = "Who can use SSBMax?",
            answer = "SSBMax is designed for all SSB aspirants preparing for Indian Armed Forces selection. Whether you're a first-time candidate or reappearing, SSBMax provides comprehensive resources for all levels.",
            category = FAQCategory.GENERAL,
            order = 2
        ),
        FAQItem(
            id = "faq_3",
            question = "Is SSBMax available offline?",
            answer = "Yes! Most study materials and completed tests are available offline once downloaded. However, you'll need an internet connection for live tests, assessor feedback, and marketplace features.",
            category = FAQCategory.GENERAL,
            order = 3
        ),
        
        // TESTS & ASSESSMENTS
        FAQItem(
            id = "faq_4",
            question = "How do I access tests?",
            answer = "Navigate to the sidebar and select your desired phase (Phase 1 or Phase 2). Then choose the specific test type (OIR, PPDT, Psychology Tests, etc.). Each topic screen has a 'Tests' tab where you can start practice tests.",
            category = FAQCategory.TESTS,
            order = 4
        ),
        FAQItem(
            id = "faq_5",
            question = "What tests are available?",
            answer = "SSBMax offers all SSB tests including:\n• Phase 1: OIR Test, PPDT\n• Phase 2: TAT, WAT, SRT, SDT (Psychology Tests), GTO Tasks, Interview preparation\n• Conference stage guidance",
            category = FAQCategory.TESTS,
            order = 5
        ),
        FAQItem(
            id = "faq_6",
            question = "How are tests graded?",
            answer = "Basic and Pro users receive automated scoring. AI Premium users get AI-powered analysis with detailed feedback. Premium users can submit tests for professional assessor review with personalized remarks.",
            category = FAQCategory.TESTS,
            order = 6
        ),
        FAQItem(
            id = "faq_7",
            question = "Can I retake tests?",
            answer = "Yes! You can retake any test multiple times. Each attempt is logged in your progress dashboard, allowing you to track improvement over time.",
            category = FAQCategory.TESTS,
            order = 7
        ),
        
        // SUBSCRIPTION & BILLING
        FAQItem(
            id = "faq_8",
            question = "What subscription plans are available?",
            answer = "SSBMax offers 4 tiers:\n• Basic (Free): All study materials\n• Pro: Basic + some tests\n• AI Premium: Pro + AI-based test analysis\n• Premium: Pro + SSB Marketplace access to coaching institutes",
            category = FAQCategory.SUBSCRIPTION,
            order = 8
        ),
        FAQItem(
            id = "faq_9",
            question = "How do I upgrade my subscription?",
            answer = "Go to Settings → Your Subscription → Upgrade Plan. Select your desired tier and complete the payment process. Your upgrade will be activated immediately.",
            category = FAQCategory.SUBSCRIPTION,
            order = 9
        ),
        FAQItem(
            id = "faq_10",
            question = "Can I cancel my subscription?",
            answer = "Yes, you can cancel anytime from Settings → Your Subscription → Manage → Cancel. You'll retain access until the end of your current billing period.",
            category = FAQCategory.SUBSCRIPTION,
            order = 10
        ),
        FAQItem(
            id = "faq_11",
            question = "What payment methods are accepted?",
            answer = "We accept Credit/Debit Cards, UPI, Net Banking, and popular mobile wallets. All payments are processed securely.",
            category = FAQCategory.SUBSCRIPTION,
            order = 11
        ),
        
        // TECHNICAL SUPPORT
        FAQItem(
            id = "faq_12",
            question = "I'm not receiving notifications. What should I do?",
            answer = "Go to Settings → Notifications and ensure push notifications are enabled. Also check your device settings to ensure SSBMax has notification permissions. If issues persist, try reinstalling the app.",
            category = FAQCategory.TECHNICAL,
            order = 12
        ),
        FAQItem(
            id = "faq_13",
            question = "The app is crashing. How do I fix this?",
            answer = "Try these steps:\n1. Force close and restart the app\n2. Clear app cache from device settings\n3. Ensure you have the latest version from Play Store\n4. Restart your device\nIf the issue persists, contact support with your device model and Android version.",
            category = FAQCategory.TECHNICAL,
            order = 13
        ),
        FAQItem(
            id = "faq_14",
            question = "How do I change the app theme?",
            answer = "Go to Settings → Appearance and select your preferred theme: Light, Dark, or System Default. The change applies immediately without restarting the app.",
            category = FAQCategory.TECHNICAL,
            order = 14
        ),
        
        // SSB PROCESS
        FAQItem(
            id = "faq_15",
            question = "What is the SSB selection process?",
            answer = "The SSB process is a 5-day selection procedure:\n• Day 1: Screening (OIR, PPDT)\n• Day 2-4: Psychological Tests, GTO Tasks, Interview\n• Day 5: Conference\nOnly screened-in candidates proceed to Day 2-5.",
            category = FAQCategory.SSB_PROCESS,
            order = 15
        ),
        FAQItem(
            id = "faq_16",
            question = "What are Officer Like Qualities (OLQs)?",
            answer = "OLQs are 15 qualities assessed during SSB: Effective Intelligence, Reasoning Ability, Organizing Ability, Power of Expression, Social Adjustment, Cooperation, Sense of Responsibility, Initiative, Self Confidence, Speed of Decision, Ability to Influence the Group, Liveliness, Determination, Courage, and Stamina.",
            category = FAQCategory.SSB_PROCESS,
            order = 16
        ),
        FAQItem(
            id = "faq_17",
            question = "How many times can I appear for SSB?",
            answer = "There's no limit on the number of times you can appear for SSB. Many successful officers cleared SSB on their 2nd or 3rd attempt. Each attempt is a learning opportunity.",
            category = FAQCategory.SSB_PROCESS,
            order = 17
        ),
        FAQItem(
            id = "faq_18",
            question = "What should I bring to SSB?",
            answer = "Essential items: Call letter, valid photo ID, passport-size photos, educational certificates, character certificates, proper formal attire (shirt, trousers, tie), sports wear, toiletries, and a positive attitude!",
            category = FAQCategory.SSB_PROCESS,
            order = 18
        ),
        FAQItem(
            id = "faq_19",
            question = "How can SSBMax help me prepare?",
            answer = "SSBMax provides:\n• Authentic test simulations matching real SSB conditions\n• Comprehensive study materials for all stages\n• Expert tips and strategies\n• AI-powered feedback (AI Premium)\n• Professional assessor reviews (Premium)\n• Access to premier coaching institutes (Premium)",
            category = FAQCategory.SSB_PROCESS,
            order = 19
        ),
        FAQItem(
            id = "faq_20",
            question = "What is the success rate at SSB?",
            answer = "Overall, about 3-5% of candidates who appear at SSB get recommended. However, with proper preparation and understanding of the process (which SSBMax provides), you can significantly improve your chances.",
            category = FAQCategory.SSB_PROCESS,
            order = 20
        )
    ).sortedBy { it.order }
}

