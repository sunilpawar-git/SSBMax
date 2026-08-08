import React, { useState } from 'react';
import { strings } from '../../constants/strings';
import { InteractiveRadarChart, OLQRadarItem } from '../evaluation/InteractiveRadarChart';
import { PsychologistDossier, PsychologistDossierData } from '../evaluation/PsychologistDossier';
import { OLQScoreCard, OLQScoreItem } from '../evaluation/OLQScoreCard';
import { Sparkles, Lock, ArrowRight, History } from 'lucide-react';

export interface AIReportAttempt {
  id: string;
  testType: string;
  date: string;
  scoreOrRating: string;
}

export interface AIReportsPageProps {
  userReports?: {
    olqScores: OLQScoreItem[];
    dossier: PsychologistDossierData;
  } | null;
  recentAttempts?: AIReportAttempt[];
  isGuest?: boolean;
  onSignIn?: () => void;
  onStartTest?: () => void;
}

const SAMPLE_OLQ_SCORES: OLQScoreItem[] = [
  { olq: 'EFFECTIVE_INTELLIGENCE', score: 8.5, reasoning: 'Demonstrated analytical clarity in tactical scenario reactions.' },
  { olq: 'REASONING_ABILITY', score: 8.0, reasoning: 'Logically structured response patterns during situation tests.' },
  { olq: 'ORGANIZING_ABILITY', score: 7.5, reasoning: 'Organized resources effectively in team scenarios.' },
  { olq: 'POWER_OF_EXPRESSION', score: 8.2, reasoning: 'Articulate narrative structure in TAT stories.' },
  { olq: 'SOCIAL_ADJUSTMENT', score: 7.8, reasoning: 'Adaptable social demeanor across diverse situations.' },
  { olq: 'COOPERATION', score: 8.4, reasoning: 'Strong group cohesion orientation.' },
  { olq: 'SENSE_OF_RESPONSIBILITY', score: 8.6, reasoning: 'Proactive ownership of group goals.' },
  { olq: 'INITIATIVE', score: 8.1, reasoning: 'Spontaneous problem-solving responses.' },
  { olq: 'SELF_CONFIDENCE', score: 7.9, reasoning: 'Poised and decisive tone.' },
  { olq: 'SPEED_OF_DECISION', score: 7.6, reasoning: 'Prompt reaction times in SRT.' },
  { olq: 'INFLUENCE_GROUP', score: 7.7, reasoning: 'Natural group influence and direction.' },
  { olq: 'LIVELINESS', score: 8.0, reasoning: 'Energetic and positive outlook.' },
  { olq: 'DETERMINATION', score: 8.3, reasoning: 'Persistent approach to obstacles.' },
  { olq: 'COURAGE', score: 8.2, reasoning: 'Moral and physical courage in situation responses.' },
  { olq: 'STAMINA', score: 7.8, reasoning: 'Sustained focus throughout 60-situation battery.' }
];

const SAMPLE_DOSSIER: PsychologistDossierData = {
  candidateId: 'CADET-DEMO-2026',
  assessedDate: '2026-08-08',
  recommendationStatus: 'RECOMMENDED',
  executiveSummary: 'Candidate exhibits high intellectual efficacy, sound reasoning under pressure, and strong social adaptability. Psychological responses reflect maturity, moral courage, and alignment with Services Selection Board expectations.',
  keyStrengths: [
    'Quick decision-making in high-stress situations',
    'Clear and articulate expression in TAT stories',
    'High sense of responsibility and team orientation'
  ],
  areasOfConcern: [
    'Slight tendency to rush initial organizing steps under tight time bounds'
  ],
  suggestedProbes: [
    'Probe candidate on handling conflicting priorities during group tasks',
    'Evaluate delegation strategies when managing subordinate teams'
  ]
};

