import { useState, useRef, useEffect, type KeyboardEvent, type FormEvent } from 'react';
import { SendHorizontal, StopCircle } from 'lucide-react';

interface ChatInputProps {
  onSend: (message: string) => void;
  onStop: () => void;
  isStreaming: boolean;
  disabled?: boolean;
}

export function ChatInput({ onSend, onStop, isStreaming, disabled }: ChatInputProps) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea
  useEffect(() => {
    const el = textareaRef.current;
    if (el) {
      el.style.height = 'auto';
      el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
    }
  }, [value]);

  const handleSubmit = (e?: FormEvent) => {
    e?.preventDefault();
    const trimmed = value.trim();
    if (!trimmed || isStreaming || disabled) return;
    onSend(trimmed);
    setValue('');
    // Reset height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="border-t border-border px-4 sm:px-6 py-4"
    >
      <div className="flex items-end gap-3 max-w-4xl mx-auto">
        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask your bro anything…"
            rows={1}
            disabled={disabled}
            className="input-field resize-none pr-4 min-h-[48px] max-h-[200px]"
          />
        </div>

        {isStreaming ? (
          <button
            type="button"
            onClick={onStop}
            className="shrink-0 w-12 h-12 rounded-xl bg-red-500/10 text-red-400
                       hover:bg-red-500/20 flex items-center justify-center
                       transition-all duration-200 cursor-pointer"
            aria-label="Stop generating"
          >
            <StopCircle className="w-5 h-5" />
          </button>
        ) : (
          <button
            type="submit"
            disabled={!value.trim() || disabled}
            className="shrink-0 w-12 h-12 rounded-xl btn-primary !p-0 flex items-center justify-center"
            aria-label="Send message"
          >
            <SendHorizontal className="w-5 h-5" />
          </button>
        )}
      </div>

      <p className="text-center text-xs text-text-tertiary mt-2">
        BroCode can make mistakes. Always verify critical code.
      </p>
    </form>
  );
}
