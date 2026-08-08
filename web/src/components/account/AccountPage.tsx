import { FC, useState, useEffect } from 'react';
import { User, CreditCard, LogOut, Edit3, Award, Target, CheckCircle2 } from 'lucide-react';
import { strings } from '../../constants/strings';
import { authService, UserProfile } from '../../services/AuthService';

export interface AccountPageProps {
  user?: UserProfile | null;
  onSignOut?: () => void;
  onEditDiagnostic?: () => void;
  onUpgradeClick?: () => void;
}

export const AccountPage: FC<AccountPageProps> = ({
  user: initialUser,
  onSignOut,
  onEditDiagnostic,
  onUpgradeClick
}) => {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(initialUser ?? null);

  useEffect(() => {
    if (initialUser !== undefined) {
      setCurrentUser(initialUser);
      return;
    }
    const user = authService.getCurrentUser();
    setCurrentUser(user);
    const unsubscribe = authService.onAuthStateChanged((u) => {
      setCurrentUser(u);
    });
    return () => unsubscribe();
  }, [initialUser]);

  const handleSignOut = async () => {
    if (onSignOut) {
      onSignOut();
    } else {
      await authService.signOut();
    }
  };

  const isPro = currentUser?.isPaidMember ?? false;
  const initials = currentUser?.displayName
    ? currentUser.displayName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2)
    : 'CD';

  return (
    <div className="w-full max-w-4xl mx-auto space-y-8" data-testid="account-page">
      {/* Header Banner */}
      <div className="text-center space-y-3">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/10 border border-sky-500/30 text-sky-400 text-xs font-bold uppercase tracking-wider">
          <User className="w-4 h-4" />
          <span>{strings.account.title}</span>
        </div>
        <h1 className="text-3xl font-black tracking-tight text-white sm:text-4xl">
          {strings.account.title}
        </h1>
        <p className="text-slate-400 text-sm max-w-2xl mx-auto">
          {strings.account.subtitle}
        </p>
      </div>

      {/* Main Candidate Card */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 shadow-xl space-y-6">
        {/* Profile Info Header */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-6 pb-6 border-b border-slate-800">
          <div className="flex items-center gap-4 text-center sm:text-left">
            {currentUser?.photoURL ? (
              <img
                src={currentUser.photoURL}
                alt={currentUser.displayName || 'Candidate Profile'}
                className="w-16 h-16 rounded-2xl border-2 border-sky-500/40 object-cover shadow-md"
                data-testid="user-avatar"
              />
            ) : (
              <div
                className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-sky-600 to-sky-400 flex items-center justify-center text-white font-black text-xl shadow-md"
                data-testid="user-initials"
              >
                {initials}
              </div>
            )}
            <div>
              <div className="flex items-center gap-2 justify-center sm:justify-start">
                <h2 className="text-xl font-bold text-white" data-testid="user-name">
                  {currentUser?.displayName || 'Candidate Officer'}
                </h2>
                <span
                  className={`px-2 py-0.5 rounded text-[10px] font-extrabold uppercase tracking-wider border ${
                    isPro
                      ? 'bg-amber-500/20 text-amber-400 border-amber-500/30'
                      : 'bg-slate-800 text-slate-400 border-slate-700'
                  }`}
                  data-testid="membership-badge"
                >
                  {isPro ? strings.subscription.proPlanTitle : strings.subscription.freePlanTitle}
                </span>
              </div>
              <p className="text-xs text-slate-400 font-medium" data-testid="user-email">
                {currentUser?.email || 'cadet@ssbmax.in'}
              </p>
              <div className="flex items-center gap-1.5 text-[11px] text-emerald-400 font-semibold mt-1">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>{currentUser ? strings.account.signedIn : strings.account.guestUser}</span>
              </div>
            </div>
          </div>

          <button
            onClick={handleSignOut}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-rose-950/40 text-slate-300 hover:text-rose-400 border border-slate-700 hover:border-rose-500/30 text-xs font-semibold transition-all flex items-center gap-2 shrink-0"
            data-testid="sign-out-button"
          >
            <LogOut className="w-4 h-4" />
            <span>{strings.account.signOut}</span>
          </button>
        </div>

        {/* Account Details & Metadata Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          {/* Card 1: Diagnostic Parameters */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-sky-400 flex items-center gap-2">
                <Target className="w-4 h-4" />
                <span>{strings.dashboard.targetBoard}</span>
              </h3>
              {onEditDiagnostic && (
                <button
                  onClick={onEditDiagnostic}
                  className="text-xs text-sky-400 hover:text-sky-300 font-semibold flex items-center gap-1"
                  data-testid="edit-diagnostic-button"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                  <span>{strings.account.editDiagnostic}</span>
                </button>
              )}
            </div>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between py-1 border-b border-slate-800/60">
                <span className="text-slate-400">{strings.account.targetBoard}:</span>
                <span className="font-semibold text-slate-200" data-testid="target-board-value">
                  {strings.diagnostic.boardArmy}
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-800/60">
                <span className="text-slate-400">{strings.account.entryStream}:</span>
                <span className="font-semibold text-slate-200" data-testid="entry-stream-value">
                  {strings.diagnostic.entryCds}
                </span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-400">{strings.account.prepStatus}:</span>
                <span className="font-semibold text-slate-200" data-testid="prep-status-value">
                  {strings.diagnostic.prepIntermediate}
                </span>
              </div>
            </div>
          </div>

          {/* Card 2: Pass & Security Credentials */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-amber-400 flex items-center gap-2">
                <CreditCard className="w-4 h-4" />
                <span>{strings.account.membershipTier}</span>
              </h3>
              {onUpgradeClick && !isPro && (
                <button
                  onClick={onUpgradeClick}
                  className="text-xs text-amber-400 hover:text-amber-300 font-semibold flex items-center gap-1"
                  data-testid="upgrade-pass-button"
                >
                  <Award className="w-3.5 h-3.5" />
                  <span>{strings.account.upgradeTier}</span>
                </button>
              )}
            </div>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between py-1 border-b border-slate-800/60">
                <span className="text-slate-400">{strings.account.membershipTier}:</span>
                <span className="font-bold text-white" data-testid="tier-status">
                  {isPro ? strings.subscription.proPlanTitle : strings.subscription.freePlanTitle}
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-800/60">
                <span className="text-slate-400">{strings.account.authStatus}:</span>
                <span className="font-semibold text-slate-200">
                  {currentUser ? strings.account.signedIn : strings.account.guestUser}
                </span>
              </div>
              <div className="flex justify-between py-1 truncate">
                <span className="text-slate-400">{strings.account.uidLabel}:</span>
                <span className="font-mono text-[11px] text-slate-400 truncate max-w-[160px]" data-testid="user-uid">
                  {currentUser?.uid || 'guest-ssbmax-session'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AccountPage;
