import { Link } from 'react-router-dom';
import { ArrowRight, Code2, Heart } from 'lucide-react';
import { FeatureCard } from '@/components/ui/FeatureCard';
import { Accordion } from '@/components/ui/Accordion';
import { FEATURES, FAQ_ITEMS } from '@/constants/landing';

export default function LandingPage() {
  return (
    <div className="min-h-[calc(100vh-64px)] overflow-x-hidden">
      {/* ─── Hero ─── */}
      <section className="relative px-4 pt-20 pb-24 sm:pt-28 sm:pb-32">
        {/* Background decoration */}
        <div className="absolute inset-0 -z-10 overflow-hidden">
          <div className="absolute -top-32 left-1/2 -translate-x-1/2 w-[600px] h-[600px] bg-brand-100/50 rounded-full blur-[120px]" />
          <div className="absolute top-40 -right-32 w-80 h-80 bg-brand-200/30 rounded-full blur-3xl" />
          <div className="absolute -bottom-20 -left-32 w-80 h-80 bg-brand-50/60 rounded-full blur-3xl" />
        </div>

        <div className="max-w-4xl mx-auto text-center">
          {/* Badge */}
          <div className="opacity-0 animate-fade-up inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-brand-50 border border-brand-200 text-brand-700 text-xs font-semibold mb-6">
            <Code2 className="w-3.5 h-3.5" />
            AI-Powered Coding Assistant
          </div>

          {/* Title */}
          <h1 className="opacity-0 animate-fade-up animation-delay-100 text-4xl sm:text-5xl md:text-6xl font-extrabold tracking-tight text-text leading-[1.1]">
            Your Code Bro,{' '}
            <span className="text-gradient">Always Online</span>
          </h1>

          {/* Subtitle */}
          <p className="opacity-0 animate-fade-up animation-delay-200 mt-6 text-lg sm:text-xl text-text-secondary max-w-2xl mx-auto leading-relaxed">
            BroCode is like your AI big brother that helps you debug, design systems, ace interviews,
            and write clean code.
          </p>

          {/* CTA */}
          <div className="opacity-0 animate-fade-up animation-delay-300 mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link to="/register" className="btn-primary text-base px-8 py-3 inline-flex items-center gap-2">
              Get Started Free
              <ArrowRight className="w-4 h-4" />
            </Link>
            <Link to="/login" className="btn-outline text-base px-8 py-3">
              Sign In
            </Link>
          </div>

          {/* Social proof */}
          <p className="opacity-0 animate-fade-up animation-delay-400 mt-8 text-xs text-text-tertiary">
            No credit card required · Instant access · Just Sign In
          </p>
        </div>
      </section>

      {/* ─── Features ─── */}
      <section className="px-4 py-20 bg-surface-alt">
        <div className="max-w-5xl mx-auto">
          <div className="text-center mb-14">
            <h2 className="text-3xl sm:text-4xl font-extrabold text-text">
              Everything You Need to{' '}
              <span className="text-gradient">Learn Faster</span>
            </h2>
            <p className="mt-4 text-text-secondary max-w-xl mx-auto">
              BroCode isn't just another chatbot. It's a purpose-built coding companion with real engineering features.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {FEATURES.map((feature, i) => (
              <FeatureCard key={feature.title} feature={feature} index={i} />
            ))}
          </div>
        </div>
      </section>

      {/* ─── About ─── */}
      <section className="px-4 py-20">
        <div className="max-w-4xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
            {/* Left */}
            <div>
              <h2 className="text-3xl sm:text-4xl font-extrabold text-text leading-tight">
                Built by a Developer,{' '}
                <span className="text-gradient">For Developers</span>
              </h2>
              <p className="mt-5 text-text-secondary leading-relaxed">
                BroCode was born from late-night debugging sessions and the wish that every dev
                had a patient, brilliant senior engineer on speed dial. No corporate jargon,
                no hand-waving — just clean code and clear explanations.
              </p>
              <ul className="mt-6 space-y-3">
                {[
                  'Streaming responses via SSE — no waiting',
                  'Strict coding-only guardrails — no off-topic noise',
                  'Complexity analysis with every solution',
                  'Secure JWT auth with HttpOnly cookies',
                ].map((item) => (
                  <li key={item} className="flex items-start gap-2.5 text-sm text-text-secondary">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-500 shrink-0 mt-1.5" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>

            {/* Right — code preview */}
            <div className="rounded-2xl border border-border overflow-hidden shadow-xl shadow-brand-500/5">
              <div className="flex items-center gap-2 px-4 py-3 bg-surface-alt border-b border-border">
                <span className="w-3 h-3 rounded-full bg-red-400" />
                <span className="w-3 h-3 rounded-full bg-amber-400" />
                <span className="w-3 h-3 rounded-full bg-green-400" />
                <span className="ml-3 text-xs text-text-tertiary font-mono">brocode.tsx</span>
              </div>
              <pre className="p-5 bg-surface-alt text-sm font-mono leading-relaxed overflow-x-auto">
                <code>
                  <span className="text-brand-600">const</span>{' '}
                  <span className="text-amber-600">bro</span>{' '}
                  <span className="text-text-secondary">=</span>{' '}
                  <span className="text-brand-600">new</span>{' '}
                  <span className="text-blue-600">BroCode</span>
                  <span className="text-text-secondary">();</span>{'\n\n'}
                  <span className="text-text-tertiary">{'// Ask anything coding-related'}</span>{'\n'}
                  <span className="text-brand-600">const</span>{' '}
                  <span className="text-amber-600">answer</span>{' '}
                  <span className="text-text-secondary">=</span>{' '}
                  <span className="text-brand-600">await</span>{' '}
                  <span className="text-amber-600">bro</span>
                  <span className="text-text-secondary">.</span>
                  <span className="text-blue-600">ask</span>
                  <span className="text-text-secondary">(</span>{'\n'}
                  <span className="text-green-600">{'  "Solve LeetCode 201"'}</span>{'\n'}
                  <span className="text-text-secondary">);</span>{'\n\n'}
                  <span className="text-text-tertiary">{'// Clean code + complexity'}</span>{'\n'}
                  <span className="text-amber-600">console</span>
                  <span className="text-text-secondary">.</span>
                  <span className="text-blue-600">log</span>
                  <span className="text-text-secondary">(</span>
                  <span className="text-amber-600">answer</span>
                  <span className="text-text-secondary">);</span>
                </code>
              </pre>
            </div>
          </div>
        </div>
      </section>

      {/* ─── FAQ ─── */}
      <section className="px-4 py-20 bg-surface-alt">
        <div className="max-w-2xl mx-auto">
          <div className="text-center mb-12">
            <h2 className="text-3xl sm:text-4xl font-extrabold text-text">
              Frequently Asked{' '}
              <span className="text-gradient">Questions</span>
            </h2>
            <p className="mt-3 text-text-secondary">
              Got questions? We've got answers.
            </p>
          </div>
          <Accordion items={FAQ_ITEMS} />
        </div>
      </section>

      {/* ─── CTA ─── */}
      <section className="px-4 py-24">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-3xl sm:text-4xl font-extrabold text-text">
            Ready to Code{' '}
            <span className="text-gradient">Smarter</span>?
          </h2>
          <p className="mt-4 text-text-secondary text-lg max-w-xl mx-auto">
            Join BroCode and get an AI mentor that actually speaks every engineer's love language.
          </p>
          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link to="/register" className="btn-primary text-base px-8 py-3 inline-flex items-center gap-2">
              Create Free Account
              <ArrowRight className="w-4 h-4" />
            </Link>
            <Link to="/login" className="btn-outline text-base px-8 py-3">
              Sign In
            </Link>
          </div>
        </div>
      </section>

      {/* ─── Footer ─── */}
      <footer className="border-t border-border px-4 py-6">
        <div className="max-w-5xl mx-auto flex items-center justify-center gap-1.5 text-xs text-text-tertiary">
          <span>Made with</span>
          <Heart className="w-3 h-3 text-red-400 fill-red-400" />
          <span>by Aryan Jaiswal, Marde Manish, Genda Alok, System Prince · © {new Date().getFullYear()} BroCode</span>
        </div>
      </footer>
    </div>
  );
}
