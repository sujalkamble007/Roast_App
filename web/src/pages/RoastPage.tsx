import type { FC } from 'react';
import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import confetti from 'canvas-confetti';
import {
  AlertCircle,
  Check,
  Clock,
  Copy,
  Flame,
  Share2,
  Zap,
} from 'lucide-react';
import { GithubIcon, SpotifyIcon, YoutubeIcon } from '../components/Icons';
import { api, ApiError } from '../api/client';
import type { QuotaInfo, Roast, Source, Tone, User } from '../api/types';

interface RoastPageProps {
  user: User;
  quota?: QuotaInfo;
  onRefreshQuota: () => void;
  onNavigate: (route: string) => void;
}

const TONES: { id: Tone; label: string; desc: string; emoji: string; color: string }[] = [
  {
    id: 'BRUTAL',
    label: 'Brutal',
    desc: 'Existential humiliation with zero mercy.',
    emoji: '💀',
    color: 'from-rose-600 to-red-700 border-red-500/40 text-rose-400',
  },
  {
    id: 'SARCASTIC',
    label: 'Sarcastic',
    desc: 'Dry developer cynicism and witty eye-rolls.',
    emoji: '😏',
    color: 'from-amber-600 to-orange-700 border-amber-500/40 text-amber-400',
  },
  {
    id: 'WHOLESOME',
    label: 'Wholesome',
    desc: 'Affectionate teasing wrapped in warm appreciation.',
    emoji: '✨',
    color: 'from-emerald-600 to-teal-700 border-emerald-500/40 text-emerald-400',
  },
  {
    id: 'FRIENDLY',
    label: 'Friendly',
    desc: 'Playful ribbing like close friends over drinks.',
    emoji: '🤝',
    color: 'from-blue-600 to-cyan-700 border-blue-500/40 text-cyan-400',
  },
];

const LOADING_MESSAGES = [
  'Querying 3 AM GitHub commits...',
  'Analyzing questionable late-night Spotify playlists...',
  'Judging your 200+ unread YouTube subscriptions...',
  'Consulting with senior comedy writers...',
  'Calibrating humiliation parameters in LLaMA 3.3 70B...',
  'Synthesizing emotional damage...',
];

