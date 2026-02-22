import type { Feature, FaqItem } from '@/types';
import {
  Zap,
  Shield,
  MessageSquare,
  Code2,
  Sparkles,
  Globe,
} from 'lucide-react';

export const FEATURES: readonly Feature[] = [
  {
    icon: Zap,
    title: 'Lightning Fast',
    description: 'Streaming AI responses in real-time with SSE. No waiting, just instant coding help.',
  },
  {
    icon: Shield,
    title: 'Secure by Design',
    description: 'JWT cookie-based auth, encrypted sessions, and zero data leaks. Your code stays yours.',
  },
  {
    icon: MessageSquare,
    title: 'Contextual Memory',
    description: 'BroCode remembers your conversation context for coherent, multi-turn coding sessions.',
  },
  {
    icon: Code2,
    title: 'Code-First Responses',
    description: 'Every answer includes clean, copy-paste ready code blocks with syntax highlighting.',
  },
  {
    icon: Sparkles,
    title: 'Complexity Analysis',
    description: 'Automatic time & space complexity breakdown for every solution. Interview-ready output.',
  },
  {
    icon: Globe,
    title: 'Any Language, Any Stack',
    description: 'Java, Python, TypeScript, Rust, Go — BroCode speaks every language fluently.',
  },
] as const;

export const FAQ_ITEMS: readonly FaqItem[] = [
  {
    question: 'What is BroCode?',
    answer:
      'BroCode is an AI-powered coding assistant that talks like your big brother. It helps with debugging, algorithms, system design, code reviews, and interview prep — all in a fun, no-fluff style.',
  },
  {
    question: 'What topics does BroCode cover?',
    answer:
      'Everything coding-related: programming languages, frameworks, databases, DevOps, algorithms, system design, security, and more. BroCode strictly focuses on software engineering — no off-topic fluff.',
  },
  {
    question: 'Does BroCode remember my conversations?',
    answer:
      'Yes! Each chat session maintains full context so BroCode can give coherent, multi-turn answers. Sessions are stored in-memory with automatic TTL-based cleanup.',
  },
  {
    question: 'Is my data secure?',
    answer:
      'Absolutely. Brocode has got your back.',
  },
  {
    question: 'Can I use BroCode for interview prep?',
    answer:
      'Definitely! BroCode provides structured answers with complexity analysis, multiple approaches, and pro tips — exactly the format used in real coding interviews.',
  },
  {
    question: 'What AI model powers BroCode?',
    answer:
      'BroCode is powered by Google\'s Gemini 2.5 Flash Lite model via Langchain4j, optimized for fast, accurate coding assistance with streaming responses.',
  },
] as const;
