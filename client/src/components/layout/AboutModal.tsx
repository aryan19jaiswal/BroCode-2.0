import { Modal } from '@/components/ui/Modal';
import { Accordion } from '@/components/ui/Accordion';
import { FEATURES, FAQ_ITEMS } from '@/constants/landing';
import { Code2, Heart } from 'lucide-react';

interface AboutModalProps {
  open: boolean;
  onClose: () => void;
}

export function AboutModal({ open, onClose }: AboutModalProps) {
  return (
    <Modal open={open} onClose={onClose} title="About BroCode">
      <div className="space-y-8">
        {/* Intro */}
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center shrink-0">
            <Code2 className="w-6 h-6 text-white" />
          </div>
          <div>
            <h3 className="text-base font-bold text-text mb-1">BroCode — Your Dev Bro</h3>
            <p className="text-sm text-text-secondary leading-relaxed">
              An AI-powered coding assistant that talks like your senior dev big brother.
              Powered by Google Gemini, built with Spring Boot & React. Fast, secure, and laser-focused on code.
            </p>
          </div>
        </div>

        {/* Features */}
        <div>
          <h3 className="text-sm font-bold text-text mb-3 uppercase tracking-wider">Features</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {FEATURES.map((feature) => {
              const Icon = feature.icon;
              return (
                <div key={feature.title} className="flex items-start gap-3 p-3 rounded-xl border border-border">
                  <div className="w-8 h-8 rounded-lg bg-brand-50 flex items-center justify-center shrink-0">
                    <Icon className="w-4 h-4 text-brand-600" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-text">{feature.title}</p>
                    <p className="text-xs text-text-secondary leading-relaxed mt-0.5">{feature.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* FAQ */}
        <div>
          <h3 className="text-sm font-bold text-text mb-3 uppercase tracking-wider">FAQ</h3>
          <Accordion items={FAQ_ITEMS} />
        </div>

        {/* Footer */}
        <div className="flex items-center justify-center gap-1.5 text-xs text-text-tertiary pt-2">
          <span>Made with</span>
          <Heart className="w-3 h-3 text-red-400 fill-red-400" />
          <span>by Aryan Jaiswal, Marde Manish, Genda Alok, System Prince</span>
        </div>
      </div>
    </Modal>
  );
}
