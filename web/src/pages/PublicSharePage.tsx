import type { FC } from 'react';
import { useEffect, useState } from 'react';
import { AlertCircle, ArrowRight, Flame, Sparkles } from 'lucide-react';
import { api } from '../api/client';
import type { ShareDetails } from '../api/types';

interface PublicSharePageProps {
  shareId: string;
  onNavigate: (route: string) => void;
}

export const PublicSharePage: FC<PublicSharePageProps> = ({ shareId, onNavigate }) => {
  const [share, setShare] = useState<ShareDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    api
      .getPublicShare(shareId)
      .then((data) => {
        if (active) setShare(data);
      })
      .catch((err) => {
        if (active) {
          if (err.status === 404) {
            setError('This roast link has been revoked or does not exist.');
          } else {
            setError(err.message || 'Failed to load roast.');
          }
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [shareId]);

  if (loading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4">
        <div className="w-12 h-12 rounded-full border-2 border-orange-500 border-t-transparent animate-spin mb-4" />
        <p className="text-sm text-slate-400">Loading certified roast...</p>
      </div>
    );
  }

  if (error || !share) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4 max-w-md mx-auto">
        <div className="p-4 rounded-full bg-rose-500/10 text-rose-400 mb-4">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-white mb-2 font-['Outfit']">Roast Unavailable</h2>
        <p className="text-sm text-slate-400 mb-6">{error}</p>
        <button
          onClick={() => onNavigate('/')}
          className="px-6 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold transition-all"
        >
          Create Your Own Roast
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-12 relative">
      <div className="glow-orange -top-20 -left-20 opacity-40" />

      {/* Top Banner */}
      <div className="text-center mb-8">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-semibold mb-3">
          <Flame className="w-3.5 h-3.5 fill-orange-500/20" />
          <span>Shared Roast Card</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold font-['Outfit']">
          Someone Got <span className="gradient-text-fire">Torched.</span>
        </h1>
      </div>

      {/* Card Preview Container */}
      <div className="relative group max-w-3xl mx-auto rounded-2xl overflow-hidden shadow-2xl border border-slate-700/80 mb-8">
        <img
          src={`/api/v1/public/shares/${share.id}/card.png`}
          alt="Roast Card Preview"
          className="w-full h-auto object-cover rounded-2xl"
        />
      </div>

      {/* Transcript Text for Accessibility & Easy Copy */}
      <div className="glass-panel p-6 sm:p-8 rounded-2xl border border-slate-800 max-w-3xl mx-auto mb-10">
        <div className="flex items-center justify-between text-xs text-slate-400 mb-4 pb-3 border-b border-slate-800">
          <span className="font-bold text-orange-400">🔥 {share.tone?.toUpperCase()} VERDICT</span>
          <span>Analyzed: {share.sources?.replace(',', ' • ')}</span>
        </div>
        <p className="text-lg sm:text-xl font-serif italic text-slate-200 leading-relaxed">
          “{share.roastText}”
        </p>
      </div>

      {/* CTA Box */}
      <div className="text-center glass-panel p-8 rounded-2xl border border-orange-500/30 max-w-xl mx-auto shadow-xl">
        <h3 className="text-xl font-bold text-white mb-2 font-['Outfit']">Think you can handle the heat?</h3>
        <p className="text-sm text-slate-400 mb-6">
          Connect your GitHub, Spotify, or YouTube to generate your personalized roast card in seconds.
        </p>
        <button
          onClick={() => onNavigate('/')}
          className="py-3 px-8 rounded-xl bg-gradient-to-r from-orange-600 via-amber-600 to-orange-500 hover:from-orange-500 hover:to-amber-400 text-white font-extrabold text-sm flex items-center justify-center gap-2 mx-auto shadow-lg shadow-orange-500/25 transition-all hover:scale-[1.02]"
        >
          <Sparkles className="w-4 h-4" />
          <span>Get Roasted Now</span>
          <ArrowRight className="w-4 h-4 ml-1" />
        </button>
      </div>
    </div>
  );
};
