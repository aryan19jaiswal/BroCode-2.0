import '@testing-library/jest-dom';
import { vi, beforeEach } from 'vitest';

// jsdom 25+ disables localStorage by default (requires a file path for persistence).
// Provide a full in-memory implementation so store modules that read localStorage
// during initialisation (e.g. authStore) work without a real browser.
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string): string | null => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = String(value); },
    removeItem: (key: string) => { delete store[key]; },
    clear: () => { store = {}; },
    get length(): number { return Object.keys(store).length; },
    key: (index: number): string | null => Object.keys(store)[index] ?? null,
  };
})();

vi.stubGlobal('localStorage', localStorageMock);

beforeEach(() => {
  localStorageMock.clear();
});
