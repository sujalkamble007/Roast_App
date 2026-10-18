import type { CreateRoastRequest, QuotaInfo, Roast, ShareDetails, ShareResponse, User } from './types';

export class ApiError extends Error {
  status: number;
  retryAfterSeconds?: number;
  problemDetail?: any;

  constructor(message: string, status: number, retryAfterSeconds?: number, problemDetail?: any) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
    this.problemDetail = problemDetail;
  }
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json');
  }

  const url = path.startsWith('http') ? path : `${API_BASE_URL}${path}`;

  const response = await fetch(url, {
    ...options,
    headers,
    credentials: 'include', // Mandated by Step 8
  });

  if (response.status === 204) {
    return {} as T;
  }

  if (!response.ok) {
    let errorDetail: any = null;
    let message = `Request failed with status ${response.status}`;
    let retryAfterSeconds: number | undefined;

    const retryHeader = response.headers.get('Retry-After');
    if (retryHeader) {
      const parsed = parseInt(retryHeader, 10);
      if (!isNaN(parsed)) retryAfterSeconds = parsed;
    }

    try {
      errorDetail = await response.json();
      if (errorDetail?.detail) {
        message = errorDetail.detail;
      }
      if (errorDetail?.retryAfterSeconds) {
        retryAfterSeconds = errorDetail.retryAfterSeconds;
      }
    } catch {
      // not JSON
    }

    throw new ApiError(message, response.status, retryAfterSeconds, errorDetail);
  }

  return response.json();
}

export const api = {
  // Identity
  getCurrentUser: async (): Promise<User | null> => {
    try {
      return await request<User>('/api/v1/me');
    } catch (err: any) {
      if (err.status === 401 || err.status === 403) return null;
      throw err;
    }
  },

  logout: async (): Promise<void> => {
    await request<void>('/api/v1/auth/logout', { method: 'POST' });
  },

  deleteAccount: async (): Promise<void> => {
    await request<void>('/api/v1/me', { method: 'DELETE' });
  },

  // Roasts
  getQuota: (): Promise<QuotaInfo> => request<QuotaInfo>('/api/v1/roasts/quota'),

  createRoast: (payload: CreateRoastRequest, idempotencyKey?: string): Promise<Roast> => {
    const headers: Record<string, string> = {};
    if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;

    return request<Roast>('/api/v1/roasts', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
  },

  getRoast: (id: string): Promise<Roast> => request<Roast>(`/api/v1/roasts/${id}`),

  listRoasts: (): Promise<Roast[]> => request<Roast[]>('/api/v1/roasts'),

  deleteRoast: (id: string): Promise<void> => request<void>(`/api/v1/roasts/${id}`, { method: 'DELETE' }),

  shareRoast: (id: string): Promise<ShareResponse> =>
    request<ShareResponse>(`/api/v1/roasts/${id}/share`, { method: 'POST' }),

  // Public Shares
  getPublicShare: (id: string): Promise<ShareDetails> => request<ShareDetails>(`/api/v1/public/shares/${id}`),

  revokeShare: (id: string): Promise<void> => request<void>(`/api/v1/public/shares/${id}`, { method: 'DELETE' }),

  // System Status
  getSystemStatus: async () => {
    try {
      return await request<any>('/api/v1/system/status');
    } catch {
      return {
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
    }
  },
};
