import type { QuestionDto, ResponseDto } from '@/types';
import { joinApiUrl } from './apiBase';

/**
 * Streams the AI response using the ReadableStream API.
 * The backend returns SSE (text/event-stream) with ResponseDto JSON per event.
 */
export async function streamChat(
  payload: QuestionDto,
  onChunk: (content: string, sessionId: string) => void,
  onDone: () => void,
  onError: (error: string) => void,
  signal?: AbortSignal
): Promise<void> {
  try {
    const res = await fetch(joinApiUrl('/bro/broCode'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload),
      signal,
    });

    if (!res.ok) {
      if (res.status === 401) {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
        onError('Session expired — please log in again');
        return;
      }
      onError(`Server responded with ${res.status}`);
      return;
    }

    const reader = res.body?.getReader();
    if (!reader) {
      onError('ReadableStream not supported');
      return;
    }

    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // SSE format: "data:{json}\n\n"
      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || trimmed === '') continue;

        if (trimmed.startsWith('data:')) {
          const jsonStr = trimmed.slice(5).trim();
          if (!jsonStr || jsonStr === '[DONE]') continue;

          try {
            const parsed: ResponseDto = JSON.parse(jsonStr);
            onChunk(parsed.response, parsed.sessionId);
          } catch {
            // Not valid JSON — could be partial; skip
          }
        }
      }
    }

    onDone();
  } catch (err: unknown) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      onDone();
      return;
    }
    onError(err instanceof Error ? err.message : 'Stream failed');
  }
}
