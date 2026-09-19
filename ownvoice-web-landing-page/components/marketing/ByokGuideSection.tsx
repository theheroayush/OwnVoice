"use client";

import React, { useState } from "react";
import { KeyRound, ShieldCheck, ExternalLink, Zap, CheckCircle2, Lock, HelpCircle } from "lucide-react";

interface ProviderTab {
  id: string;
  name: string;
  tag: string;
  tagColor: string;
  freeTier: string;
  setupTime: string;
  steps: string[];
  link: string;
}

const PROVIDERS: ProviderTab[] = [
  {
    id: "gemini",
    name: "Google Gemini",
    tag: "Recommended • 100% Free Tier",
    tagColor: "text-blue-400 bg-blue-500/10 border-blue-500/30",
    freeTier: "Free forever (15 RPM included, perfect for personal voice dictation)",
    setupTime: "60 seconds",
    steps: [
      "Open Google AI Studio (aistudio.google.com) with your regular Google account.",
      "Click the blue 'Get API Key' button and tap 'Create API Key'.",
      "Copy the key and paste it into Own Voice Settings.",
      "Zero credit card required. You're ready to speak!",
    ],
    link: "https://aistudio.google.com/app/apikey",
  },
  {
    id: "groq",
    name: "Groq Cloud",
    tag: "Blazing Fast (Whisper ~200ms)",
    tagColor: "text-amber-400 bg-amber-500/10 border-amber-500/30",
    freeTier: "Generous free developer tier with high rate limits",
    setupTime: "90 seconds",
    steps: [
      "Log in to console.groq.com using GitHub or Google.",
      "Navigate to 'API Keys' and click 'Create API Key'.",
      "Copy the key into Own Voice Settings.",
      "Experience near-instant 200ms speech-to-text with Groq LPU acceleration.",
    ],
    link: "https://console.groq.com/keys",
  },
  {
    id: "openai",
    name: "OpenAI",
    tag: "Industry Standard (Whisper + GPT-4o)",
    tagColor: "text-emerald-400 bg-emerald-500/10 border-emerald-500/30",
    freeTier: "Pay-as-you-go ($0.006/min for Whisper = pennies per month)",
    setupTime: "2 minutes",
    steps: [
      "Log in to platform.openai.com.",
      "Go to 'API Keys' and create a new secret key.",
      "Paste into Own Voice. A typical 30-second note costs ~$0.003.",
      "Enjoy world-class transcription and multi-language rewriting.",
    ],
    link: "https://platform.openai.com/api-keys",
  },
];

export function ByokGuideSection() {
  const [activeTab, setActiveTab] = useState<string>("gemini");
  const current = PROVIDERS.find((p) => p.id === activeTab) || PROVIDERS[0];

  return (
    <section id="byok-guide" className="py-20 px-4 sm:px-6 relative">
      <div className="max-w-5xl mx-auto space-y-10">
        {/* Header */}
        <div className="text-center space-y-3 max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            <KeyRound className="w-3.5 h-3.5" />
            Zero-Confusion Guide
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold text-white tracking-tight">
            Never used an API key before? It takes 60 seconds.
          </h2>
          <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed">
            You do not need to be a developer. We guide you through getting a free, lifetime key from Google in 3 clicks with no credit card required.
          </p>
        </div>

        {/* Provider Tabs Container */}
        <div className="rounded-3xl bg-[#0c101c]/95 border border-white/10 p-6 sm:p-10 shadow-2xl space-y-8">
          {/* Tabs */}
          <div className="flex items-center justify-center gap-2 flex-wrap border-b border-white/[0.08] pb-6">
            {PROVIDERS.map((prov) => {
              const active = prov.id === activeTab;
              return (
                <button
                  key={prov.id}
                  type="button"
                  onClick={() => setActiveTab(prov.id)}
                  className={`px-5 py-2.5 rounded-full text-xs font-semibold transition-all ${
                    active
                      ? "bg-blue-600 text-white shadow-md shadow-blue-500/25 border border-blue-400/40"
                      : "bg-zinc-900/80 text-zinc-400 hover:text-white border border-white/5"
                  }`}
                >
                  {prov.name}
                </button>
              );
            })}
          </div>

          {/* Tab Content */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
            {/* Left: Step By Step */}
            <div className="lg:col-span-7 space-y-6">
              <div className="flex items-center gap-3">
                <span className={`text-[11px] font-mono px-3 py-1 rounded-full border font-semibold ${current.tagColor}`}>
                  {current.tag}
                </span>
                <span className="text-xs text-zinc-400">
                  Setup time: <strong className="text-white">{current.setupTime}</strong>
                </span>
              </div>

              <div className="space-y-3">
                {current.steps.map((step, idx) => (
                  <div
                    key={idx}
                    className="p-3.5 rounded-xl bg-zinc-900/60 border border-white/5 flex items-start gap-3 text-xs text-zinc-300"
                  >
                    <span className="w-5 h-5 rounded-full bg-blue-600/20 text-blue-400 font-bold font-mono flex items-center justify-center flex-shrink-0 mt-0.5 text-[11px]">
                      {idx + 1}
                    </span>
                    <span className="leading-relaxed">{step}</span>
                  </div>
                ))}
              </div>

              <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                <a
                  href={current.link}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-5 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-95"
                >
                  <span>Open {current.name} Key Portal</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>

            {/* Right: Security & Privacy Vault Graphic */}
            <div className="lg:col-span-5 p-6 rounded-2xl bg-black/60 border border-white/[0.08] space-y-5">
              <div className="flex items-center gap-2 text-emerald-400 text-xs font-bold uppercase tracking-wider">
                <ShieldCheck className="w-4 h-4" />
                <span>The Privacy Advantage</span>
              </div>

              <h4 className="text-base font-bold text-white leading-snug">
                Your keys & audio never touch our servers.
              </h4>

              <p className="text-xs text-zinc-400 leading-relaxed">
                When you use Own Voice, your audio communicates directly between your computer and Google/OpenAI using military-grade encryption.
              </p>

              <ul className="space-y-2.5 text-xs text-zinc-300 pt-1">
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Stored locally with AES-256 encryption</span>
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Zero intermediate retention of your audio</span>
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>You can revoke or rotate your keys anytime</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
