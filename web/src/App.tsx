import { useState, FC } from 'react';
import { AppLayout } from './components/layout/AppLayout';
import { HeroSection } from './components/landing/HeroSection';
import { SubscriptionPage } from './components/subscription/SubscriptionPage';
import { AccountPage } from './components/account/AccountPage';
import { SettingsPage } from './components/settings/SettingsPage';
import { PrivacyPolicy } from './components/legal/PrivacyPolicy';
import { TermsAndRefunds } from './components/legal/TermsAndRefunds';

export const App: FC = () => {
  const [activeTab, setActiveTab] = useState('home');

  const handleStartFree = () => {
    setActiveTab('practice');
  };

  const handleUnlockPro = () => {
    setActiveTab('pricing');
  };

  const handleBackToHome = () => {
    setActiveTab('home');
  };

  return (
    <AppLayout activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'home' && (
        <HeroSection
          onStartFreeClick={handleStartFree}
          onUnlockProClick={handleUnlockPro}
        />
      )}
      {activeTab === 'pricing' && <SubscriptionPage />}
      {activeTab === 'account' && (
        <AccountPage
          onUpgradeClick={handleUnlockPro}
        />
      )}
      {activeTab === 'settings' && <SettingsPage />}
      {activeTab === 'privacy' && (
        <PrivacyPolicy onBackClick={handleBackToHome} />
      )}
      {activeTab === 'terms' && (
        <TermsAndRefunds onBackClick={handleBackToHome} />
      )}
    </AppLayout>
  );
};

export default App;
