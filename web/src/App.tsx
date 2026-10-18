import { useEffect, useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { api } from './api/client';
import type { QuotaInfo, User } from './api/types';
import { Navbar } from './components/Navbar';
import { LandingPage } from './pages/LandingPage';
import { RoastPage } from './pages/RoastPage';
import { PublicSharePage } from './pages/PublicSharePage';
import { HistoryPage } from './pages/HistoryPage';
import { PrivacyPage } from './pages/PrivacyPage';
import { TermsPage } from './pages/TermsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 5000,
    },
  },
});

export function AppContent() {
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  const [user, setUser] = useState<User | null>(null);
  const [quota, setQuota] = useState<QuotaInfo | undefined>(undefined);
  const [loadingAuth, setLoadingAuth] = useState(true);

  // Sync with browser back/forward buttons
  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname);
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigate = (path: string) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const loadUserData = async () => {
    try {
      const u = await api.getCurrentUser();
      setUser(u);
      if (u) {
        try {
          const q = await api.getQuota();
          setQuota(q);
        } catch {
          // Quota load failed
        }
      }
    } catch {
      setUser(null);
    } finally {
      setLoadingAuth(false);
    }
  };

  useEffect(() => {
    loadUserData();
  }, []);

  const handleLogout = async () => {
    try {
      await api.logout();
      setUser(null);
      setQuota(undefined);
      navigate('/');
    } catch (err: any) {
      alert('Logout failed: ' + err.message);
    }
  };

  // Route matching
  const renderCurrentPage = () => {
    // 1. Legal / Compliance Routes
    if (currentPath === '/privacy') {
      return <PrivacyPage onNavigate={navigate} />;
    }
    if (currentPath === '/terms') {
      return <TermsPage onNavigate={navigate} />;
    }

    // 2. Match /r/:id (Public Roast Share View)
    const publicShareMatch = currentPath.match(/^\/r\/([a-zA-Z0-9_-]+)/);
    if (publicShareMatch) {
      const shareId = publicShareMatch[1];
      return <PublicSharePage shareId={shareId} onNavigate={navigate} />;
    }

    // 3. Authenticated Routes
    if (currentPath === '/roast') {
      if (!user && !loadingAuth) {
        return <LandingPage user={null} onNavigate={navigate} />;
      }
      return (
        <RoastPage
          user={user!}
          quota={quota}
          onRefreshQuota={loadUserData}
          onNavigate={navigate}
        />
      );
    }

    if (currentPath === '/history') {
      if (!user && !loadingAuth) {
        return <LandingPage user={null} onNavigate={navigate} />;
      }
      return <HistoryPage onNavigate={navigate} />;
    }

    // Default: Landing Page (/)
    return <LandingPage user={user} onNavigate={navigate} />;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-['Inter']">
      <Navbar
        user={user}
        quota={quota}
        currentRoute={currentPath}
        onNavigate={navigate}
        onLogout={handleLogout}
      />

      <main className="flex-1">
        {renderCurrentPage()}
      </main>

      {/* Footer */}
      <footer className="glass-panel border-t border-slate-900 py-6 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-1.5 font-bold text-slate-400">
            <span>🔥 RoastApp</span>
            <span>•</span>
            <span className="font-normal">Zero Permanent Token Retention</span>
          </div>
          
          <div className="flex items-center gap-4 text-slate-400">
            <button
              onClick={() => navigate('/privacy')}
              className="hover:text-orange-400 transition cursor-pointer"
            >
              Privacy Policy
            </button>
            <span>•</span>
            <button
              onClick={() => navigate('/terms')}
              className="hover:text-orange-400 transition cursor-pointer"
            >
              Terms of Service
            </button>
          </div>

          <div className="text-slate-600 text-[11px]">
            Built with Spring Boot 3.4, Neon Postgres, Cloudflare R2 & Groq
          </div>
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
    </QueryClientProvider>
  );
}
