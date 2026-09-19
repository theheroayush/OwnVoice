"use client";

import { useState } from "react";
import Link from "next/link";
import { Plus, Minus, ArrowRight, Mic } from "lucide-react";

export function FaqAndCtaSection() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const faqs = [
    {
      q: "Is Own Voice really a one-time purchase?",
      a: "Yes. You buy Own Voice once and receive lifetime access to the desktop and web apps, including 1 year of free updates. There are zero recurring subscription fees from us.",
    },
    {
      q: "What does BYOK mean?",
      a: "BYOK stands for 'Bring Your Own Key'. You connect your personal API key directly from OpenAI, Google Gemini, Anthropic, or Groq. You pay the AI provider directly at wholesale inference rates (fractions of a cent per note), with zero platform markup.",
    },
    {
      q: "Do I have to pay for OpenAI or other providers separately?",
      a: "Yes, you pay your chosen provider directly at wholesale developer rates. For most people speaking 10–20 times a day, your monthly provider bill is around $0.50 to $1.50 — a fraction of typical $20/month SaaS subscriptions.",
    },
    {
      q: "Can I change AI providers?",
      a: "Yes, at any time. You can toggle between OpenAI (Whisper + GPT-4o), Google Gemini 2.5 Flash, Anthropic Claude 3.5, or Groq with a single click in your settings.",
    },
    {
      q: "What if I'm not a developer and don't know how to get an API key?",
      a: "You don't need any coding knowledge! We built a simple 60-second walkthrough right inside the app. You can get a free, lifetime key from Google AI Studio in 3 clicks with no credit card required.",
    },
    {
      q: "What is your refund policy?",
      a: "We offer a 30-day 100% money-back guarantee. If Own Voice doesn't immediately speed up your workflow and save you money, email support@ownvoice.ai for a prompt, hassle-free refund.",
    },
    {
      q: "Do you store my voice recordings?",
      a: "No. In strict accordance with our Ephemeral Audio Architecture, your voice is streamed to memory solely for transcription and discarded immediately after. We never retain, store, or train on your audio.",
    },
  ];

  const toggleFaq = (idx: number) => {
    setOpenIndex(openIndex === idx ? null : idx);
  };

  return (
    <section id="faq" className="py-24 px-4 sm:px-6 relative border-t border-white/[0.04]">
      <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-8 items-start">
        {/* Left Column: FAQ Accordion */}
        <div className="lg:col-span-7 space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Common questions.
            </h2>
            <Link
              href="/faq"
              className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-full bg-zinc-900 hover:bg-zinc-800 text-zinc-300 border border-white/10 transition-colors"
            >
              View all
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>

          <div className="space-y-3 pt-2">
            {faqs.map((item, idx) => {
              const isOpen = openIndex === idx;
              return (
                <div
                  key={idx}
                  className="rounded-2xl border border-white/[0.08] bg-[#0c0f16]/90 overflow-hidden transition-all duration-200"
                >
                  <button
                    type="button"
                    onClick={() => toggleFaq(idx)}
                    className="w-full px-5 py-4 text-left flex items-center justify-between text-sm sm:text-base font-semibold text-white hover:text-blue-400 transition-colors"
                  >
                    <span>{item.q}</span>
                    <div className="w-6 h-6 rounded-full bg-zinc-800/80 flex items-center justify-center text-zinc-400 flex-shrink-0 ml-4">
                      {isOpen ? <Minus className="w-3.5 h-3.5" /> : <Plus className="w-3.5 h-3.5" />}
                    </div>
                  </button>

                  {isOpen && (
                    <div className="px-5 pb-4 pt-1 text-xs sm:text-sm text-zinc-400 leading-relaxed border-t border-white/[0.04]">
                      {item.a}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Column: Radiant Blue CTA Banner with 3D Mic */}
        <div className="lg:col-span-5 h-full">
          <div className="h-full rounded-3xl bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-900 p-8 sm:p-10 text-white shadow-[0_25px_60px_rgba(37,99,235,0.4)] relative overflow-hidden flex flex-col justify-between min-h-[380px]">
            {/* Background lighting accents */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-sky-400/20 rounded-full blur-3xl pointer-events-none" />

            <div className="space-y-4 relative z-10">
              <h3 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white leading-tight">
                Ready to own<br />your voice?
              </h3>
              <p className="text-sm sm:text-base text-blue-100/90 leading-relaxed max-w-xs">
                Get started today. One purchase. A lifetime of possibilities.
              </p>
            </div>

            <div className="relative z-10 pt-8 flex items-center justify-between">
              <Link
                href="#pricing"
                className="inline-flex items-center gap-2 px-6 py-3.5 rounded-full bg-white hover:bg-zinc-100 text-zinc-950 font-bold text-sm shadow-xl active:scale-95 transition-all"
              >
                Get Own Voice
                <ArrowRight className="w-4 h-4" />
              </Link>

              {/* Glowing 3D Metallic Blue Mic Graphic */}
              <div className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-full bg-gradient-to-tr from-sky-400/40 via-white/20 to-transparent p-1 flex items-center justify-center shadow-[0_0_30px_rgba(255,255,255,0.3)]">
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-gradient-to-b from-sky-300 to-blue-600 flex items-center justify-center shadow-inner">
                  <Mic className="w-9 h-9 sm:w-11 sm:h-11 text-white filter drop-shadow-[0_4px_8px_rgba(0,0,0,0.4)]" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
