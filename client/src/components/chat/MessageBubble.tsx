import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkBreaks from 'remark-breaks';
import { Copy, Check } from 'lucide-react';
import { useState, useCallback, memo } from 'react';
import type { ChatMessage } from '@/types';

interface MessageBubbleProps {
  message: ChatMessage;
  isStreaming?: boolean;
}

const CodeBlock = memo(function CodeBlock({ children, className }: { children: string; className?: string }) {
  const [copied, setCopied] = useState(false);
  const language = className?.replace('language-', '') ?? '';

  const handleCopy = useCallback(async () => {
    await navigator.clipboard.writeText(children.trim());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [children]);

  return (
    <div className="relative group my-3 rounded-xl overflow-hidden border border-border">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-2 bg-surface-hover text-xs">
        <span className="text-text-secondary font-mono">
          {language || 'code'}
        </span>
        <button
          onClick={handleCopy}
          className="flex items-center gap-1.5 text-text-secondary
                     hover:text-brand-600 transition-colors duration-150 cursor-pointer"
        >
          {copied ? <Check className="w-3.5 h-3.5 text-green-500" /> : <Copy className="w-3.5 h-3.5" />}
          <span>{copied ? 'Copied!' : 'Copy'}</span>
        </button>
      </div>
      {/* Code body */}
      <pre className="overflow-x-auto p-4 bg-surface-alt text-sm leading-relaxed">
        <code className={`font-mono ${className ?? ''}`}>{children}</code>
      </pre>
    </div>
  );
});

export const MessageBubble = memo(function MessageBubble({ message, isStreaming }: MessageBubbleProps) {
  const isUser = message.role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
      <div
        className={`
          max-w-[85%] lg:max-w-[70%] px-5 py-3.5 rounded-2xl
          ${
            isUser
              ? 'bg-gradient-to-br from-brand-500 to-brand-600 text-white shadow-lg shadow-brand-500/20'
              : 'glass-card'
          }
        `}
      >
        {isUser ? (
          <p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>
        ) : (
          <div className="markdown-body prose prose-sm max-w-none prose-pre:p-0 prose-pre:bg-transparent prose-pre:m-0">
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkBreaks]}
              components={{
                code({ className, children, ...props }) {
                  const isInline = !className;
                  if (isInline) {
                    return (
                      <code
                        className="px-1.5 py-0.5 rounded-md bg-brand-50 text-brand-600 font-mono text-[0.85em]"
                        {...props}
                      >
                        {children}
                      </code>
                    );
                  }
                  return (
                    <CodeBlock className={className}>
                      {String(children).replace(/\n$/, '')}
                    </CodeBlock>
                  );
                },
                a({ href, children }) {
                  return (
                    <a href={href} target="_blank" rel="noopener noreferrer" className="text-brand-600 hover:text-brand-700 underline underline-offset-2">
                      {children}
                    </a>
                  );
                },
              }}
            >
              {message.content}
            </ReactMarkdown>
            {isStreaming && !message.content && (
              <span className="typing-cursor text-sm" />
            )}
            {isStreaming && message.content && (
              <span className="typing-cursor" />
            )}
          </div>
        )}
      </div>
    </div>
  );
});
