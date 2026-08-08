import { FC, ReactNode, useState } from 'react';
import { ShieldCheck, Menu, X, Sun, Moon, Wifi, WifiOff, LayoutDashboard, FileText, BookOpen, BarChart3, Award, User, Settings } from 'lucide-react';
import { strings } from '../../constants/strings';
import { useTheme } from '../../hooks/useTheme';
import { useOnlineStatus } from '../../hooks/useOnlineStatus';

export interface AppLayoutProps {
  children: ReactNode;
  activeTab?: string;
  onTabChange?: (tab: string) => void;
}

export const AppLayout: FC<AppLayoutProps> = ({ children, activeTab = 'home', onTabChange }) => {
  const { theme, toggleTheme } = useTheme();
  const isOnline = useOnlineStatus();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

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

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col font-sans selection:bg-sky-500 selection:text-white">
      {/* Tactical Top Navigation Bar */}
      <header className="sticky top-0 z-50 w-full bg-slate-900/90 backdrop-blur-md border-b border-slate-800 px-4 py-3 shadow-lg">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          {/* Brand Logo & Title */}
          <div 
            className="flex items-center gap-3 cursor-pointer group"
            onClick={() => handleNavClick('home')}
            data-testid="brand-logo"
          >
            <div className="p-2 rounded-xl bg-gradient-to-tr from-sky-600 to-sky-400 text-white shadow-md shadow-sky-900/40 group-hover:scale-105 transition-transform">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-lg font-black tracking-wider text-white uppercase">{strings.header.title}</span>
                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded uppercase tracking-widest">
                  PRO
                </span>
              </div>
              <p className="text-xs text-slate-400 font-medium">{strings.header.tagline}</p>
            </div>
          </div>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center gap-1 bg-slate-800/60 p-1 rounded-xl border border-slate-700/50">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => handleNavClick(item.id)}
                  className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                    isActive
                      ? 'bg-sky-600 text-white shadow-md shadow-sky-900/30'
                      : 'text-slate-300 hover:text-white hover:bg-slate-700/50'
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
                  ? 'bg-emerald-950/50 text-emerald-400 border-emerald-700/40'
                  : 'bg-amber-950/50 text-amber-400 border-amber-700/40'
              }`}
              data-testid="online-status-badge"
            >
              {isOnline ? (
                <>
                  <Wifi className="w-3.5 h-3.5" />
                  <span>{strings.header.statusOnline}</span>
                </>
              ) : (
                <>
                  <WifiOff className="w-3.5 h-3.5" />
                  <span>{strings.header.statusOffline}</span>
                </>
              )}
            </div>

            {/* Theme Toggle Button */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800 border border-slate-700/60 transition-colors"
              title={theme === 'dark' ? strings.header.toggleThemeLight : strings.header.toggleThemeDark}
              aria-label={theme === 'dark' ? strings.header.toggleThemeLight : strings.header.toggleThemeDark}
              data-testid="theme-toggle-button"
            >
              {theme === 'dark' ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-300" />}
            </button>

            {/* Mobile Menu Toggle Button */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800 border border-slate-700/60 transition-colors"
              aria-label="Toggle navigation menu"
              data-testid="mobile-menu-button"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {mobileMenuOpen && (
          <div className="md:hidden mt-3 pt-3 border-t border-slate-800 flex flex-col gap-1 pb-2" data-testid="mobile-menu-drawer">
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
                      : 'text-slate-300 hover:text-white hover:bg-slate-800'
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

      {/* Main Content Body */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 py-6">
        {children}
      </main>

      {/* Tactical Footer */}
      <footer className="w-full bg-slate-950 border-t border-slate-800/80 text-slate-400 py-8 px-4 mt-auto">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4 text-center md:text-left">
          <div className="flex items-center gap-3">
            <ShieldCheck className="w-5 h-5 text-sky-400" />
            <span className="text-sm font-bold text-slate-200 tracking-wide">{strings.common.appName}</span>
            <span className="text-xs text-slate-500">| {strings.common.appTagline}</span>
          </div>
          <p className="text-xs text-slate-500">
            &copy; {new Date().getFullYear()} {strings.common.appName}. Tactical Command Platform for Defence Officers.
          </p>
        </div>
      </footer>
    </div>
  );
};

export default AppLayout;
