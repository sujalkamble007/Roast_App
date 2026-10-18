import type { FC } from 'react';
import { Flame, History, LogOut, Sparkles, User as UserIcon } from 'lucide-react';
import type { QuotaInfo, User } from '../api/types';
import { SystemStatusWidget } from './SystemStatusWidget';

interface NavbarProps {
  user: User | null;
  quota?: QuotaInfo;
  currentRoute: string;
  onNavigate: (route: string) => void;
  onLogout: () => void;
}

export const Navbar: FC<NavbarProps> = ({
  user,
  quota,
  currentRoute,
  onNavigate,
  onLogout,
}) => {
  return (
    <header className="sticky top-0 z-50 glass-panel border-b border-slate-800/80 px-4 lg:px-8 py-3 transition-all">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        {/* Brand */}
        <button
          onClick={() => onNavigate('/')}
          className="flex items-center gap-2.5 text-left group focus:outline-none"
        >
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-orange-600 to-amber-500 flex items-center justify-center shadow-lg shadow-orange-500/25 group-hover:scale-105 transition-transform">
            <Flame className="w-6 h-6 text-white fill-white/20" />
          </div>
          <div>
            <span className="font-extrabold text-xl tracking-tight text-white flex items-center gap-1.5 font-['Outfit']">
              Roast<span className="text-orange-500">App</span>
            </span>
            <span className="hidden sm:block text-[10px] text-slate-400 font-medium tracking-wider uppercase">
              Digital Life Humiliation
            </span>
          </div>
        </button>

        {/* Center Nav for logged in users */}
        {user && (
          <nav className="flex items-center gap-1 bg-slate-900/80 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => onNavigate('/roast')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                currentRoute === '/roast'
                  ? 'bg-gradient-to-r from-orange-600 to-amber-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>Roast Me</span>
            </button>
            <button
              onClick={() => onNavigate('/history')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                currentRoute === '/history'
                  ? 'bg-gradient-to-r from-orange-600 to-amber-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <History className="w-3.5 h-3.5" />
              <span>History</span>
            </button>
          </nav>
        )}

        {/* Right action / Profile & Status */}
        <div className="flex items-center gap-3">
          <SystemStatusWidget />

          {user ? (
            <>
              {quota && (
                <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800/80 text-xs font-medium">
                  <span className="text-slate-400">Daily Quota:</span>
                  <span
                    className={`font-bold px-1.5 py-0.5 rounded ${
                      quota.remaining > 0
                        ? 'bg-emerald-500/10 text-emerald-400'
                        : 'bg-rose-500/10 text-rose-400'
                    }`}
                  >
                    {quota.remaining}/{quota.limit}
                  </span>
                </div>
              )}

              <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
                {user.avatarUrl ? (
                  <img
                    src={user.avatarUrl}
                    alt={user.name || 'User'}
                    className="w-8 h-8 rounded-full border border-orange-500/40 object-cover"
                  />
                ) : (
                  <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-slate-300">
                    <UserIcon className="w-4 h-4" />
                  </div>
                )}
                <span className="hidden md:block text-xs font-medium text-slate-300 max-w-[120px] truncate">
                  {user.name || user.email || 'Roasted Soul'}
                </span>
                <button
                  onClick={onLogout}
                  title="Logout"
                  className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors ml-1"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            </>
          ) : (
            <button
              onClick={() => onNavigate('/')}
              className="px-4 py-2 text-xs font-bold text-white bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-500 hover:to-amber-500 rounded-xl shadow-lg shadow-orange-500/20 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              Get Roasted
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
