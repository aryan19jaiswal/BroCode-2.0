import { create } from 'zustand';
import { authService } from '@/services/auth';
import type { AuthRequest, RegisterRequest } from '@/types';
import toast from 'react-hot-toast';

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  username: string | null;
  login: (data: AuthRequest) => Promise<boolean>;
  register: (data: RegisterRequest) => Promise<boolean>;
  logout: () => Promise<void>;
  clearAuth: () => void;
  setUsername: (name: string) => void;
  fetchProfile: () => Promise<void>;
}

function getStoredAuth(): boolean {
  return localStorage.getItem('brocode-auth') === 'true';
}

function getStoredUsername(): string | null {
  return localStorage.getItem('brocode-username');
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: getStoredAuth(),
  isLoading: false,
  username: getStoredUsername(),

  login: async (data) => {
    set({ isLoading: true });
    try {
      const res = await authService.login(data);
      if (res.success) {
        localStorage.setItem('brocode-auth', 'true');
        const name = res.username ?? null;
        if (name) localStorage.setItem('brocode-username', name);
        set({ isAuthenticated: true, isLoading: false, username: name });
        toast.success('Welcome back, bro!');
        return true;
      }
      toast.error(res.message);
      set({ isLoading: false });
      return false;
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? 'Login failed';
      toast.error(message);
      set({ isLoading: false });
      return false;
    }
  },

  register: async (data) => {
    set({ isLoading: true });
    try {
      const res = await authService.register(data);
      if (res.success) {
        set({ isLoading: false });
        toast.success('Account created! Now log in.');
        return true;
      }
      toast.error(res.message);
      set({ isLoading: false });
      return false;
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? 'Registration failed';
      toast.error(message);
      set({ isLoading: false });
      return false;
    }
  },

  logout: async () => {
    try {
      await authService.logout();
    } catch {
      // Best-effort
    }
    localStorage.removeItem('brocode-auth');
    localStorage.removeItem('brocode-username');
    set({ isAuthenticated: false, username: null });
    toast.success('Logged out. See ya, bro!');
  },

  clearAuth: () => {
    localStorage.removeItem('brocode-auth');
    localStorage.removeItem('brocode-username');
    set({ isAuthenticated: false, username: null });
  },

  setUsername: (name) => {
    localStorage.setItem('brocode-username', name);
    set({ username: name });
  },

  fetchProfile: async () => {
    try {
      const res = await authService.getProfile();
      if (res.success && res.username) {
        localStorage.setItem('brocode-username', res.username);
        set({ username: res.username });
      }
    } catch {
      // Best-effort — username will remain from localStorage cache
    }
  },
}));

// Listen for 401 events dispatched by API interceptor
if (typeof window !== 'undefined') {
  window.addEventListener('auth:unauthorized', () => {
    useAuthStore.getState().clearAuth();
  });
}
