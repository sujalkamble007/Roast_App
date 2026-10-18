import { useState, useEffect } from 'react';
import type { FC } from 'react';
import { ChevronDown, CheckCircle2, Clock, AlertCircle, Server } from 'lucide-react';
import { api } from '../api/client';
import type { SystemStatus } from '../api/types';

interface SystemStatusWidgetProps {
  onStatusChange?: (status: SystemStatus) => void;
}

export const SystemStatusWidget: FC<SystemStatusWidgetProps> = ({ onStatusChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [status, setStatus] = useState<SystemStatus>({
    status: 'STARTING',
    upCount: 1,
    totalCount: 5,
    services: {
      gateway: 'UP',
      identity: 'STARTING',
      connector: 'STARTING',
      roast: 'STARTING',
      share: 'STARTING',
    },
  });

  const checkStatus = async () => {
    try {
      const s = await api.getSystemStatus();
      setStatus(s);
      if (onStatusChange) onStatusChange(s);
    } catch {
      const offlineStatus: SystemStatus = {
        status: 'OFFLINE',
        upCount: 0,
        totalCount: 5,
        services: {
          gateway: 'DOWN',
          identity: 'DOWN',
          connector: 'DOWN',
          roast: 'DOWN',
          share: 'DOWN',
        },
      };
      setStatus(offlineStatus);
      if (onStatusChange) onStatusChange(offlineStatus);
    }
  };

  useEffect(() => {
    checkStatus();
    // Poll every 5s while starting/offline, or every 15s when all are UP
    const intervalTime = status.status === 'UP' ? 15000 : 5000;
    const interval = setInterval(checkStatus, intervalTime);
    return () => clearInterval(interval);
  }, [status.status]);

  const serviceLabels: Record<string, { name: string; port: string }> = {
    gateway: { name: 'API Gateway Router', port: ':10000' },
    identity: { name: 'OAuth & Identity Service', port: ':8081' },
    connector: { name: 'Third-Party Connectors', port: ':8082' },
    roast: { name: 'AI Roast Synthesizer', port: ':8083' },
    share: { name: 'OG Image Share Service', port: ':8084' },
  };

  return (
    <div className="relative inline-block text-left">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border transition-all cursor-pointer backdrop-blur-md ${
          status.status === 'UP'
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20'
            : status.status === 'STARTING'
            ? 'bg-amber-500/10 border-amber-500/30 text-amber-400 hover:bg-amber-500/20'
            : 'bg-rose-500/10 border-rose-500/30 text-rose-400 hover:bg-rose-500/20'
        }`}
      >
        <span className="relative flex h-2 w-2">
          <span
            className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
              status.status === 'UP'
                ? 'bg-emerald-400'
                : status.status === 'STARTING'
                ? 'bg-amber-400'
                : 'bg-rose-400'
            }`}
          />
          <span
            className={`relative inline-flex rounded-full h-2 w-2 ${
              status.status === 'UP'
                ? 'bg-emerald-500'
                : status.status === 'STARTING'
                ? 'bg-amber-500'
                : 'bg-rose-500'
            }`}
          />
        </span>

        <span className="font-semibold">
          {status.status === 'UP'
            ? 'Systems Operational'
            : status.status === 'STARTING'
            ? `Warming Up (${status.upCount}/5)`
            : 'Server Waking Up...'}
        </span>

        <ChevronDown className={`w-3.5 h-3.5 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {/* Popover Dropdown */}
      {isOpen && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />
          <div className="absolute right-0 mt-2 w-80 rounded-2xl glass-panel border border-slate-800 bg-slate-950/95 p-4 shadow-2xl z-50 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3 mb-3">
              <div className="flex items-center gap-2">
                <Server className="w-4 h-4 text-orange-400" />
                <span className="text-xs font-bold text-white uppercase tracking-wider">
                  Microservices Status
                </span>
              </div>
              <span className="text-[11px] font-mono text-slate-400">
                {status.upCount} of 5 Online
              </span>
            </div>

            <div className="space-y-2.5">
              {Object.entries(status.services).map(([key, svcStatus]) => {
                const info = serviceLabels[key] || { name: key, port: '' };
                const isUp = svcStatus === 'UP';
                return (
                  <div
                    key={key}
                    className="flex items-center justify-between p-2 rounded-xl bg-slate-900/60 border border-slate-800/60 text-xs"
                  >
                    <div>
                      <div className="font-medium text-slate-200">{info.name}</div>
                      <div className="text-[10px] font-mono text-slate-500">{info.port}</div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      {isUp ? (
                        <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-400">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                          UP
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-400 animate-pulse">
                          <Clock className="w-3.5 h-3.5 text-amber-400" />
                          Booting
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {status.status !== 'UP' && (
              <div className="mt-3 p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-[11px] text-amber-300 leading-tight flex items-start gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-amber-400 mt-0.5" />
                <span>
                  Render free tier spins down on inactivity. Booting all 5 microservices takes ~60–90s.
                </span>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
