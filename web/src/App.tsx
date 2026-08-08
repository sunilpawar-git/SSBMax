import { useState, FC } from 'react';
import { AppLayout } from './components/layout/AppLayout';
import { HeroSection } from './components/landing/HeroSection';
import { SubscriptionPage } from './components/subscription/SubscriptionPage';
import { AccountPage } from './components/account/AccountPage';
import { SettingsPage } from './components/settings/SettingsPage';

export const App: FC = () => {
  const [activeTab, setActiveTab] = useState('home');

  const handleStartFree = () => {
    setActiveTab('practice');
  };

  const handleUnlockPro = () => {
    setActiveTab('pricing');
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
    </AppLayout>
  );
};

export default App;

