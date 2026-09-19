"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowRight, Check, KeyRound, Shield, Zap, Sparkles } from "lucide-react";

export function ByokShowcase() {
  const [testKey, setTestKey] = useState("sk-proj-91A8...4F21");
  const [tested, setTested] = useState(false);

  const handleTest = () => {
    setTested(true);
    setTimeout(() => setTested(false), 3000);
  };

  return (
    <section className="py-20 px-4 sm:px-6 relative">
      <div className="max-w-6xl mx-auto rounded-[32px] bg-gradient-to-br from-[#0d121f] via-[#090c16] to-[#06080e] text-white p-8 sm:p-12 lg:p-16 border border-blue-500/30 shadow-[0_25px_70px_rgba(0,0,0,0.9),0_0_50px_rgba(59,130,246,0.15)] relative overflow-hidden backdrop-blur-2xl">
        {/* Soft radial blue lighting */}
        <div className="absolute top-0 right-1/4 w-[500px] h-[300px] bg-blue-500/15 rounded-full blur-[120px] pointer-events-none" />
        <div className="absolute -bottom-20 -left-20 w-80 h-80 bg-indigo-600/15 rounded-full blur-[100px] pointer-events-none" />

        {/* Floating handwritten note in top right */}
        <div className="absolute top-8 right-8 z-20 hidden md:flex items-center gap-2 transform rotate-[-4deg]">
          <span className="font-handwriting text-2xl sm:text-3xl text-sky-300 font-bold tracking-wide filter drop-shadow-[0_0_8px_rgba(56,189,248,0.5)]">
            Bring your own key
          </span>
          <svg
            className="w-7 h-7 text-sky-400 transform translate-y-1.5"
            viewBox="0 0 50 50"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
          >
            <path d="M10 15 C 25 10, 35 25, 40 40 M 35 35 L 40 40 L 45 32" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center relative z-10">
          {/* Left Column: Copy & CTA */}
          <div className="lg:col-span-6 space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-950/80 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
              <KeyRound className="w-3.5 h-3.5" />
              <span>BYOK Architecture</span>
            </div>

            <h2 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-white leading-[1.08]">
              Your AI. Your keys.<br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-sky-300 to-indigo-300">
                Your choice.
              </span>
            </h2>

            <p className="text-base sm:text-lg text-zinc-300 leading-relaxed max-w-lg font-normal">
              Connect your own API keys directly from the providers you trust. You control the model, zero middleman markups, and your data never trains public models.
            </p>

            <div className="flex items-center gap-4 pt-2">
              <Link
                href="/docs/connecting-api-key"
                className="inline-flex items-center gap-2 px-7 py-3.5 rounded-full bg-white hover:bg-zinc-200 text-black font-semibold text-sm transition-all shadow-xl active:scale-95"
              >
                Explore providers
                <ArrowRight className="w-4 h-4" />
              </Link>
              <div className="text-xs text-zinc-400 font-mono flex items-center gap-2">
                <Shield className="w-4 h-4 text-emerald-400" />
                <span>AES-256 Encrypted</span>
              </div>
            </div>
          </div>

          {/* Right Column: 3D Provider Badges + Interactive API Key Card */}
          <div className="lg:col-span-6 flex flex-col items-center lg:items-end justify-center relative space-y-5">
            {/* Top Row: 3D Provider Badges */}
            <div className="flex items-center gap-3 sm:gap-4 z-10">
              {/* OpenAI Badge */}
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl bg-gradient-to-b from-[#182030] to-[#0e1422] p-3.5 border border-blue-500/25 shadow-[0_10px_25px_rgba(0,0,0,0.6),0_0_15px_rgba(59,130,246,0.15)] flex flex-col items-center justify-between text-center transform hover:-translate-y-1.5 transition-all">
                <div className="w-9 h-9 rounded-xl bg-white/10 flex items-center justify-center text-white">
                  <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                    <path d="M22.28 10.37a5.58 5.58 0 0 0-.47-4.52 5.67 5.67 0 0 0-4.9-2.73c-.3 0-.6.03-.89.09A5.62 5.62 0 0 0 11.5 1a5.67 5.67 0 0 0-5.18 3.39 5.56 5.56 0 0 0-3.6 2.6 5.64 5.64 0 0 0 .52 6.55 5.58 5.58 0 0 0 .47 4.52 5.67 5.67 0 0 0 4.9 2.73c.3 0 .6-.03.89-.09A5.62 5.62 0 0 0 13.5 23a5.67 5.67 0 0 0 5.18-3.39 5.56 5.56 0 0 0 3.6-2.6 5.64 5.64 0 0 0-.52-6.55z" />
                  </svg>
                </div>
                <div>
                  <div className="text-xs font-bold text-white">OpenAI</div>
                  <div className="text-[9px] text-blue-400 font-mono">Whisper+4o</div>
                </div>
              </div>

              {/* Anthropic Badge */}
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl bg-gradient-to-b from-[#251e18] to-[#14100c] p-3.5 border border-amber-500/25 shadow-[0_10px_25px_rgba(0,0,0,0.6),0_0_15px_rgba(245,158,11,0.15)] flex flex-col items-center justify-between text-center transform hover:-translate-y-1.5 transition-all">
                <div className="w-9 h-9 rounded-xl bg-amber-500/20 flex items-center justify-center text-amber-300">
                  <span className="font-black text-sm">AI</span>
                </div>
                <div>
                  <div className="text-xs font-bold text-white">Anthropic</div>
                  <div className="text-[9px] text-amber-400 font-mono">Claude 3.5</div>
                </div>
              </div>

              {/* Google Badge */}
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl bg-gradient-to-b from-[#182030] to-[#0e1422] p-3.5 border border-sky-500/25 shadow-[0_10px_25px_rgba(0,0,0,0.6),0_0_15px_rgba(14,165,233,0.15)] flex flex-col items-center justify-between text-center transform hover:-translate-y-1.5 transition-all">
                <div className="w-9 h-9 rounded-xl bg-white/10 flex items-center justify-center">
                  <span className="font-black text-lg bg-clip-text text-transparent bg-gradient-to-r from-blue-400 via-rose-400 to-amber-300">
                    G
                  </span>
                </div>
                <div>
                  <div className="text-xs font-bold text-white">Google</div>
                  <div className="text-[9px] text-sky-400 font-mono">Gemini 2.5</div>
                </div>
              </div>
            </div>

            {/* Bottom Card: Frosted Glass Interactive Key Card */}
            <div className="w-full max-w-sm rounded-2xl bg-zinc-900/90 backdrop-blur-2xl border border-white/[0.12] p-5 shadow-2xl space-y-3">
              <div className="flex items-center justify-between text-xs font-semibold text-white">
                <span className="flex items-center gap-2 text-zinc-300">
                  <KeyRound className="w-4 h-4 text-blue-400" />
                  Connected API Key
                </span>
                <span className="text-[10px] text-emerald-400 font-mono flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  Verified
                </span>
              </div>

              <div className="flex items-center justify-between bg-black/60 border border-white/10 rounded-xl px-3.5 py-2.5 text-xs font-mono text-zinc-300 shadow-inner">
                <span className="tracking-wider text-zinc-200">{testKey}</span>
                <button
                  type="button"
                  onClick={handleTest}
                  className="px-2.5 py-1 rounded-lg bg-blue-600/30 hover:bg-blue-600/50 text-blue-300 text-[10px] font-semibold border border-blue-500/30 transition-colors flex items-center gap-1"
                >
                  {tested ? (
                    <>
                      <Check className="w-3 h-3 text-emerald-400" />
                      Valid!
                    </>
                  ) : (
                    "Test Key"
                  )}
                </button>
              </div>

              <div className="flex items-center justify-between text-[11px] text-zinc-500 font-mono pt-1">
                <span>Inference Rate: ~\$0.001 / note</span>
                <span className="text-zinc-400">Zero SaaS markup</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
