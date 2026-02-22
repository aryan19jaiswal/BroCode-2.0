import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Code2, User, LogOut, Menu, X, Info } from 'lucide-react';
import { Tooltip } from '@/components/ui/Tooltip';
import { AboutModal } from '@/components/layout/AboutModal';
import { useAuthStore } from '@/stores/authStore';
import { useState, useCallback } from 'react';

export function Navbar() {
  const { isAuthenticated, logout } = useAuthStore();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [aboutOpen, setAboutOpen] = useState(false);

  const handleLogout = useCallback(async () => {
    await logout();
    navigate('/login');
  }, [logout, navigate]);

  const isActive = (path: string) => location.pathname === path;

  return (
    <>
      <nav className="sticky top-0 z-40 glass border-b border-border shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <Link to={isAuthenticated ? '/chat' : '/'} className="flex items-center gap-2.5 group">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center shadow-lg shadow-brand-500/20 group-hover:shadow-brand-500/40 transition-shadow duration-300">
                <Code2 className="w-5 h-5 text-white" />
              </div>
              <span className="text-xl font-bold tracking-tight text-gradient">BroCode</span>
            </Link>

            {/* Desktop nav */}
            <div className="hidden md:flex items-center gap-1">
              {isAuthenticated ? (
                <>
                  <Link
                    to="/chat"
                    className={`btn-ghost text-sm font-medium ${
                      isActive('/chat') ? 'text-brand-600 bg-brand-500/10' : ''
                    }`}
                  >
                    Chat
                  </Link>
                  <Tooltip label="About">
                    <button
                      onClick={() => setAboutOpen(true)}
                      className="btn-ghost p-2.5"
                      aria-label="About BroCode"
                    >
                      <Info className="w-4 h-4" />
                    </button>
                  </Tooltip>
                  <Tooltip label="Profile">
                    <Link
                      to="/profile"
                      className={`btn-ghost p-2.5 ${
                        isActive('/profile') ? 'text-brand-600 bg-brand-500/10' : ''
                      }`}
                      aria-label="Profile"
                    >
                      <User className="w-4 h-4" />
                    </Link>
                  </Tooltip>
                  <Tooltip label="Logout">
                    <button
                      onClick={handleLogout}
                      className="btn-ghost p-2.5 text-red-500 hover:text-red-600 hover:bg-red-50"
                      aria-label="Logout"
                    >
                      <LogOut className="w-4 h-4" />
                    </button>
                  </Tooltip>
                </>
              ) : (
                <>
                  <Link to="/login" className="btn-ghost text-sm font-medium">
                    Sign In
                  </Link>
                  <Link to="/register" className="btn-primary text-sm">
                    Get Started
                  </Link>
                </>
              )}
            </div>

            {/* Mobile toggle */}
            <div className="md:hidden">
              <button
                onClick={() => setMobileOpen(!mobileOpen)}
                className="btn-ghost p-2"
                aria-label="Toggle menu"
              >
                {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {/* Mobile menu */}
          {mobileOpen && (
            <div className="md:hidden pb-4 space-y-1">
              {isAuthenticated ? (
                <>
                  <Link
                    to="/chat"
                    onClick={() => setMobileOpen(false)}
                    className="block btn-ghost text-sm font-medium w-full text-left"
                  >
                    Chat
                  </Link>
                  <button
                    onClick={() => {
                      setMobileOpen(false);
                      setAboutOpen(true);
                    }}
                    className="block btn-ghost text-sm font-medium w-full text-left"
                  >
                    About
                  </button>
                  <Link
                    to="/profile"
                    onClick={() => setMobileOpen(false)}
                    className="block btn-ghost text-sm font-medium w-full text-left"
                  >
                    Profile
                  </Link>
                  <button
                    onClick={() => {
                      setMobileOpen(false);
                      handleLogout();
                    }}
                    className="block btn-ghost text-sm font-medium w-full text-left text-red-500"
                  >
                    Logout
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    onClick={() => setMobileOpen(false)}
                    className="block btn-ghost text-sm font-medium w-full text-left"
                  >
                    Sign In
                  </Link>
                  <Link
                    to="/register"
                    onClick={() => setMobileOpen(false)}
                    className="block btn-primary text-sm text-center"
                  >
                    Get Started
                  </Link>
                </>
              )}
            </div>
          )}
        </div>
      </nav>

      {/* About modal */}
      <AboutModal open={aboutOpen} onClose={() => setAboutOpen(false)} />
    </>
  );
}
