"use client";

import { Mic, Sparkles, SlidersHorizontal, Search, Shield, FileText, ArrowUpRight } from "lucide-react";

export function FeaturesGrid() {
  const features = [
    {
      icon: Mic,
      title: "Voice to text",
      description: "Accurate, near-zero latency transcription from natural speech even with accents and background noise.",
      tag: "150+ WPM",
      gradient: "from-blue-500/20 to-sky-500/5",
    },
    {
      icon: Sparkles,
      title: "AI cleanup",
      description: "Automatically eliminates 'umm', 'aah', stuttering, and self-corrections without losing your natural voice.",
      tag: "Zero Fillers",
      gradient: "from-indigo-500/20 to-purple-500/5",
    },
    {
      icon: FileText,
      title: "Custom instructions",
      description: "Teach Own Voice your writing style, vocabulary, technical terms, and executive communication templates.",
      tag: "Personalized",
      gradient: "from-cyan-500/20 to-blue-500/5",
    },
    {
      icon: SlidersHorizontal,
      title: "Model control",
      description: "Switch seamlessly between GPT-4o, Claude 3.5 Sonnet, Gemini 2.5 Flash, or ultra-fast Groq Llama 3.3.",
      tag: "Multi-Model",
      gradient: "from-purple-500/20 to-pink-500/5",
    },
    {
      icon: Search,
      title: "History & search",
      description: "Instant full-text search across every voice note, draft, and transcription you have ever created.",
      tag: "Instant Retrieval",
      gradient: "from-blue-500/20 to-emerald-500/5",
    },
    {
      icon: Shield,
      title: "Privacy focused",
      description: "Your keys, your data, your rules. Audio is processed ephemerally in-memory and discarded immediately.",
      tag: "AES-256 Encrypted",
      gradient: "from-emerald-500/20 to-teal-500/5",
    },
  ];

  return (
    <section id="features" className="py-20 px-4 sm:px-6 relative">
      <div className="max-w-6xl mx-auto space-y-12">
        <div className="text-center space-y-3 max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            Built for execution
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight text-white">
            Engineered for pure speed.
          </h2>
          <p className="text-sm sm:text-base text-zinc-400">
            Every feature is designed to reduce friction between your raw thoughts and finished, ready-to-publish work.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {features.map((feat, idx) => {
            const Icon = feat.icon;
            return (
              <div
                key={idx}
                className="relative rounded-2xl border border-white/[0.08] bg-[#0a0d15]/90 p-7 space-y-4 hover:border-blue-500/40 hover:bg-[#0d111c] transition-all duration-300 group shadow-xl flex flex-col justify-between"
              >
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="w-11 h-11 rounded-xl bg-blue-600/15 border border-blue-500/25 flex items-center justify-center text-blue-400 group-hover:bg-blue-600/25 transition-colors shadow-[0_0_12px_rgba(59,130,246,0.2)]">
                      <Icon className="w-5 h-5" />
                    </div>
                    <span className="text-[10px] font-mono uppercase tracking-wider px-2.5 py-1 rounded-full bg-white/[0.05] text-zinc-400 border border-white/[0.08]">
                      {feat.tag}
                    </span>
                  </div>

                  <h3 className="text-lg font-semibold text-white tracking-tight group-hover:text-blue-300 transition-colors">
                    {feat.title}
                  </h3>

                  <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed font-normal">
                    {feat.description}
                  </p>
                </div>

                <div className="pt-2 flex items-center gap-1 text-xs text-blue-400/80 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>Learn more</span>
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
