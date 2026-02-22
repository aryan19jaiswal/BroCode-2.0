import { useState, useCallback } from 'react';
import { ChevronDown } from 'lucide-react';
import type { FaqItem } from '@/types';

interface AccordionItemProps {
  item: FaqItem;
  isOpen: boolean;
  onToggle: () => void;
}

function AccordionItem({ item, isOpen, onToggle }: AccordionItemProps) {
  return (
    <div className="border border-border rounded-xl overflow-hidden transition-colors hover:border-brand-200">
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between gap-4 px-5 py-4 text-left cursor-pointer"
        aria-expanded={isOpen}
      >
        <span className="text-sm font-semibold text-text">{item.question}</span>
        <ChevronDown
          className={`w-4 h-4 shrink-0 text-text-secondary transition-transform duration-300 ${
            isOpen ? 'rotate-180' : ''
          }`}
        />
      </button>
      <div
        className={`grid transition-[grid-template-rows] duration-300 ease-out ${
          isOpen ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'
        }`}
      >
        <div className="overflow-hidden">
          <p className="px-5 pb-4 text-sm text-text-secondary leading-relaxed">
            {item.answer}
          </p>
        </div>
      </div>
    </div>
  );
}

interface AccordionProps {
  items: readonly FaqItem[];
}

export function Accordion({ items }: AccordionProps) {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  const handleToggle = useCallback((index: number) => {
    setOpenIndex((prev) => (prev === index ? null : index));
  }, []);

  return (
    <div className="space-y-3">
      {items.map((item, i) => (
        <AccordionItem
          key={i}
          item={item}
          isOpen={openIndex === i}
          onToggle={() => handleToggle(i)}
        />
      ))}
    </div>
  );
}
