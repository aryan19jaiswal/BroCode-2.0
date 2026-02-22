import { create } from 'zustand';
import type { ChatMessage, ChatSession } from '@/types';
import { streamChat } from '@/services/stream';

interface ChatState {
  sessions: ChatSession[];
  activeSessionId: string | null;
  isStreaming: boolean;
  abortController: AbortController | null;

  // Actions
  sendMessage: (question: string) => Promise<void>;
  stopStreaming: () => void;
  createNewSession: () => void;
  setActiveSession: (id: string) => void;
  deleteSession: (id: string) => void;
}

function generateId(): string {
  return crypto.randomUUID();
}

export const useChatStore = create<ChatState>((set, get) => ({
  sessions: [],
  activeSessionId: null,
  isStreaming: false,
  abortController: null,

  createNewSession: () => {
    set({ activeSessionId: null });
  },

  setActiveSession: (id) => {
    set({ activeSessionId: id });
  },

  deleteSession: (id) => {
    set((state) => {
      const sessions = state.sessions.filter((s) => s.id !== id);
      const activeSessionId = state.activeSessionId === id ? null : state.activeSessionId;
      return { sessions, activeSessionId };
    });
  },

  stopStreaming: () => {
    const { abortController } = get();
    abortController?.abort();
    set({ isStreaming: false, abortController: null });
  },

  sendMessage: async (question) => {
    const { activeSessionId, isStreaming } = get();
    if (isStreaming) return;

    const userMessage: ChatMessage = {
      id: generateId(),
      role: 'user',
      content: question,
      timestamp: Date.now(),
    };

    const assistantMessage: ChatMessage = {
      id: generateId(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
    };

    // If there's an active session, append to it. Otherwise, create a temp entry.
    let sessionId = activeSessionId;

    if (sessionId) {
      // Append user message to existing session
      set((state) => ({
        sessions: state.sessions.map((s) =>
          s.id === sessionId
            ? { ...s, messages: [...s.messages, userMessage, assistantMessage] }
            : s
        ),
      }));
    } else {
      // We'll create the session once we get the server sessionId
      const tempId = '__pending__';
      sessionId = tempId;
      set((state) => ({
        activeSessionId: tempId,
        sessions: [
          {
            id: tempId,
            title: question.slice(0, 50) + (question.length > 50 ? '…' : ''),
            messages: [userMessage, assistantMessage],
            createdAt: Date.now(),
          },
          ...state.sessions,
        ],
      }));
    }

    const abortController = new AbortController();
    set({ isStreaming: true, abortController });

    const currentSessionId = sessionId;
    let serverSessionId: string | null = activeSessionId === '__pending__' ? null : activeSessionId;

    await streamChat(
      { question, sessionId: serverSessionId },
      (content, sId) => {
        // First chunk — capture server sessionId
        if (!serverSessionId) {
          serverSessionId = sId;
          // Replace temp id with real server sessionId
          set((state) => ({
            activeSessionId: sId,
            sessions: state.sessions.map((s) =>
              s.id === currentSessionId ? { ...s, id: sId } : s
            ),
          }));
        }

        // Append chunk to the assistant message
        set((state) => ({
          sessions: state.sessions.map((s) => {
            const sid = serverSessionId ?? currentSessionId;
            if (s.id !== sid) return s;
            const msgs = [...s.messages];
            const last = msgs[msgs.length - 1];
            if (last?.role === 'assistant') {
              msgs[msgs.length - 1] = { ...last, content: last.content + content };
            }
            return { ...s, messages: msgs };
          }),
        }));
      },
      () => {
        set({ isStreaming: false, abortController: null });
      },
      (error) => {
        // Put error in assistant message
        set((state) => ({
          isStreaming: false,
          abortController: null,
          sessions: state.sessions.map((s) => {
            const sid = serverSessionId ?? currentSessionId;
            if (s.id !== sid) return s;
            const msgs = [...s.messages];
            const last = msgs[msgs.length - 1];
            if (last?.role === 'assistant') {
              msgs[msgs.length - 1] = {
                ...last,
                content: last.content || `⚠️ ${error}`,
              };
            }
            return { ...s, messages: msgs };
          }),
        }));
      },
      abortController.signal
    );
  },
}));
