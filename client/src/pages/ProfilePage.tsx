import { useState, type FormEvent } from 'react';
import { User, Mail, Lock, Save } from 'lucide-react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { authService } from '@/services/auth';
import { useAuthStore } from '@/stores/authStore';
import toast from 'react-hot-toast';

export default function ProfilePage() {
  const currentUsername = useAuthStore((s) => s.username);
  const setUsername = useAuthStore((s) => s.setUsername);

  const [username, setUsernameLocal] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const payload: Record<string, string> = {};
    if (username.trim()) payload.username = username.trim();
    if (email.trim()) payload.email = email.trim();
    if (password.trim()) payload.password = password.trim();

    if (Object.keys(payload).length === 0) {
      toast.error('Nothing to update — fill in at least one field');
      return;
    }

    setIsLoading(true);
    try {
      const res = await authService.updateProfile(payload);
      if (res.success) {
        toast.success('Profile updated! Nice one, bro.');
        if (payload.username) setUsername(payload.username);
        setPassword('');
      } else {
        toast.error(res.message);
      }
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? 'Update failed';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-start justify-center px-4 py-12">
      <div className="w-full max-w-lg">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <div className="w-14 h-14 rounded-2xl bg-brand-50 flex items-center justify-center">
            <User className="w-7 h-7 text-brand-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Profile Settings</h1>
            {currentUsername && (
              <p className="text-sm font-medium text-brand-600">@{currentUsername}</p>
            )}
            <p className="text-sm text-text-secondary">
              Update your account info
            </p>
          </div>
        </div>

        {/* Form */}
        <div className="glass-card p-8">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="relative">
              <Input
                label="Username"
                type="text"
                placeholder="New username"
                value={username}
                onChange={(e) => setUsernameLocal(e.target.value)}
                autoComplete="username"
              />
              <User className="absolute right-3 top-[38px] w-4 h-4 text-text-secondary pointer-events-none" />
            </div>

            <div className="relative">
              <Input
                label="Email"
                type="email"
                placeholder="New email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
              />
              <Mail className="absolute right-3 top-[38px] w-4 h-4 text-text-secondary pointer-events-none" />
            </div>

            <div className="relative">
              <Input
                label="Password"
                type="password"
                placeholder="New password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
              />
              <Lock className="absolute right-3 top-[38px] w-4 h-4 text-text-secondary pointer-events-none" />
            </div>

            <p className="text-xs text-text-secondary">
              Leave fields blank to keep them unchanged.
            </p>

            <Button
              type="submit"
              isLoading={isLoading}
              icon={<Save className="w-4 h-4" />}
              className="w-full"
            >
              Save Changes
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
