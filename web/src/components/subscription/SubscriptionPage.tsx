import { FC } from 'react';
import { Check, ShieldCheck, Zap, Lock, AlertCircle, Award } from 'lucide-react';
import { strings } from '../../constants/strings';
import { usePaymentViewModel, PaymentState } from '../../viewmodels/PaymentViewModel';

export interface SubscriptionPageProps {
  initialState?: Partial<PaymentState>;
  onPaymentSuccess?: () => void;
  createOrderFn?: (planId: string) => Promise<{ orderId: string; amount: number; currency: string; keyId: string }>;
}

export const SubscriptionPage: FC<SubscriptionPageProps> = ({
  onPaymentSuccess,
  createOrderFn
}) => {
  const paymentVM = usePaymentViewModel(createOrderFn);
  const { status, errorMessage, isPaidMember, initiatePayment } = paymentVM;

  const handleUpgradeClick = async () => {
    await initiatePayment('pro_monthly');
    if (onPaymentSuccess && status === 'success') {
      onPaymentSuccess();
    }
  };

  const freeFeatures = [
    strings.subscription.freeFeature1,
    strings.subscription.freeFeature2,
    strings.subscription.freeFeature3
  ];

  const proFeatures = [
    strings.subscription.proFeature1,
    strings.subscription.proFeature2,
    strings.subscription.proFeature3,
    strings.subscription.proFeature4,
    strings.subscription.proFeature5
  ];

  return (
    <div className="w-full space-y-8" data-testid="subscription-page">
      {/* Header Banner */}
      <div className="text-center space-y-3">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/10 border border-sky-500/30 text-sky-400 text-xs font-bold uppercase tracking-wider">
          <Award className="w-4 h-4" />
          <span>{strings.subscription.title}</span>
        </div>
        <h1 className="text-3xl font-black tracking-tight text-white sm:text-4xl">
          {strings.subscription.title}
        </h1>
        <p className="text-slate-400 text-sm max-w-2xl mx-auto">
          {strings.subscription.subtitle}
        </p>
      </div>

      {/* Success Notification Banner */}
      {isPaidMember && (
        <div className="p-4 rounded-xl bg-emerald-950/40 border border-emerald-500/40 text-emerald-300 flex items-center gap-3" data-testid="subscription-success-banner">
          <ShieldCheck className="w-6 h-6 text-emerald-400 shrink-0" />
          <div>
            <h4 className="font-bold text-sm text-emerald-200">{strings.subscription.successBadge}</h4>
            <p className="text-xs text-emerald-400/90">{strings.subscription.successMessage}</p>
          </div>
        </div>
      )}

      {/* Error Notification Banner */}
      {status === 'error' && errorMessage && (
        <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-500/40 text-rose-300 flex items-center gap-3" data-testid="subscription-error-banner">
          <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
          <p className="text-xs font-medium">{errorMessage}</p>
        </div>
      )}

      {/* Pricing Tier Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-5xl mx-auto">
        {/* Tier 1: Free Cadet Pass */}
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between space-y-6 hover:border-slate-700 transition-all">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-bold text-white">{strings.subscription.freePlanTitle}</h3>
              <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-800 text-slate-400 border border-slate-700">
                {strings.subscription.freePlanBadge}
              </span>
            </div>
            <div className="text-3xl font-black text-white">
              {strings.subscription.freePlanPrice}
            </div>
            <ul className="space-y-3 pt-2">
              {freeFeatures.map((feat, idx) => (
                <li key={idx} className="flex items-start gap-2.5 text-xs text-slate-300">
                  <Check className="w-4 h-4 text-slate-500 shrink-0 mt-0.5" />
                  <span>{feat}</span>
                </li>
              ))}
            </ul>
          </div>
          <button
            disabled
            className="w-full py-3 rounded-xl bg-slate-800 text-slate-400 font-semibold text-xs border border-slate-700/50 cursor-not-allowed opacity-80"
          >
            {strings.subscription.currentPlan}
          </button>
        </div>

        {/* Tier 2: Pro Officer Pass */}
        <div className="p-6 rounded-2xl bg-gradient-to-b from-slate-900 to-slate-900/90 border-2 border-sky-500/50 relative shadow-xl shadow-sky-950/50 flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-bold text-white flex items-center gap-2">
                <span>{strings.subscription.proPlanTitle}</span>
                <Zap className="w-4 h-4 text-amber-400" />
              </h3>
              <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-amber-500/20 text-amber-300 border border-amber-500/30 tracking-wider">
                {strings.subscription.proPlanBadge}
              </span>
            </div>
            <div className="text-3xl font-black text-sky-400">
              {strings.subscription.proPlanPrice}
            </div>
            <ul className="space-y-3 pt-2">
              {proFeatures.map((feat, idx) => (
                <li key={idx} className="flex items-start gap-2.5 text-xs text-slate-200">
                  <Check className="w-4 h-4 text-sky-400 shrink-0 mt-0.5" />
                  <span>{feat}</span>
                </li>
              ))}
            </ul>
          </div>

          <button
            onClick={handleUpgradeClick}
            disabled={status === 'creating_order' || status === 'checkout_open' || isPaidMember}
            className={`w-full py-3.5 rounded-xl font-bold text-xs uppercase tracking-wider transition-all flex items-center justify-center gap-2 ${
              isPaidMember
                ? 'bg-emerald-600/30 text-emerald-300 border border-emerald-500/40 cursor-default'
                : 'bg-gradient-to-r from-sky-600 to-sky-500 hover:from-sky-500 hover:to-sky-400 text-white shadow-lg shadow-sky-900/40 active:scale-[0.99]'
            }`}
            data-testid="upgrade-pro-button"
          >
            {status === 'creating_order' || status === 'checkout_open' ? (
              <span>{strings.subscription.processing}</span>
            ) : isPaidMember ? (
              <>
                <ShieldCheck className="w-4 h-4" />
                <span>{strings.subscription.successBadge}</span>
              </>
            ) : (
              <>
                <Zap className="w-4 h-4" />
                <span>{strings.subscription.upgradeNow}</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Security & Guarantee Note */}
      <div className="flex items-center justify-center gap-2 text-xs text-slate-500 text-center pt-4">
        <Lock className="w-3.5 h-3.5 text-slate-400" />
        <span>{strings.subscription.guarantee}</span>
      </div>
    </div>
  );
};

export default SubscriptionPage;
