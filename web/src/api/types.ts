export type Tone = 'BRUTAL' | 'SARCASTIC' | 'WHOLESOME' | 'FRIENDLY';
export type Source = 'spotify' | 'github' | 'youtube';
export type RoastStatus = 'PENDING' | 'READY' | 'FAILED';

export interface User {
  id: string;
  email?: string;
  name?: string;
  avatarUrl?: string;
  linkedAccounts: Source[];
}

export interface QuotaInfo {
  limit: number;
  used: number;
  remaining: number;
  resetsAt: string;
}

export interface Roast {
  id: string;
  userId: string;
  status: RoastStatus;
  tone: Tone;
  sources: string;
  statsJson?: string;
  roastText?: string;
  errorMessage?: string;
  promptVersion: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRoastRequest {
  sources: Source[];
  tone: Tone;
}

export interface ShareResponse {
  id: string;
  shareUrl: string;
  imageUrl: string;
}

export interface ShareDetails {
  id: string;
  roastId: string;
  imageUrl: string;
  roastText: string;
  tone: string;
  sources: string;
  createdAt: string;
}

export interface SystemStatus {
  status: 'UP' | 'STARTING' | 'OFFLINE';
  upCount: number;
  totalCount: number;
  services: {
    gateway: string;
    identity: string;
    connector: string;
    roast: string;
    share: string;
  };
}
