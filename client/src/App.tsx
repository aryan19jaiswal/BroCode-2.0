import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { Navbar } from '@/components/layout/Navbar';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorBoundary } from '@/components/ui/ErrorBoundary';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';

/* ── Lazy-loaded pages (code-split per route) ── */
const LandingPage = lazy(() => import('@/pages/LandingPage'));
const LoginPage = lazy(() => import('@/pages/LoginPage'));
const RegisterPage = lazy(() => import('@/pages/RegisterPage'));
const ChatPage = lazy(() => import('@/pages/ChatPage'));
const ProfilePage = lazy(() => import('@/pages/ProfilePage'));

function CatchAll() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  return <Navigate to={isAuthenticated ? '/chat' : '/'} replace />;
}

export default function App() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const fetchProfile = useAuthStore((s) => s.fetchProfile);
  const loadSessions = useChatStore((s) => s.loadSessions);

  // On every auth state change (login, page reload with stored auth), verify the
  // JWT is still valid server-side and hydrate the session list from MongoDB.
  useEffect(() => {
    if (isAuthenticated) {
      fetchProfile();
      loadSessions();
    }
  }, [isAuthenticated, fetchProfile, loadSessions]);

  return (
    <ErrorBoundary>
    <BrowserRouter>
      <div className="min-h-screen bg-surface">
        <Navbar />
        <Suspense fallback={<Spinner />}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route
              path="/chat"
              element={
                <ProtectedRoute>
                  <ChatPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<CatchAll />} />
          </Routes>
        </Suspense>
      </div>

      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            borderRadius: '12px',
            padding: '12px 16px',
            fontSize: '14px',
            fontWeight: 500,
            background: '#ffffff',
            color: '#0f172a',
            border: '1px solid #e2e8f0',
          },
        }}
      />
    </BrowserRouter>
    </ErrorBoundary>
  );
}
