export const strings = {
  common: {
    appName: 'SSBMax',
    appTagline: 'Armed Forces Selection Board Preparation Platform',
    loading: 'Loading...',
    error: 'An error occurred',
    retry: 'Retry',
    cancel: 'Cancel',
    save: 'Save',
    submit: 'Submit',
    close: 'Close',
    back: 'Back',
    next: 'Next'
  },
  header: {
    title: 'SSBMax',
    tagline: 'SSB Prep',
    toggleThemeDark: 'Switch to Dark Mode',
    toggleThemeLight: 'Switch to Light Mode',
    statusOnline: 'Online',
    statusOffline: 'Offline',
    installPwa: 'Install App',
    signIn: 'Sign In',
    signOut: 'Sign Out'
  },
  auth: {
    signInWithGoogle: 'Sign in with Google',
    signOut: 'Sign Out',
    welcomeBack: 'Welcome back,',
    authError: 'Authentication failed. Please try again.'
  },
  oir: {
    title: 'Officer Intelligence Rating (OIR)',
    instructions: 'Complete 50 questions within the timed window.',
    timerLabel: 'Time Remaining',
    questionCount: 'Question {current} of {total}',
    submitTest: 'Submit Test',
    submitting: 'Submitting test answers...',
    requiresOnline: 'OIR score evaluation requires online connection.',
    completedTitle: 'OIR Test Completed',
    scoreLabel: 'Your Score',
    ratingLabel: 'OIR Rating',
    noQuestions: 'No questions available'
  },
  psychology: {
    tatTitle: 'Thematic Apperception Test (TAT)',
    watTitle: 'Word Association Test (WAT)',
    srtTitle: 'Situation Reaction Test (SRT)',
    ppdtTitle: 'Picture Perception & Discussion Test (PPDT)',
    sdTitle: 'Self Description (SD)',
    slideTimer: 'Slide Time',
    requiresOnline: 'AI Evaluation requires active internet connection.',
    writeResponsePlaceholder: 'Write your story / response here...',
    nextSlide: 'Next Slide',
    finishTest: 'Complete & Submit',
    completedTitle: 'Psychology Test Completed',
    completedMessage: 'Your responses have been recorded and sent for AI evaluation.'
  },
  payment: {
    upgradeTitle: 'Unlock Pro Membership',
    upgradeDescription: 'Get unlimited AI evaluations for TAT, WAT, SRT, and Mock Interviews.',
    payButton: 'Upgrade Now with Razorpay',
    paymentSuccess: 'Payment successful! Access granted.',
    paymentFailed: 'Payment verification failed.'
  },
  offline: {
    queuedMessage: 'Test response saved offline. Will sync when back online.',
    reconnectNotice: 'You are currently offline. Local cache enabled.'
  },
  studyMaterial: {
    title: 'Free Study Material',
    allCategories: 'All Categories',
    readTime: '{min} min read',
    markAsRead: 'Mark as Read',
    completed: 'Completed',
    noMaterials: 'No study materials available at the moment.',
    loadError: 'Failed to load study materials.',
    offlineNotice: 'Viewing cached study material'
  },
  olq: {
    scoreCardTitle: 'Officer-Like Qualities (OLQ) Assessment',
    factor1: 'Factor I: Intellectual Qualities',
    factor2: 'Factor II: Social Qualities',
    factor3: 'Factor III: Dynamic Qualities',
    factor4: 'Factor IV: Character & Physical Qualities',
    recommendations: 'Key Recommendations & Improvement Areas',
    overallConfidence: 'Overall Assessment Confidence',
    scoreFormat: 'Score: {score} / 10',
    noScores: 'No OLQ scores available.'
  },
  nav: {
    dashboard: 'Command Center',
    landing: 'Home',
    practice: 'Practice Tests',
    study: 'Study Material',
    reports: 'AI Reports',
    pricing: 'Officer Pass'
  },
  landing: {
    heroBadge: 'AI-Powered SSB Intelligence Platform',
    heroTitle: 'Master the Services Selection Board with AI Precision',
    heroSubtitle: 'Comprehensive Stage-I Screening, Stage-II Psychology Battery (TAT, WAT, SRT, SD), and Gemini 2.5 Flash AI Dossier Evaluation with 15 OLQ Analytics.',
    startFree: 'Start Free OIR Test',
    unlockPro: 'Unlock Pro Officer Pass',
    statOlq: '15 Officer Like Qualities',
    statStage: '5-Day SSB Coverage',
    statAi: 'Gemini 2.5 AI Analysis',
    featureTitle: 'Complete Selection Board Suite',
    featureSubtitle: 'Trained on standardized Defence Selection Board guidelines for Army, Navy, and Air Force aspirants.'
  },
  dashboard: {
    title: 'Candidate Command Center',
    subtitle: 'Track your preparation progress, run timed SSB test simulators, and review AI OLQ assessments.',
    readinessScore: 'SSB Readiness Score',
    stage1Title: 'Stage I: Screening Battery',
    stage2Title: 'Stage II: Psychology Tests',
    recentReports: 'Recent AI Dossier Reports',
    noReports: 'No AI test reports generated yet. Complete a Psychology test to receive your 15 OLQ evaluation.'
  }
} as const;
