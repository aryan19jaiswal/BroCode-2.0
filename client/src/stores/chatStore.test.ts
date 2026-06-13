import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useChatStore } from './chatStore';
import { streamChat } from '@/services/stream';
import { chatService } from '@/services/chatService';

vi.mock('@/services/stream', () => ({
  streamChat: vi.fn(),
}));

vi.mock('@/services/chatService', () => ({
  chatService: {
    getSessions: vi.fn(),
    deleteSession: vi.fn(),
  },
}));

const resetStore = () =>
  useChatStore.setState({
    sessions: [],
    activeSessionId: null,
    isStreaming: false,
    abortController: null,
  });

beforeEach(() => {
  vi.clearAllMocks();
  resetStore();
  // Default no-op mocks so tests that don't care about these don't fail
  vi.mocked(chatService.getSessions).mockResolvedValue([]);
  vi.mocked(chatService.deleteSession).mockResolvedValue(undefined);
});

describe('chatStore', () => {
  describe('sendMessage', () => {
    it('creates a new session and accumulates chunks', async () => {
      vi.mocked(streamChat).mockImplementation(async (_payload, onChunk, onDone) => {
        onChunk('Hello, ', 'server-sess-1');
        onChunk('bro!', 'server-sess-1');
        onDone();
      });

      await useChatStore.getState().sendMessage('How do hooks work?');

      const { sessions, activeSessionId, isStreaming } = useChatStore.getState();
      expect(sessions).toHaveLength(1);
      expect(sessions[0].id).toBe('server-sess-1');
      expect(activeSessionId).toBe('server-sess-1');
      expect(isStreaming).toBe(false);

      const msgs = sessions[0].messages;
      expect(msgs).toHaveLength(2);
      expect(msgs[0].role).toBe('user');
      expect(msgs[0].content).toBe('How do hooks work?');
      expect(msgs[1].role).toBe('assistant');
      expect(msgs[1].content).toBe('Hello, bro!');
    });

    it('appends new messages to an existing session', async () => {
      useChatStore.setState({
        activeSessionId: 'existing-sess',
        sessions: [
          {
            id: 'existing-sess',
            title: 'Existing chat',
            messages: [
              { id: 'u1', role: 'user', content: 'First question', timestamp: 0 },
              { id: 'a1', role: 'assistant', content: 'First answer', timestamp: 0 },
            ],
            createdAt: 0,
          },
        ],
      });

      vi.mocked(streamChat).mockImplementation(async (_payload, onChunk, onDone) => {
        onChunk('Follow-up answer', 'existing-sess');
        onDone();
      });

      await useChatStore.getState().sendMessage('Follow-up question');

      const { sessions } = useChatStore.getState();
      expect(sessions[0].messages).toHaveLength(4);
      expect(sessions[0].messages[3].content).toBe('Follow-up answer');
    });

    it('is a no-op when already streaming', async () => {
      useChatStore.setState({ isStreaming: true });

      await useChatStore.getState().sendMessage('Another question');

      expect(streamChat).not.toHaveBeenCalled();
    });

    it('puts an error message into the assistant bubble on stream error', async () => {
      vi.mocked(streamChat).mockImplementation(async (_payload, _onChunk, _onDone, onError) => {
        onError('Connection lost');
      });

      await useChatStore.getState().sendMessage('test question');

      const { sessions, isStreaming } = useChatStore.getState();
      expect(isStreaming).toBe(false);
      const assistantMsg = sessions[0].messages[1];
      expect(assistantMsg.role).toBe('assistant');
      expect(assistantMsg.content).toBe('⚠️ Connection lost');
    });
  });

  describe('deleteSession', () => {
    it('calls the API and removes the session from state', async () => {
      useChatStore.setState({
        sessions: [
          { id: 'sess-1', title: 'Test', messages: [], createdAt: 0 },
          { id: 'sess-2', title: 'Keep', messages: [], createdAt: 0 },
        ],
        activeSessionId: 'sess-1',
      });

      await useChatStore.getState().deleteSession('sess-1');

      expect(chatService.deleteSession).toHaveBeenCalledWith('sess-1');
      const { sessions, activeSessionId } = useChatStore.getState();
      expect(sessions).toHaveLength(1);
      expect(sessions[0].id).toBe('sess-2');
      expect(activeSessionId).toBeNull();
    });

    it('removes from state even when API call throws', async () => {
      vi.mocked(chatService.deleteSession).mockRejectedValue(new Error('Network error'));
      useChatStore.setState({
        sessions: [{ id: 'sess-1', title: 'Test', messages: [], createdAt: 0 }],
        activeSessionId: null,
      });

      await useChatStore.getState().deleteSession('sess-1');

      expect(useChatStore.getState().sessions).toHaveLength(0);
    });
  });

  describe('loadSessions', () => {
    it('hydrates sessions from the API response', async () => {
      vi.mocked(chatService.getSessions).mockResolvedValue([
        {
          id: 'sess-1',
          title: 'React hooks',
          messages: [
            { role: 'user', content: 'How do hooks work?' },
            { role: 'assistant', content: 'Hooks are...' },
            { role: 'system', content: 'sys-prompt' }, // should be filtered out
          ],
          createdAt: 1_000_000,
        },
      ]);

      await useChatStore.getState().loadSessions();

      const { sessions } = useChatStore.getState();
      expect(sessions).toHaveLength(1);
      expect(sessions[0].id).toBe('sess-1');
      expect(sessions[0].title).toBe('React hooks');
      // System message is filtered
      expect(sessions[0].messages).toHaveLength(2);
      expect(sessions[0].messages[0].role).toBe('user');
      expect(sessions[0].messages[1].role).toBe('assistant');
    });

    it('silently ignores API errors and leaves sessions empty', async () => {
      vi.mocked(chatService.getSessions).mockRejectedValue(new Error('Network error'));

      await useChatStore.getState().loadSessions();

      expect(useChatStore.getState().sessions).toHaveLength(0);
    });
  });
});