export const RoastPage: FC<RoastPageProps> = ({
  quota,
  onRefreshQuota,
  onNavigate,
}) => {
  // Form State
  const [selectedSources, setSelectedSources] = useState<Source[]>(['github', 'spotify']);
  const [selectedTone, setSelectedTone] = useState<Tone>('BRUTAL');
  const [activeRoastId, setActiveRoastId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [rateLimitSeconds, setRateLimitSeconds] = useState<number | null>(null);
  const [loadingMsgIdx, setLoadingMsgIdx] = useState(0);

  // Sharing State
  const [shareUrl, setShareUrl] = useState<string | null>(null);
  const [isSharing, setIsSharing] = useState(false);
  const [copied, setCopied] = useState(false);

  // Cycling humorous loading messages
  useEffect(() => {
    if (!activeRoastId) return;
    const interval = setInterval(() => {
      setLoadingMsgIdx((prev) => (prev + 1) % LOADING_MESSAGES.length);
    }, 1500);
    return () => clearInterval(interval);
  }, [activeRoastId]);

  // Rate limit countdown timer
  useEffect(() => {
    if (rateLimitSeconds === null || rateLimitSeconds <= 0) return;
    const interval = setInterval(() => {
      setRateLimitSeconds((prev) => (prev && prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [rateLimitSeconds]);

  // Polling active roast with TanStack Query (1.5s interval as mandated by Plan.md)
  const { data: roastData } = useQuery<Roast>({
    queryKey: ['roast', activeRoastId],
    queryFn: () => api.getRoast(activeRoastId!),
    enabled: !!activeRoastId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'READY' || status === 'FAILED') return false;
      return 1500; // 1.5 seconds polling interval
    },
  });

  // Confetti trigger when status becomes READY
  useEffect(() => {
    if (roastData?.status === 'READY') {
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#f97316', '#fb923c', '#ec4899', '#8b5cf6'],
      });
      onRefreshQuota();
    }
  }, [roastData?.status]);

  const toggleSource = (source: Source) => {
    if (selectedSources.includes(source)) {
      if (selectedSources.length === 1) return; // Keep at least one
      setSelectedSources(selectedSources.filter((s) => s !== source));
    } else {
      setSelectedSources([...selectedSources, source]);
    }
  };

  const handleGenerate = async () => {
    setErrorMessage(null);
    setShareUrl(null);
    setRateLimitSeconds(null);

    try {
      const idempotencyKey = `idemp-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      const initiated = await api.createRoast(
        {
          sources: selectedSources,
          tone: selectedTone,
        },
        idempotencyKey
      );

      setActiveRoastId(initiated.id);
    } catch (err: any) {
      if (err instanceof ApiError && err.status === 429) {
        setRateLimitSeconds(err.retryAfterSeconds || 3600);
        setErrorMessage(err.message || 'Daily quota limit reached.');
      } else {
        setErrorMessage(err.message || 'Could not initiate roast. Please try again.');
      }
    }
  };

  const handleShare = async () => {
    if (!roastData) return;
    setIsSharing(true);
    try {
      const res = await api.shareRoast(roastData.id);
      const fullShareUrl = window.location.origin + res.shareUrl;
      setShareUrl(fullShareUrl);
      await navigator.clipboard.writeText(fullShareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 3000);
    } catch (err: any) {
      alert('Failed to generate share link: ' + err.message);
    } finally {
      setIsSharing(false);
    }
  };

  const isGenerating = !!activeRoastId && roastData?.status === 'PENDING';
  const isReady = roastData?.status === 'READY';
  const isFailed = roastData?.status === 'FAILED';
  const isOutOfQuota = quota ? quota.remaining <= 0 : false;

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 relative">
      <div className="glow-orange -top-10 -right-20 opacity-50" />

      {/* Header */}
      <div className="text-center mb-8">
        <h1 className="text-3xl sm:text-5xl font-black font-['Outfit'] mb-3">
          Configure Your <span className="gradient-text-fire">Roast</span>
        </h1>
        <p className="text-slate-400 text-sm sm:text-base max-w-xl mx-auto">
          Select the data sources to extract intelligence from and set your desired level of emotional impact.
        </p>
      </div>

      {/* Quota Banner */}
      {quota && (
        <div className="glass-panel p-4 rounded-xl border border-slate-800 flex items-center justify-between mb-8 max-w-2xl mx-auto">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-orange-500/10 text-orange-400">
              <Zap className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-semibold text-slate-300">Daily Free Allowance</div>
              <div className="text-xs text-slate-500">Resets automatically at midnight UTC</div>
            </div>
          </div>
          <div className="flex items-center gap-1.5 font-mono text-sm font-bold">
            <span className={quota.remaining > 0 ? 'text-emerald-400' : 'text-rose-400'}>
              {quota.remaining}
            </span>
            <span className="text-slate-500">/</span>
            <span className="text-slate-400">{quota.limit}</span>
            <span className="text-xs text-slate-500 font-sans ml-1">remaining</span>
          </div>
        </div>
      )}

      {/* 429 Quota Exceeded Alert */}
      {rateLimitSeconds !== null && rateLimitSeconds > 0 && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-start gap-3">
          <Clock className="w-5 h-5 flex-shrink-0 text-rose-400 mt-0.5" />
          <div>
            <div className="font-bold text-rose-200">Daily Roast Quota Exceeded (429)</div>
            <p className="text-xs text-rose-300/80 mt-1">
              You have exhausted your daily roasts. Your quota resets in{' '}
              <span className="font-mono font-bold text-white">
                {Math.floor(rateLimitSeconds / 3600)}h {Math.floor((rateLimitSeconds % 3600) / 60)}m{' '}
                {rateLimitSeconds % 60}s
              </span>
              . Identical cached roasts will still load without using quota!
            </p>
          </div>
        </div>
      )}

      {/* Error Alert */}
      {errorMessage && rateLimitSeconds === null && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0 text-rose-400" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Generator Form (visible when not viewing a completed roast) */}
      {!isReady && (
        <div className="space-y-8 glass-panel p-6 sm:p-8 rounded-2xl border border-slate-800/90 shadow-xl max-w-2xl mx-auto">
          {/* Step 1: Sources */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">
              1. Select Digital Footprints
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {/* GitHub */}
              <button
                type="button"
                onClick={() => toggleSource('github')}
                className={`flex items-center gap-3 p-3.5 rounded-xl border text-left transition-all ${
                  selectedSources.includes('github')
                    ? 'bg-slate-900 border-orange-500/60 shadow-lg shadow-orange-500/10'
                    : 'bg-slate-950/60 border-slate-800 text-slate-500 hover:border-slate-700'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                    selectedSources.includes('github')
                      ? 'bg-purple-500/20 text-purple-300'
                      : 'bg-slate-800 text-slate-500'
                  }`}
                >
                  <GithubIcon className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs font-bold text-white">GitHub</div>
                  <div className="text-[10px] text-slate-400">Repos & commits</div>
                </div>
              </button>

              {/* Spotify */}
              <button
                type="button"
                onClick={() => toggleSource('spotify')}
                className={`flex items-center gap-3 p-3.5 rounded-xl border text-left transition-all ${
                  selectedSources.includes('spotify')
                    ? 'bg-slate-900 border-emerald-500/60 shadow-lg shadow-emerald-500/10'
                    : 'bg-slate-950/60 border-slate-800 text-slate-500 hover:border-slate-700'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                    selectedSources.includes('spotify')
                      ? 'bg-emerald-500/20 text-emerald-300'
                      : 'bg-slate-800 text-slate-500'
                  }`}
                >
                  <SpotifyIcon className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs font-bold text-white">Spotify</div>
                  <div className="text-[10px] text-slate-400">Top tracks & genres</div>
                </div>
              </button>

              {/* YouTube */}
              <button
                type="button"
                onClick={() => toggleSource('youtube')}
                className={`flex items-center gap-3 p-3.5 rounded-xl border text-left transition-all ${
                  selectedSources.includes('youtube')
                    ? 'bg-slate-900 border-rose-500/60 shadow-lg shadow-rose-500/10'
                    : 'bg-slate-950/60 border-slate-800 text-slate-500 hover:border-slate-700'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                    selectedSources.includes('youtube')
                      ? 'bg-rose-500/20 text-rose-400'
                      : 'bg-slate-800 text-slate-500'
                  }`}
                >
                  <YoutubeIcon className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs font-bold text-white">YouTube</div>
                  <div className="text-[10px] text-slate-400">Subscriptions & likes</div>
                </div>
              </button>
            </div>
          </div>

          {/* Step 2: Tone Selection */}
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">
              2. Select Humiliation Heat Level
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {TONES.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => setSelectedTone(t.id)}
                  className={`p-4 rounded-xl border text-left transition-all relative overflow-hidden ${
                    selectedTone === t.id
                      ? `bg-slate-900 border-orange-500 shadow-md ${t.color}`
                      : 'bg-slate-950/50 border-slate-800/80 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-extrabold text-sm text-white flex items-center gap-2">
                      <span>{t.emoji}</span>
                      <span>{t.label}</span>
                    </span>
                    {selectedTone === t.id && (
                      <span className="w-2 h-2 rounded-full bg-orange-400 animate-pulse" />
                    )}
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed">{t.desc}</p>
                </button>
              ))}
            </div>
          </div>

          {/* Generate Button or Loading State */}
          <div className="pt-2">
            {isGenerating ? (
              <div className="text-center py-6 glass-panel rounded-xl border border-orange-500/30">
                <div className="inline-block p-4 rounded-full bg-orange-500/10 mb-4 animate-flame">
                  <Flame className="w-10 h-10 text-orange-500 fill-orange-500/30" />
                </div>
                <div className="text-base font-bold text-white mb-1">
                  {LOADING_MESSAGES[loadingMsgIdx]}
                </div>
                <div className="text-xs text-slate-400 flex items-center justify-center gap-1.5">
                  <span className="inline-block w-1.5 h-1.5 rounded-full bg-orange-400 animate-ping" />
                  <span>Polling status every 1.5s...</span>
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={handleGenerate}
                disabled={isOutOfQuota}
                className={`w-full py-4 px-6 rounded-xl font-extrabold text-base flex items-center justify-center gap-2 shadow-xl transition-all ${
                  isOutOfQuota
                    ? 'bg-slate-800 text-slate-500 cursor-not-allowed'
                    : 'bg-gradient-to-r from-orange-600 via-amber-600 to-orange-500 hover:from-orange-500 hover:to-amber-400 text-white shadow-orange-500/25 hover:scale-[1.01] active:scale-[0.99]'
                }`}
              >
                <Flame className="w-5 h-5" />
                <span>Torch My Profile 🔥</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* Completed Roast Display */}
      {isReady && roastData && (
        <div className="space-y-6 max-w-2xl mx-auto animate-in fade-in zoom-in-95 duration-300">
          <div className="glass-panel p-6 sm:p-8 rounded-2xl border border-orange-500/40 shadow-2xl relative">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-6 pb-4 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-rose-500" />
                <span className="w-3 h-3 rounded-full bg-amber-500" />
                <span className="w-3 h-3 rounded-full bg-emerald-500" />
                <span className="text-xs font-mono text-slate-400 ml-2">roast_id: {roastData.id.substring(0, 8)}...</span>
              </div>
              <span className="px-3 py-1 rounded-full bg-orange-500/20 text-orange-300 border border-orange-500/40 text-xs font-bold">
                🔥 {roastData.tone} ROAST
              </span>
            </div>

            {/* Roast Text */}
            <div className="my-6">
              <p className="text-xl sm:text-2xl font-serif italic text-slate-100 leading-relaxed whitespace-pre-wrap">
                “{roastData.roastText}”
              </p>
            </div>

            {/* Footer Stats & Sources */}
            <div className="flex flex-wrap items-center justify-between pt-4 border-t border-slate-800 text-xs text-slate-400">
              <div>Sources: {roastData.sources.replace(',', ' • ')}</div>
              <div>Prompt {roastData.promptVersion}</div>
            </div>
          </div>

          {/* Action Toolbar */}
          <div className="flex flex-wrap items-center gap-3 justify-between">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={handleShare}
                disabled={isSharing}
                className="px-5 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold flex items-center gap-2 shadow-lg shadow-orange-500/20 transition-all hover:scale-[1.02]"
              >
                {copied ? <Check className="w-4 h-4 text-emerald-300" /> : <Share2 className="w-4 h-4" />}
                <span>{copied ? 'Share Link Copied! 📋' : isSharing ? 'Creating Card...' : 'Share Roast & OG Card'}</span>
              </button>

              {shareUrl && (
                <button
                  type="button"
                  onClick={() => {
                    navigator.clipboard.writeText(shareUrl);
                    setCopied(true);
                    setTimeout(() => setCopied(false), 2000);
                  }}
                  className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs transition-colors"
                  title="Copy link"
                >
                  <Copy className="w-4 h-4" />
                </button>
              )}
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => {
                  setActiveRoastId(null);
                  setShareUrl(null);
                }}
                className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-semibold transition-all"
              >
                Roast Again
              </button>
              <button
                type="button"
                onClick={() => onNavigate('/history')}
                className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-semibold transition-all"
              >
                View History
              </button>
            </div>
          </div>

          {/* Share Preview Link Alert */}
          {shareUrl && (
            <div className="p-4 rounded-xl bg-slate-900 border border-orange-500/30 text-xs flex items-center justify-between gap-3">
              <div className="truncate">
                <span className="text-slate-400">Public Link: </span>
                <a href={shareUrl} target="_blank" rel="noreferrer" className="text-orange-400 hover:underline font-mono">
                  {shareUrl}
                </a>
              </div>
              <span className="text-emerald-400 font-semibold flex-shrink-0">Ready for WhatsApp / X!</span>
            </div>
          )}
        </div>
      )}

      {/* Generation Failure View */}
      {isFailed && (
        <div className="glass-panel p-6 rounded-2xl border border-rose-500/40 text-center max-w-lg mx-auto">
          <AlertCircle className="w-10 h-10 text-rose-500 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white mb-2">Generation Failed</h3>
          <p className="text-xs text-rose-300/80 mb-6">
            {roastData?.errorMessage || 'An error occurred during LLM roast synthesis.'}
          </p>
          <button
            type="button"
            onClick={() => setActiveRoastId(null)}
            className="px-6 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold"
          >
            Try Again
          </button>
        </div>
      )}
    </div>
  );
};
