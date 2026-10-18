import type { FC } from 'react';
import { ArrowLeft, FileText, AlertCircle, Sparkles } from 'lucide-react';

interface TermsPageProps {
  onNavigate: (route: string) => void;
}

export const TermsPage: FC<TermsPageProps> = ({ onNavigate }) => {
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
          <FileText className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-3xl font-black font-['Outfit'] text-white">Terms of Service</h1>
          <p className="text-sm text-slate-400">Last updated: October 2026</p>
        </div>
      </div>

      <div className="space-y-8 text-slate-300 leading-relaxed text-sm sm:text-base">
        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-orange-400" />
            1. Nature of the Service
          </h2>
          <p>
            RoastApp is an entertainment application designed to generate humorous, satirical, and sarcastic AI commentary based on your public digital footprint. All roasts are generated automatically by large language models and are intended solely for comedic and self-deprecating entertainment.
          </p>
        </section>

        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-orange-400" />
            2. Satirical Disclaimer
          </h2>
          <p>
            The generated commentary does not represent literal facts, assessments of character, or malicious intent. Users acknowledge that roasts may be provocative or sharp, but agree not to use the generated output to harass, defame, or abuse others.
          </p>
        </section>

        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white">3. Fair Use & Rate Limiting</h2>
          <p>
            To ensure availability and prevent abuse of third-party APIs, user accounts are subject to quota limits (default: 3 roasts per 24 hours). Automated scraping, botting, or attempting to bypass rate limits is prohibited.
          </p>
        </section>

        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white">4. User Content & Sharing</h2>
          <p>
            When you choose to create a public share link or roast card, you grant RoastApp permission to render and publicly host the synthesized roast card image and text for sharing purposes.
          </p>
        </section>

        <section className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-3">
          <h2 className="text-lg font-bold text-white">5. Termination & Contact</h2>
          <p>
            We reserve the right to suspend or restrict access to any user found violating these terms. If you have questions regarding these terms, contact us at <span className="font-mono text-orange-400">sujalkamble007@gmail.com</span>.
          </p>
        </section>
      </div>
    </div>
  );
};
