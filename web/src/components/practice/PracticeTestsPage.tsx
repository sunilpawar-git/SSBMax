import { FC, useState } from 'react';
import { Target, Play, Lock, Search } from 'lucide-react';
import { strings } from '../../constants/strings';

export interface PracticeTestsPageProps {
  isPaidMember?: boolean;
  onStartTest?: (testType: 'oir' | 'ppdt' | 'psychology' | 'tat' | 'wat' | 'srt' | 'sd') => void;
  onUpgrade?: () => void;
}

interface TestItem {
  id: 'oir' | 'ppdt' | 'psychology' | 'tat' | 'wat' | 'srt' | 'sd';
  title: string;
  desc: string;
  stage: 'stage1' | 'stage2' | 'stage3';
  isPro: boolean;
  timeLimit: string;
}

export const PracticeTestsPage: FC<PracticeTestsPageProps> = ({
  isPaidMember = false,
  onStartTest,
  onUpgrade
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  const testList: TestItem[] = [
    { id: 'oir', title: strings.practice.oirTitle, desc: strings.practice.oirDesc, stage: 'stage1', isPro: false, timeLimit: '50 Qs / 30m' },
    { id: 'ppdt', title: strings.practice.ppdtTitle, desc: strings.practice.ppdtDesc, stage: 'stage1', isPro: true, timeLimit: '30s view / 4m write' },
    { id: 'psychology', title: 'Full Psychology Battery', desc: 'Complete TAT, WAT, SRT, and SD in one timed session.', stage: 'stage2', isPro: true, timeLimit: 'Full 4-Test Battery' },
    { id: 'tat', title: strings.practice.tatTitle, desc: strings.practice.tatDesc, stage: 'stage2', isPro: true, timeLimit: '12 Slides / 48m' },
    { id: 'wat', title: strings.practice.watTitle, desc: strings.practice.watDesc, stage: 'stage2', isPro: true, timeLimit: '60 Words / 15m' },
    { id: 'srt', title: strings.practice.srtTitle, desc: strings.practice.srtDesc, stage: 'stage2', isPro: true, timeLimit: '60 Situations / 30m' },
    { id: 'sd', title: strings.practice.sdTitle, desc: strings.practice.sdDesc, stage: 'stage2', isPro: true, timeLimit: '5 Paragraphs / 15m' }
  ];

  const filteredTests = testList.filter(
    (t) =>
      t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.desc.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleAction = (test: TestItem) => {
    if (test.isPro && !isPaidMember) {
      onUpgrade?.();
    } else {
      onStartTest?.(test.id);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300" data-testid="practice-tests-page">
      {/* Header Banner */}
      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-lg backdrop-blur-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-xs font-bold text-sky-400 uppercase tracking-widest mb-1">
              <Target className="w-4 h-4" />
              <span>{strings.nav.practice}</span>
            </div>
            <h1 className="text-2xl font-black text-white tracking-tight">{strings.practice.title}</h1>
            <p className="text-xs text-slate-400 mt-1 max-w-2xl">{strings.practice.subtitle}</p>
          </div>

          <div className="relative w-full md:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={strings.practice.searchPlaceholder}
              className="w-full bg-slate-900/80 border border-slate-700/80 rounded-xl pl-9 pr-4 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
              data-testid="search-input"
            />
          </div>
        </div>
      </div>

      {/* Stage 1 Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <span className="px-2 py-0.5 rounded bg-sky-500/20 text-sky-400 border border-sky-500/30 text-[10px] uppercase font-mono">
              Stage I
            </span>
            {strings.practice.stage1Title}
          </h2>
          <span className="text-xs text-slate-400">{strings.practice.stage1Sub}</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredTests
            .filter((t) => t.stage === 'stage1')
            .map((test) => (
              <div
                key={test.id}
                className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-5 flex flex-col justify-between hover:border-slate-600 transition-all shadow-md"
                data-testid={`test-card-${test.id}`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                        test.isPro
                          ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                          : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                      }`}
                    >
                      {test.isPro ? strings.practice.proBadge : strings.practice.freeBadge}
                    </span>
                    <span className="text-[10px] text-slate-400 font-mono">{test.timeLimit}</span>
                  </div>
                  <h3 className="text-base font-bold text-white mb-1">{test.title}</h3>
                  <p className="text-xs text-slate-400 leading-relaxed mb-4">{test.desc}</p>
                </div>

                <button
                  onClick={() => handleAction(test)}
                  className={`w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl font-bold text-xs transition-all shadow-md ${
                    test.isPro && !isPaidMember
                      ? 'bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 border border-amber-500/40'
                      : 'bg-sky-600 hover:bg-sky-500 text-white'
                  }`}
                  data-testid={`launch-test-${test.id}`}
                >
                  {test.isPro && !isPaidMember ? (
                    <>
                      <Lock className="w-3.5 h-3.5" />
                      <span>{strings.practice.proRequired}</span>
                    </>
                  ) : (
                    <>
                      <Play className="w-3.5 h-3.5" />
                      <span>{strings.practice.startTest}</span>
                    </>
                  )}
                </button>
              </div>
            ))}
        </div>
      </div>

      {/* Stage 2 Section */}
      <div className="space-y-3 pt-2">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <span className="px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 border border-amber-500/30 text-[10px] uppercase font-mono">
              Stage II
            </span>
            {strings.practice.stage2Title}
          </h2>
          <span className="text-xs text-slate-400">{strings.practice.stage2Sub}</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredTests
            .filter((t) => t.stage === 'stage2')
            .map((test) => (
              <div
                key={test.id}
                className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-5 flex flex-col justify-between hover:border-slate-600 transition-all shadow-md"
                data-testid={`test-card-${test.id}`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 border border-amber-500/30 text-[10px] font-bold uppercase tracking-wider">
                      {strings.practice.proBadge}
                    </span>
                    <span className="text-[10px] text-slate-400 font-mono">{test.timeLimit}</span>
                  </div>
                  <h3 className="text-sm font-bold text-white mb-1">{test.title}</h3>
                  <p className="text-xs text-slate-400 leading-relaxed mb-4">{test.desc}</p>
                </div>

                <button
                  onClick={() => handleAction(test)}
                  className={`w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl font-bold text-xs transition-all shadow-md ${
                    !isPaidMember
                      ? 'bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 border border-amber-500/40'
                      : 'bg-amber-600 hover:bg-amber-500 text-white'
                  }`}
                  data-testid={`launch-test-${test.id}`}
                >
                  {!isPaidMember ? (
                    <>
                      <Lock className="w-3.5 h-3.5" />
                      <span>{strings.practice.proRequired}</span>
                    </>
                  ) : (
                    <>
                      <Play className="w-3.5 h-3.5" />
                      <span>{strings.practice.startTest}</span>
                    </>
                  )}
                </button>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
};

export default PracticeTestsPage;
