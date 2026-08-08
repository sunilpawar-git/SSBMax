import { useState, FC } from 'react';
import { AppLayout } from './components/layout/AppLayout';
import { HeroSection } from './components/landing/HeroSection';
import { SubscriptionPage } from './components/subscription/SubscriptionPage';

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
    </AppLayout>
  );
};

export default App;

