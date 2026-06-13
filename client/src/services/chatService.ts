import api from './api';
import type { ChatSessionResponse } from '@/types';

export const chatService = {
  async getSessions(): Promise<ChatSessionResponse[]> {
    const res = await api.get<ChatSessionResponse[]>('/bro/sessions');
    return res.data;
  },

  async deleteSession(id: string): Promise<void> {
    await api.delete(`/bro/session/${id}`);
  },
};
