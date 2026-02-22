import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost' | 'danger';
  isLoading?: boolean;
  icon?: ReactNode;
}

export function Button({
  variant = 'primary',
  isLoading = false,
  icon,
  children,
  className = '',
  disabled,
  ...props
}: ButtonProps) {
  const base =
    variant === 'primary'
      ? 'btn-primary'
      : variant === 'danger'
        ? 'bg-red-500/10 text-red-400 hover:bg-red-500/20 rounded-xl px-4 py-2 transition-all duration-200 cursor-pointer'
        : 'btn-ghost';

  return (
    <button
      className={`${base} inline-flex items-center justify-center gap-2 ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <Loader2 className="w-4 h-4 animate-spin" />
      ) : icon ? (
        <span className="w-4 h-4 flex items-center justify-center">{icon}</span>
      ) : null}
      {children}
    </button>
  );
}
