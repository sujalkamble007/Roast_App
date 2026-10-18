import type { FC } from 'react';
import { ArrowLeft, Shield, Lock, EyeOff, Trash2, Mail } from 'lucide-react';

interface PrivacyPageProps {
  onNavigate: (route: string) => void;
}

export const PrivacyPage: FC<PrivacyPageProps> = ({ onNavigate }) => {
  return (
    <div className="relative overflow-hidden py-12 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto">
      {/* Back button */}
      <button
        onClick={() => onNavigate('/')}
        className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-orange-400 transition mb-8 cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to Home
      </button>

      <div className="flex items-center gap-3 mb-6">
        <div className="p-3 rounded-2xl bg-orange-500/10 border border-orange-500/20 text-orange-400">
          <Shield className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-3xl font-black font-['Outfit'] text-white">Privacy Policy</h1>
          <p className="text-sm text-slate-400">Last updated: October 2026</p>
        </div>
      </div>

      <div className="space-y-8 text-slate-300 leading-relaxed text-sm sm:text-base">
        {/* Overview */}
        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Lock className="w-5 h-5 text-orange-400" />
            1. Overview & Commitment to Zero Retention
          </h2>
          <p>
            Welcome to RoastApp. We respect your privacy. Our core architectural design enforces a 
            <strong className="text-orange-300"> Zero Permanent Token Retention</strong> policy. We only access your connected platforms (GitHub, Spotify, YouTube/Google) in real-time to generate your personalized AI roast, after which authentication tokens are discarded.
          </p>
        </section>

        {/* Data We Access */}
        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <EyeOff className="w-5 h-5 text-orange-400" />
            2. Information We Access
          </h2>
          <p>When you sign in using an OAuth provider, we temporarily access:</p>
          <ul className="list-disc list-inside space-y-2 pl-2 text-slate-300">
            <li>
              <strong className="text-white">GitHub:</strong> Public profile information, top repositories, commit frequencies, and programming languages used.
            </li>
            <li>
              <strong className="text-white">Spotify:</strong> Top artists, top tracks, recently played songs, and public playlists.
            </li>
            <li>
              <strong className="text-white">Google & YouTube:</strong> Read-only access to YouTube subscriptions, public playlists, and basic profile info (<code className="text-xs bg-slate-800 px-1.5 py-0.5 rounded text-orange-300">youtube.readonly</code>).
            </li>
          </ul>
        </section>

        {/* How We Use Your Data */}
        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white">3. How Your Data Is Used</h2>
          <p>The retrieved metadata is used strictly for the following purposes:</p>
          <ul className="list-disc list-inside space-y-1.5 pl-2">
            <li>Synthesizing creative, humorous, and satirical AI roasts based on your activity.</li>
            <li>Generating shareable roast cards if you explicitly choose to publish them.</li>
            <li>Enforcing fair-use daily rate limits (e.g. 3 roasts per user per day).</li>
          </ul>
          <p className="text-slate-400 text-xs italic mt-2">
            We do NOT train artificial intelligence models on your private data, nor do we sell, rent, or trade your personal information with any third party.
          </p>
        </section>

        {/* Storage & Security */}
        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white">4. Data Storage and Security</h2>
          <p>
            - Access tokens are AES-256 encrypted in-memory and held in temporary Redis caches with a strict 15-minute Time-To-Live (TTL).
          </p>
          <p>
            - No platform passwords or raw credentials ever touch our databases.
          </p>
          <p>
            - Publicly shared roast cards (if created) are stored securely on Cloudflare R2 object storage.
          </p>
        </section>

        {/* Revocation & Deletion */}
        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Trash2 className="w-5 h-5 text-orange-400" />
            5. Revoking Access & Data Deletion
          </h2>
          <p>
            You can revoke RoastApp's access at any time from your account settings on the respective platforms:
          </p>
          <ul className="list-disc list-inside space-y-1 pl-2 text-sm text-slate-300">
            <li><a href="https://myaccount.google.com/permissions" target="_blank" rel="noreferrer" className="text-orange-400 underline">Google Account Permissions</a></li>
            <li><a href="https://github.com/settings/applications" target="_blank" rel="noreferrer" className="text-orange-400 underline">GitHub Authorized OAuth Apps</a></li>
            <li><a href="https://www.spotify.com/account/apps/" target="_blank" rel="noreferrer" className="text-orange-400 underline">Spotify Account Apps</a></li>
          </ul>
        </section>

        {/* Contact */}
        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Mail className="w-5 h-5 text-orange-400" />
            6. Contact Us
          </h2>
          <p>
            If you have questions about this Privacy Policy or wish to request deletion of your account history, contact us at:
          </p>
          <p className="font-mono text-orange-400">sujalkamble007@gmail.com</p>
        </section>
      </div>
    </div>
  );
};
