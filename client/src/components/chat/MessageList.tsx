import { useRef, useEffect, memo } from 'react';
import { MessageBubble } from './MessageBubble';
import type { ChatMessage } from '@/types';

interface MessageListProps {
  messages: ChatMessage[];
  isStreaming: boolean;
}

export const MessageList = memo(function MessageList({ messages, isStreaming }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, messages[messages.length - 1]?.content]);

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
        <div className="w-20 h-20 rounded-2xl bg-brand-50 flex items-center justify-center mb-6">
          <span className="text-4xl">🤙</span>
        </div>
        <h2 className="text-2xl font-bold mb-2 text-gradient">Yo, what's good bro?</h2>
        <p className="text-text-secondary max-w-md leading-relaxed">
          I'm BroCode — drop me a question about code, architecture, debugging, or anything tech.
          Let's debug it.
        </p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto px-4 sm:px-6 py-6">
      {messages.map((msg, i) => (
        <MessageBubble
          key={msg.id}
          message={msg}
          isStreaming={isStreaming && i === messages.length - 1 && msg.role === 'assistant'}
        />
      ))}
      <div ref={bottomRef} />
    </div>
  );
});
