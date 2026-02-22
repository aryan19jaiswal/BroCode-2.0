import { useState } from 'react';
import { PanelLeftOpen } from 'lucide-react';
import { Sidebar } from '@/components/layout/Sidebar';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { useChatStore } from '@/stores/chatStore';
import { useAuthStore } from '@/stores/authStore';

export default function ChatPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { sessions, activeSessionId, isStreaming, sendMessage, stopStreaming } = useChatStore();
  const username = useAuthStore((s) => s.username);

  const activeSession = sessions.find((s) => s.id === activeSessionId);
  const messages = activeSession?.messages ?? [];

  return (
    <div className="flex h-[calc(100vh-64px)]">
      {/* Sidebar */}
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main chat area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top bar with sidebar toggle + username */}
        <div className="flex items-center gap-3 px-4 py-3 border-b border-border">
          <button
            onClick={() => setSidebarOpen(true)}
            className="lg:hidden btn-ghost p-2"
            aria-label="Open sidebar"
          >
            <PanelLeftOpen className="w-5 h-5" />
          </button>
          <h2 className="text-sm font-medium text-text-secondary truncate flex-1">
            {activeSession?.title ?? 'New conversation'}
          </h2>
          {username && (
            <span className="text-xs font-medium text-text-tertiary hidden sm:block">
              {username}
            </span>
          )}
        </div>

        {/* Messages */}
        <MessageList messages={messages} isStreaming={isStreaming} />

        {/* Input */}
        <ChatInput
          onSend={sendMessage}
          onStop={stopStreaming}
          isStreaming={isStreaming}
        />
      </div>
    </div>
  );
}
