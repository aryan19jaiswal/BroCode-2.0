import { useState, useCallback, useRef, useEffect, type ReactNode } from 'react';

interface TooltipProps {
  label: string;
  children: ReactNode;
  position?: 'top' | 'bottom';
  delay?: number;
}

export function Tooltip({ label, children, position = 'bottom', delay = 200 }: TooltipProps) {
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const show = useCallback(() => {
    timeoutRef.current = setTimeout(() => setVisible(true), delay);
  }, [delay]);

  const hide = useCallback(() => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setVisible(false);
  }, []);

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const positionClasses =
    position === 'top'
      ? 'bottom-full mb-2 left-1/2 -translate-x-1/2'
      : 'top-full mt-2 left-1/2 -translate-x-1/2';

  return (
    <div
      className="relative inline-flex"
      onMouseEnter={show}
      onMouseLeave={hide}
      onFocus={show}
      onBlur={hide}
    >
      {children}
      {visible && (
        <div
          role="tooltip"
          className={`absolute z-50 ${positionClasses} pointer-events-none tooltip-animate`}
        >
          <span className="block whitespace-nowrap rounded-lg bg-text px-2.5 py-1 text-xs font-medium text-white shadow-lg">
            {label}
          </span>
        </div>
      )}
    </div>
  );
}
