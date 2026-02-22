import { Loader2 } from 'lucide-react';

export function Spinner({ className = '' }: { className?: string }) {
  return <Loader2 className={`animate-spin text-brand-500 ${className}`} />;
}

export function FullPageSpinner() {
  return (
    <div className="fixed inset-0 flex items-center justify-center bg-surface z-50">
      <div className="flex flex-col items-center gap-4">
        <Spinner className="w-10 h-10" />
        <p className="text-sm text-text-secondary animate-pulse">
          Loading…
        </p>
      </div>
    </div>
  );
}
