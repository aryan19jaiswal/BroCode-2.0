import type { Feature } from '@/types';

interface FeatureCardProps {
  feature: Feature;
  index: number;
}

export function FeatureCard({ feature, index }: FeatureCardProps) {
  const Icon = feature.icon;
  const delayClass = `animation-delay-${(index + 1) * 100}`;

  return (
    <div
      className={`opacity-0 animate-fade-up ${delayClass} group p-6 rounded-2xl border border-border bg-surface hover:border-brand-200 hover:shadow-lg hover:shadow-brand-500/5 transition-all duration-300`}
    >
      <div className="w-11 h-11 rounded-xl bg-brand-50 flex items-center justify-center mb-4 group-hover:bg-brand-100 transition-colors duration-300">
        <Icon className="w-5 h-5 text-brand-600" />
      </div>
      <h3 className="text-base font-bold text-text mb-2">{feature.title}</h3>
      <p className="text-sm text-text-secondary leading-relaxed">{feature.description}</p>
    </div>
  );
}
