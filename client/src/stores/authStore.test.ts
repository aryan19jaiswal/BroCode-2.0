import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';
import { authService } from '@/services/auth';

vi.mock('@/services/auth', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  },
}));

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const resetStore = () =>
  useAuthStore.setState({ isAuthenticated: false, isLoading: false, username: null });

beforeEach(() => {
  vi.clearAllMocks();
  resetStore();
});

describe('authStore', () => {
  describe('login', () => {
    it('sets isAuthenticated and username on success', async () => {
      vi.mocked(authService.login).mockResolvedValue({
        success: true,
        username: 'brodev',
        message: '',
      });

      const ok = await useAuthStore.getState().login({
        identifier: 'bro@example.com',
        password: 'securepass',
      });

      expect(ok).toBe(true);
      expect(useAuthStore.getState().isAuthenticated).toBe(true);
      expect(useAuthStore.getState().username).toBe('brodev');
      expect(localStorage.getItem('brocode-auth')).toBe('true');
    });

    it('does not set isAuthenticated when success is false', async () => {
      vi.mocked(authService.login).mockResolvedValue({
        success: false,
        message: 'Wrong password',
        username: undefined,
      });

      const ok = await useAuthStore.getState().login({
        identifier: 'bro@example.com',
        password: 'wrong',
      });

      expect(ok).toBe(false);
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
      expect(useAuthStore.getState().isLoading).toBe(false);
    });

    it('does not set isAuthenticated when login throws', async () => {
      vi.mocked(authService.login).mockRejectedValue(new Error('Network error'));

      const ok = await useAuthStore.getState().login({
        identifier: 'bro@example.com',
        password: 'securepass',
      });

      expect(ok).toBe(false);
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
      expect(useAuthStore.getState().isLoading).toBe(false);
    });
  });

  describe('logout', () => {
    it('clears authentication state', async () => {
      useAuthStore.setState({ isAuthenticated: true, username: 'brodev' });
      vi.mocked(authService.logout).mockResolvedValue({ success: true, message: '' });

      await useAuthStore.getState().logout();

      expect(useAuthStore.getState().isAuthenticated).toBe(false);
      expect(useAuthStore.getState().username).toBeNull();
      expect(localStorage.getItem('brocode-auth')).toBeNull();
      expect(localStorage.getItem('brocode-username')).toBeNull();
    });

    it('clears state even when logout API call throws', async () => {
      useAuthStore.setState({ isAuthenticated: true, username: 'brodev' });
      vi.mocked(authService.logout).mockRejectedValue(new Error('Network error'));

      await useAuthStore.getState().logout();

      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });
  });

  describe('clearAuth', () => {
    it('clears authentication state on auth:unauthorized event', () => {
      useAuthStore.setState({ isAuthenticated: true, username: 'brodev' });

      window.dispatchEvent(new CustomEvent('auth:unauthorized'));

      expect(useAuthStore.getState().isAuthenticated).toBe(false);
      expect(useAuthStore.getState().username).toBeNull();
    });
  });

  describe('fetchProfile', () => {
    it('updates username from server response', async () => {
      useAuthStore.setState({ username: 'old-name' });
      vi.mocked(authService.getProfile).mockResolvedValue({
        success: true,
        username: 'updated-name',
      });

      await useAuthStore.getState().fetchProfile();

      expect(useAuthStore.getState().username).toBe('updated-name');
      expect(localStorage.getItem('brocode-username')).toBe('updated-name');
    });

    it('keeps existing username when getProfile throws', async () => {
      useAuthStore.setState({ username: 'existing-name' });
      vi.mocked(authService.getProfile).mockRejectedValue(new Error('Network error'));

      await useAuthStore.getState().fetchProfile();

      expect(useAuthStore.getState().username).toBe('existing-name');
    });
  });
});
