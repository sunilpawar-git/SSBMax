import { FC } from 'react';
import { Shield, Sparkles, ArrowRight, CheckCircle2, Award, Zap, BarChart } from 'lucide-react';
import { strings } from '../../constants/strings';

export interface HeroSectionProps {
  onStartFreeClick?: () => void;
  onUnlockProClick?: () => void;
}

export const HeroSection: FC<HeroSectionProps> = ({ onStartFreeClick, onUnlockProClick }) => {
  return (
    <section className="relative overflow-hidden py-12 md:py-20 flex flex-col items-center text-center" data-testid="hero-section">
      {/* Glow Background Gradient Orbs */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute top-1/3 right-10 w-72 h-72 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 max-w-4xl mx-auto px-4 flex flex-col items-center">
        {/* Tactical Badge Header */}
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-slate-800/90 border border-sky-500/30 text-sky-400 text-xs font-bold tracking-wide uppercase shadow-lg shadow-sky-950/40 mb-6">
          <Sparkles className="w-4 h-4 text-amber-400" />
          <span>{strings.landing.heroBadge}</span>
        </div>

        {/* Main Headline */}
        <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-white leading-tight mb-6">
          {strings.landing.heroTitle.split('AI Precision')[0]}
          <span className="bg-gradient-to-r from-sky-400 via-sky-300 to-amber-400 bg-clip-text text-transparent">
            AI Precision
          </span>
        </h1>

        {/* Subtitle */}
        <p className="text-lg md:text-xl text-slate-300 max-w-2xl font-normal leading-relaxed mb-8">
          {strings.landing.heroSubtitle}
        </p>

        {/* Call-to-Action Buttons */}
        <div className="flex flex-col sm:flex-row items-center gap-4 w-full justify-center mb-12">
          <button
            onClick={onStartFreeClick}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-8 py-4 bg-gradient-to-r from-sky-600 to-blue-600 hover:from-sky-500 hover:to-blue-500 text-white font-bold text-base rounded-xl shadow-xl shadow-sky-900/40 transform hover:-translate-y-0.5 transition-all"
            data-testid="start-free-btn"
          >
            <span>{strings.landing.startFree}</span>
            <ArrowRight className="w-5 h-5" />
          </button>

          <button
            onClick={onUnlockProClick}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-8 py-4 bg-slate-800/80 hover:bg-slate-800 text-amber-400 border border-amber-500/40 font-bold text-base rounded-xl shadow-lg transition-all"
            data-testid="unlock-pro-btn"
          >
            <Award className="w-5 h-5 text-amber-400" />
            <span>{strings.landing.unlockPro}</span>
          </button>
        </div>

        {/* Key Tactical Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 w-full max-w-3xl">
          <div className="flex items-center gap-3 p-4 rounded-2xl bg-slate-800/50 border border-slate-700/60 text-left shadow-md backdrop-blur-sm">
            <div className="p-2.5 rounded-xl bg-sky-500/10 text-sky-400 border border-sky-500/20">
              <BarChart className="w-6 h-6" />
            </div>
            <div>
              <p className="text-sm font-bold text-white">{strings.landing.statOlq}</p>
              <p className="text-xs text-slate-400">Psychology & GTO Metrics</p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-4 rounded-2xl bg-slate-800/50 border border-slate-700/60 text-left shadow-md backdrop-blur-sm">
            <div className="p-2.5 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20">
              <Shield className="w-6 h-6" />
            </div>
            <div>
              <p className="text-sm font-bold text-white">{strings.landing.statStage}</p>
              <p className="text-xs text-slate-400">Screening to Interview</p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-4 rounded-2xl bg-slate-800/50 border border-slate-700/60 text-left shadow-md backdrop-blur-sm">
            <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <Zap className="w-6 h-6" />
            </div>
            <div>
              <p className="text-sm font-bold text-white">{strings.landing.statAi}</p>
              <p className="text-xs text-slate-400">Confidential Assessor Dossier</p>
            </div>
          </div>
        </div>

        {/* Defence Standards Notice */}
        <div className="mt-8 flex items-center justify-center gap-2 text-xs text-slate-400 font-medium">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{strings.landing.featureSubtitle}</span>
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
