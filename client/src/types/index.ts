import type { ComponentType } from 'react';

/* ──── Auth ──── */
export interface AuthRequest {
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export interface ApiResponse {
  message: string;
  success: boolean;
}

export interface LoginResponse extends ApiResponse {
  username?: string;
}

export interface ProfileResponse {
  username: string;
  success: boolean;
}

export interface UserProfile {
  email?: string;
  username?: string;
  password?: string;
}

/* ──── Chat ──── */
export interface QuestionDto {
  question: string;
  sessionId: string | null;
}

export interface ResponseDto {
  response: string;
  sessionId: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

export interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: number;
}

/* ──── Landing ──── */
export interface Feature {
  icon: ComponentType<{ className?: string }>;
  title: string;
  description: string;
}

export interface FaqItem {
  question: string;
  answer: string;
}
