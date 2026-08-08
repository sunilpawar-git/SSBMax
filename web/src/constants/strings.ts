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
    requiresOnline: 'OIR score evaluation requires online connection.'
  },
  psychology: {
    tatTitle: 'Thematic Apperception Test (TAT)',
    watTitle: 'Word Association Test (WAT)',
    srtTitle: 'Situation Reaction Test (SRT)',
    ppdtTitle: 'Picture Perception & Discussion Test (PPDT)',
    sdTitle: 'Self Description (SD)',
    slideTimer: 'Slide Time',
    requiresOnline: 'AI Evaluation requires active internet connection.'
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
  }
} as const;
