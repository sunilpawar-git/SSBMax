import { FC, ReactNode, useState, useEffect } from 'react';
import { ShieldCheck, Menu, X, Sun, Moon, Monitor, Wifi, WifiOff, LayoutDashboard, FileText, BookOpen, BarChart3, Award, User, Settings, Download } from 'lucide-react';
import { strings } from '../../constants/strings';
import { useTheme } from '../../hooks/useTheme';
import { useOnlineStatus } from '../../hooks/useOnlineStatus';
import { Footer } from '../legal/Footer';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

export interface AppLayoutProps {
  children: ReactNode;
  activeTab?: string;
  onTabChange?: (tab: string) => void;
  isTestMode?: boolean;
}

export const AppLayout: FC<AppLayoutProps> = ({ children, activeTab = 'home', onTabChange, isTestMode = false }) => {
  const { theme, toggleTheme } = useTheme();
  const isOnline = useOnlineStatus();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);

  useEffect(() => {
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  const handleInstallClick = async () => {
    if (!deferredPrompt) return;
    await deferredPrompt.prompt();
    const choiceResult = await deferredPrompt.userChoice;
    if (choiceResult.outcome === 'accepted') {
      setDeferredPrompt(null);
    }
  };

  const navItems = [
    { id: 'home', label: strings.nav.landing, icon: ShieldCheck },
    { id: 'dashboard', label: strings.nav.dashboard, icon: LayoutDashboard },
    { id: 'practice', label: strings.nav.practice, icon: FileText },
    { id: 'study', label: strings.nav.study, icon: BookOpen },
    { id: 'reports', label: strings.nav.reports, icon: BarChart3 },
    { id: 'pricing', label: strings.nav.pricing, icon: Award },
    { id: 'account', label: strings.nav.account, icon: User },
    { id: 'settings', label: strings.nav.settings, icon: Settings }
  ];

  const handleNavClick = (tabId: string) => {
    onTabChange?.(tabId);
    setMobileMenuOpen(false);
  };

  const getThemeTitle = () => {
    if (theme === 'dark') return `${strings.header.toggleThemeLight} (Current: Dark)`;
    if (theme === 'light') return `${strings.header.toggleThemeSystem} (Current: Light)`;
    return `${strings.header.toggleThemeDark} (Current: System OS)`;
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-slate-100 flex flex-col font-sans selection:bg-sky-500 selection:text-white transition-colors duration-200">
      {/* Tactical Top Navigation Bar */}
      {!isTestMode && (
        <header className="sticky top-0 z-50 w-full bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 px-4 py-3 shadow-sm dark:shadow-lg">
          <div className="max-w-7xl mx-auto flex items-center justify-between">
            {/* Brand Logo & Title */}
            <div 
              className="flex items-center gap-3 cursor-pointer group"
              onClick={() => handleNavClick('home')}
              data-testid="brand-logo"
            >
              <div className="p-2 rounded-xl bg-gradient-to-tr from-sky-600 to-blue-600 text-white shadow-md shadow-sky-600/20 dark:shadow-sky-900/40 group-hover:scale-105 transition-transform">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-lg font-black tracking-wider text-slate-900 dark:text-white uppercase">{strings.header.title}</span>
                  <span className="px-1.5 py-0.5 text-[10px] font-bold bg-amber-500/10 dark:bg-amber-500/20 text-amber-700 dark:text-amber-400 border border-amber-500/30 rounded uppercase tracking-widest">
                    PRO
                  </span>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">{strings.header.tagline}</p>
              </div>
            </div>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center gap-1 bg-slate-100 dark:bg-slate-800/60 p-1 rounded-xl border border-slate-200 dark:border-slate-700/50">
              {navItems.map((item) => {
                const Icon = item.icon;
                const isActive = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => handleNavClick(item.id)}
                    className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                      isActive
                        ? 'bg-sky-600 text-white shadow-md shadow-sky-600/20 dark:shadow-sky-900/30'
                        : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-200/60 dark:hover:bg-slate-700/50'
                    }`}
                    data-testid={`nav-item-${item.id}`}
                  >
                    <Icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </nav>

            {/* Right Action Toolbar */}
            <div className="flex items-center gap-3">
              {/* Online / Offline Status Badge */}
              <div
                className={`hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${
                  isOnline
                    ? 'bg-emerald-500/10 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-400 border-emerald-500/30 dark:border-emerald-700/40'
                    : 'bg-amber-500/10 dark:bg-amber-950/50 text-amber-700 dark:text-amber-400 border-amber-500/30 dark:border-amber-700/40'
                }`}
                data-testid="online-status-badge"
              >
                {isOnline ? (
                  <>
                    <Wifi className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                    <span>{strings.header.statusOnline}</span>
                  </>
                ) : (
                  <>
                    <WifiOff className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
                    <span>{strings.header.statusOffline}</span>
                  </>
                )}
              </div>

              {/* PWA Install Trigger Button */}
              {deferredPrompt && (
                <button
                  onClick={handleInstallClick}
                  className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold bg-sky-600 hover:bg-sky-500 text-white rounded-xl shadow-md transition-colors"
                  title={strings.header.installPwa}
                  data-testid="pwa-install-button"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">{strings.header.installPwa}</span>
                </button>
              )}

              {/* 3-State Theme Toggle Button */}
              <button
                onClick={toggleTheme}
                className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700/60 transition-colors flex items-center gap-1"
                title={getThemeTitle()}
                aria-label={getThemeTitle()}
                data-testid="theme-toggle-button"
              >
                {theme === 'dark' ? (
                  <Moon className="w-4 h-4 text-sky-500 dark:text-sky-400" />
                ) : theme === 'light' ? (
                  <Sun className="w-4 h-4 text-amber-500" />
                ) : (
                  <Monitor className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                )}
              </button>

              {/* Mobile Menu Toggle Button */}
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="md:hidden p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700/60 transition-colors"
                aria-label="Toggle navigation menu"
                data-testid="mobile-menu-button"
              >
                {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {/* Mobile Navigation Drawer */}
          {mobileMenuOpen && (
            <div className="md:hidden mt-3 pt-3 border-t border-slate-200 dark:border-slate-800 flex flex-col gap-1 pb-2" data-testid="mobile-menu-drawer">
              {navItems.map((item) => {
                const Icon = item.icon;
                const isActive = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => handleNavClick(item.id)}
                    className={`flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                      isActive
                        ? 'bg-sky-600 text-white shadow-md'
                        : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    <Icon className="w-5 h-5" />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </div>
          )}
        </header>
      )}

      {/* Main Content Body */}
      <main className={`flex-1 w-full max-w-7xl mx-auto px-4 py-6 ${isTestMode ? 'max-w-none p-0 flex flex-col justify-center' : ''}`}>
        {children}
      </main>

      {/* Tactical Footer Component */}
      {!isTestMode && <Footer onNavClick={handleNavClick} />}
    </div>
  );
};

export default AppLayout;
