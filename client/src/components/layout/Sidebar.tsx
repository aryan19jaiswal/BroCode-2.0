import { memo, useCallback } from 'react';
import { MessageSquarePlus, Trash2, MessageCircle } from 'lucide-react';
import { useChatStore } from '@/stores/chatStore';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

export const Sidebar = memo(function Sidebar({ open, onClose }: SidebarProps) {
  const { sessions, activeSessionId, createNewSession, setActiveSession, deleteSession } =
    useChatStore();

  const handleNew = useCallback(() => {
    createNewSession();
    onClose();
  }, [createNewSession, onClose]);

  const handleSelect = useCallback(
    (id: string) => {
      setActiveSession(id);
      onClose();
    },
    [setActiveSession, onClose],
  );

  return (
    <>
      {/* Overlay */}
      {open && (
        <div
          className="fixed inset-0 bg-black/30 z-40 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar panel */}
      <aside
        className={`
          fixed lg:sticky top-0 left-0 z-50 h-screen w-72
          bg-surface-alt border-r border-border
          flex flex-col transition-transform duration-300 ease-out
          ${open ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
      >
        {/* Header */}
        <div className="p-4 border-b border-border">
          <button
            onClick={handleNew}
            className="w-full btn-primary flex items-center justify-center gap-2 text-sm"
          >
            <MessageSquarePlus className="w-4 h-4" />
            New Chat
          </button>
        </div>

        {/* Session list */}
        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {sessions.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-text-secondary opacity-60">
              <MessageCircle className="w-8 h-8 mb-2" />
              <p className="text-sm">No chats yet</p>
              <p className="text-xs mt-1">Start a conversation!</p>
            </div>
          ) : (
            sessions.map((session) => (
              <div
                key={session.id}
                className={`
                  group flex items-center gap-2 px-3 py-2.5 rounded-xl cursor-pointer
                  transition-all duration-150
                  ${
                    activeSessionId === session.id
                      ? 'bg-brand-500/10 text-brand-600'
                      : 'hover:bg-surface-hover text-text-secondary'
                  }
                `}
                onClick={() => handleSelect(session.id)}
              >
                <MessageCircle className="w-4 h-4 shrink-0" />
                <span className="flex-1 truncate text-sm">{session.title}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    deleteSession(session.id);
                  }}
                  className="opacity-0 group-hover:opacity-100 p-1 rounded-lg
                             hover:bg-red-50 text-red-400 transition-all duration-150"
                  aria-label="Delete chat"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-border">
          <p className="text-xs text-text-tertiary text-center">
            BroCode — Your Dev Bro
          </p>
        </div>
      </aside>
    </>
  );
});
