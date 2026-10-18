import { useState, useEffect } from 'react';
import type { FC } from 'react';
import { ArrowRight, Flame, Shield, Sparkles, Zap } from 'lucide-react';
import { GithubIcon, SpotifyIcon, YoutubeIcon } from '../components/Icons';
import type { User, SystemStatus } from '../api/types';
import { api } from '../api/client';

interface LandingPageProps {
  user: User | null;
  onNavigate: (route: string) => void;
}

export const LandingPage: FC<LandingPageProps> = ({ user, onNavigate }) => {
  const [sysStatus, setSysStatus] = useState<SystemStatus | null>(null);

  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const s = await api.getSystemStatus();
        setSysStatus(s);
      } catch {
        // ignore
      }
    };
    fetchStatus();
    const interval = setInterval(fetchStatus, 6000);
    return () => clearInterval(interval);
  }, []);

  const isAuthReady = sysStatus?.services?.identity === 'UP';

  const handleOAuthLogin = (provider: string) => {
    if (sysStatus && !isAuthReady) {
      alert('The authentication microservice is currently warming up on Render (~30s). Please wait until the green "Operational" badge appears in the top bar before signing in!');
      return;
    }
    // Spotify strictly requires 127.0.0.1 loopback origin for callbacks; ensure origin matches to share session cookies
    if (provider === 'spotify' && window.location.hostname === 'localhost') {
      window.location.href = `http://127.0.0.1:5173/oauth2/authorization/spotify`;
      return;
    }
    const apiBase = (
      import.meta.env.VITE_API_BASE_URL ||
      (typeof window !== 'undefined' && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
        ? ''
        : 'https://roastapp-api.onrender.com')
    ).replace(/\/$/, '');
    window.location.href = `${apiBase}/oauth2/authorization/${provider}`;
  };

  return (
    <div className="relative overflow-hidden pt-12 pb-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      {/* Decorative Glow Orbs */}
      <div className="glow-orange -top-20 -left-20" />
      <div className="glow-purple top-40 -right-20" />

      {/* Hero Header */}
      <div className="text-center max-w-3xl mx-auto relative z-10">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-semibold mb-6 shadow-sm">
          <Flame className="w-4 h-4 fill-orange-500/20" />
          <span>AI-Powered Reality Check</span>
        </div>

        <h1 className="text-4xl sm:text-6xl lg:text-7xl font-black tracking-tight font-['Outfit'] leading-[1.08] mb-6">
          Your Digital Life Is About To Get{' '}
          <span className="gradient-text-fire">Torched.</span>
        </h1>

        <p className="text-lg sm:text-xl text-slate-400 max-w-2xl mx-auto mb-10 leading-relaxed font-normal">
          Connect your GitHub, Spotify, or YouTube. Our ruthless LLM will analyze your 3 AM commits,
          questionable playlists, and binge habits to roast you into another dimension.
        </p>

        {/* Primary Action Section */}
        {user ? (
          <div className="glass-panel p-6 rounded-2xl max-w-md mx-auto border border-orange-500/30 shadow-xl shadow-orange-500/10 mb-14">
            <p className="text-sm text-slate-300 mb-4">
              Welcome back, <span className="font-bold text-white">{user.name || user.email}</span>! Ready for your verdict?
            </p>
            <button
              onClick={() => onNavigate('/roast')}
              className="w-full py-3.5 px-6 rounded-xl bg-gradient-to-r from-orange-600 via-amber-600 to-orange-500 hover:from-orange-500 hover:to-amber-400 text-white font-extrabold flex items-center justify-center gap-2 shadow-lg shadow-orange-500/25 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              <span>Go To Roast Generator</span>
              <ArrowRight className="w-5 h-5" />
            </button>
          </div>
        ) : (
          <div className="max-w-md mx-auto mb-16">
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Connect an account to begin
              </span>
              {sysStatus && (
                <span className={`inline-flex items-center gap-1.5 text-[11px] font-medium px-2.5 py-0.5 rounded-full ${
                  isAuthReady
                    ? 'text-emerald-400 bg-emerald-500/10'
                    : 'text-amber-400 bg-amber-500/10 animate-pulse'
                }`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${isAuthReady ? 'bg-emerald-400' : 'bg-amber-400'}`} />
                  {isAuthReady ? 'Auth Ready' : 'Warming Up...'}
                </span>
              )}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {/* GitHub */}
              <button
                onClick={() => handleOAuthLogin('github')}
                className="flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-slate-900/90 hover:bg-slate-800 border border-slate-700/80 hover:border-slate-500 text-white text-xs font-bold transition-all shadow-md group hover:scale-[1.02] active:scale-[0.98]"
              >
                <GithubIcon className="w-4 h-4 group-hover:text-purple-400 transition-colors" />
                <span>GitHub</span>
              </button>

              {/* Spotify */}
              <button
                onClick={() => handleOAuthLogin('spotify')}
                className="flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-slate-900/90 hover:bg-slate-800 border border-slate-700/80 hover:border-emerald-500/60 text-white text-xs font-bold transition-all shadow-md group hover:scale-[1.02] active:scale-[0.98]"
              >
                <SpotifyIcon className="w-4 h-4 text-emerald-400 group-hover:scale-110 transition-transform" />
                <span>Spotify</span>
              </button>

              {/* YouTube */}
              <button
                onClick={() => handleOAuthLogin('google')}
                className="flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-slate-900/90 hover:bg-slate-800 border border-slate-700/80 hover:border-rose-500/60 text-white text-xs font-bold transition-all shadow-md group hover:scale-[1.02] active:scale-[0.98]"
              >
                <YoutubeIcon className="w-4 h-4 text-rose-500 group-hover:scale-110 transition-transform" />
                <span>YouTube</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Showcase Card Preview */}
      <div className="relative max-w-4xl mx-auto my-12 z-10">
        <div className="absolute -inset-1 bg-gradient-to-r from-orange-500/30 via-pink-500/20 to-purple-500/30 rounded-3xl blur-xl opacity-70"></div>
        <div className="relative glass-panel rounded-2xl p-6 sm:p-8 border border-slate-700/80 shadow-2xl">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-6 pb-4 border-b border-slate-800">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-rose-500/80" />
              <span className="w-3 h-3 rounded-full bg-amber-500/80" />
              <span className="w-3 h-3 rounded-full bg-emerald-500/80" />
              <span className="text-xs font-mono text-slate-400 ml-2">preview // sample_roast.md</span>
            </div>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/20 border border-orange-500/40 text-orange-300 text-xs font-bold">
              <Flame className="w-3.5 h-3.5" />
              <span>BRUTAL ROAST</span>
            </div>
          </div>

          <p className="text-xl sm:text-2xl font-serif italic text-slate-200 leading-relaxed mb-6">
            “You have 37 abandoned git repositories, 78% of your Spotify listening happens past 2 AM,
            and your YouTube search history is just ‘how to center a div’ followed by 4 hours of urban exploration documentaries.
            You don't need a senior developer role, you need daylight and a therapist.”
          </p>

          <div className="flex flex-wrap items-center justify-between pt-4 border-t border-slate-800 text-xs text-slate-400">
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1 text-slate-300 font-medium">
                <GithubIcon className="w-3.5 h-3.5" /> GitHub
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 text-slate-300 font-medium">
                <SpotifyIcon className="w-3.5 h-3.5 text-emerald-400" /> Spotify
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 text-slate-300 font-medium">
                <YoutubeIcon className="w-3.5 h-3.5 text-rose-500" /> YouTube
              </span>
            </div>
            <div className="font-semibold text-orange-400">Generated in 1.4s with LLaMA 3.3 70B</div>
          </div>
        </div>
      </div>

      {/* Features & Security Guarantees */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto mt-20 relative z-10">
        <div className="glass-panel p-6 rounded-2xl border border-slate-800/80">
          <div className="w-10 h-10 rounded-xl bg-orange-500/10 border border-orange-500/30 flex items-center justify-center text-orange-400 mb-4">
            <Zap className="w-5 h-5" />
          </div>
          <h3 className="text-lg font-bold text-white mb-2 font-['Outfit']">Instant 1200x630 OG Cards</h3>
          <p className="text-sm text-slate-400 leading-relaxed">
            Every roast automatically generates a high-definition social preview card rendered with Java2D and hosted on Cloudflare R2 for WhatsApp, Twitter, and LinkedIn previews.
          </p>
        </div>

        <div className="glass-panel p-6 rounded-2xl border border-slate-800/80">
          <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400 mb-4">
            <Shield className="w-5 h-5" />
          </div>
          <h3 className="text-lg font-bold text-white mb-2 font-['Outfit']">Total Privacy Guarantee</h3>
          <p className="text-sm text-slate-400 leading-relaxed">
            We never store passwords. OAuth tokens are AES-256-GCM encrypted in Redis with a 15-minute TTL. Your data is deleted the moment your roast is synthesized.
          </p>
        </div>

        <div className="glass-panel p-6 rounded-2xl border border-slate-800/80">
          <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 mb-4">
            <Sparkles className="w-5 h-5" />
          </div>
          <h3 className="text-lg font-bold text-white mb-2 font-['Outfit']">4 Precision Humiliation Tones</h3>
          <p className="text-sm text-slate-400 leading-relaxed">
            From playful friendly teasing to unhinged existential brutality. You pick the heat level you can handle.
          </p>
        </div>
      </div>
    </div>
  );
};