export const AIReportsPage: React.FC<AIReportsPageProps> = ({
  userReports,
  recentAttempts = [],
  isGuest = false,
  onSignIn,
  onStartTest
}) => {
  const hasUserReports = !!(userReports && userReports.olqScores.length > 0);
  const [showSampleMode, setShowSampleMode] = useState(!hasUserReports);

  const activeOlqScores = showSampleMode || !hasUserReports ? SAMPLE_OLQ_SCORES : userReports.olqScores;
  const activeDossier = showSampleMode || !hasUserReports ? SAMPLE_DOSSIER : userReports.dossier;

  const radarItems: OLQRadarItem[] = activeOlqScores.map((s) => ({
    olq: s.olq,
    score: s.score,
    reasoning: s.reasoning
  }));

  return (
    <div className="max-w-6xl mx-auto space-y-8 p-4 sm:p-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 rounded-2xl bg-[var(--color-bg-card)] border border-[var(--color-border)] shadow-sm">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Sparkles className="w-5 h-5 text-[var(--color-accent)]" />
            <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-accent)]">
              {strings.reportsPage.title}
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)]">
            {strings.reportsPage.title}
          </h1>
          <p className="text-sm text-[var(--color-text-secondary)] mt-1 max-w-2xl">
            {strings.reportsPage.subtitle}
          </p>
        </div>

        {hasUserReports && (
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowSampleMode(!showSampleMode)}
              className="px-4 py-2 text-xs font-semibold rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-elevated)] text-[var(--color-text-primary)] hover:border-[var(--color-accent)] transition-all"
            >
              {showSampleMode ? strings.reportsPage.toggleUser : strings.reportsPage.toggleSample}
            </button>
          </div>
        )}
      </div>

      {/* Guest Auth Banner */}
      {isGuest && (
        <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <Lock className="w-5 h-5 text-[var(--color-warning)] shrink-0" />
            <p className="text-xs text-[var(--color-text-primary)]">{strings.reportsPage.guestBanner}</p>
          </div>
          {onSignIn && (
            <button
              onClick={onSignIn}
              className="px-4 py-2 rounded-lg bg-[var(--color-accent)] text-white text-xs font-bold hover:opacity-90 shrink-0 transition-opacity"
            >
              {strings.reportsPage.signInAction}
            </button>
          )}
        </div>
      )}

      {/* Sample Banner Notice */}
      {(showSampleMode || !hasUserReports) && (
        <div className="p-4 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border-subtle)] flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="px-2.5 py-1 rounded text-[10px] font-black bg-[var(--color-accent)]/20 text-[var(--color-accent)]">
              {strings.reportsPage.sampleBadge}
            </span>
            <p className="text-xs text-[var(--color-text-secondary)]">{strings.reportsPage.sampleBanner}</p>
          </div>
          {onStartTest && (
            <button
              onClick={onStartTest}
              className="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-[var(--color-accent)] text-white text-xs font-bold hover:opacity-90 transition-opacity shrink-0"
            >
              {strings.reportsPage.startTest}
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      )}

      {/* 15 OLQ Radar Chart */}
      <InteractiveRadarChart scores={radarItems} />

      {/* Military Psychologist Dossier */}
      <PsychologistDossier dossier={activeDossier} />

      {/* Detailed OLQ Factor Breakdown */}
      <OLQScoreCard olqScores={activeOlqScores} overallConfidence={88} />

      {/* Recent Test History */}
      <div className="p-6 rounded-2xl bg-[var(--color-bg-card)] border border-[var(--color-border)] shadow-sm space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-[var(--color-border)]">
          <h3 className="text-lg font-bold text-[var(--color-text-primary)] flex items-center gap-2">
            <History className="w-5 h-5 text-[var(--color-accent)]" />
            {strings.reportsPage.historyTitle}
          </h3>
          {onStartTest && (
            <button
              onClick={onStartTest}
              className="text-xs font-semibold text-[var(--color-accent)] hover:underline flex items-center gap-1"
            >
              {strings.reportsPage.startTest}
              <ArrowRight className="w-3 h-3" />
            </button>
          )}
        </div>

        {recentAttempts.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            {recentAttempts.map((attempt) => (
              <div
                key={attempt.id}
                className="p-4 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border-subtle)] space-y-1"
              >
                <div className="flex justify-between items-center text-xs font-bold text-[var(--color-text-primary)]">
                  <span>{attempt.testType}</span>
                  <span className="text-[var(--color-accent)]">{attempt.scoreOrRating}</span>
                </div>
                <p className="text-[11px] text-[var(--color-text-muted)]">{attempt.date}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-[var(--color-text-muted)] italic text-center py-4">
            {strings.reportsPage.noHistory}
          </p>
        )}
      </div>
    </div>
  );
};
