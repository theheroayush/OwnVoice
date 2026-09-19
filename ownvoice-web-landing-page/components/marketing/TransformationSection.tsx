"use client";

import { Activity } from "lucide-react";

export function TransformationSection() {
  return (
    <section id="how-it-works" className="py-24 px-4 sm:px-6 relative overflow-hidden">
      {/* Subtle radial ambient glow */}
      <div className="absolute top-1/2 -left-20 w-80 h-80 bg-blue-600/15 rounded-full blur-[100px] pointer-events-none" />

      <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
        {/* Left Side: Staggered Floating Cards */}
        <div className="lg:col-span-6 relative space-y-4 max-w-lg mx-auto lg:mx-0 w-full">
          {/* Card 1: Raw Speech */}
          <div className="rounded-2xl border border-white/[0.08] bg-[#0f131c]/90 p-5 shadow-xl backdrop-blur-md transform transition-all duration-300 hover:translate-x-1">
            <div className="flex items-center justify-between text-xs text-zinc-400 mb-2">
              <span className="font-semibold text-zinc-300 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping" />
                Raw speech
              </span>
              <span className="text-[11px] text-zinc-500 font-mono">Just now, 12 sec</span>
            </div>
            <p className="text-sm sm:text-base text-zinc-300 font-serif italic leading-relaxed">
              &ldquo;okay so write an email to the client...&rdquo;
            </p>
          </div>

          {/* Card 2: AI Understands */}
          <div className="rounded-2xl border border-white/[0.08] bg-[#121722]/95 p-5 shadow-xl backdrop-blur-md ml-4 sm:ml-8 transform transition-all duration-300 hover:translate-x-1">
            <div className="text-xs font-semibold text-zinc-300 mb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-blue-400" />
              AI understands
            </div>
            <p className="text-sm text-zinc-400 font-mono flex items-center gap-2">
              <span className="text-blue-400">Detecting intent...</span> Formatting...
            </p>
          </div>

          {/* Card 3: Finished Output */}
          <div className="flex items-start gap-3 ml-2 sm:ml-4">
            {/* Blue glowing audio icon container */}
            <div className="w-12 h-12 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 shadow-[0_0_15px_rgba(59,130,246,0.3)] flex-shrink-0 mt-2">
              <Activity className="w-6 h-6" />
            </div>

            {/* Finished Output Card */}
            <div className="flex-1 rounded-2xl border border-blue-500/25 bg-[#0f1422]/95 p-5 shadow-[0_15px_35px_rgba(0,0,0,0.6)] backdrop-blur-md">
              <div className="flex items-center justify-between text-xs font-semibold text-white mb-2">
                <span className="flex items-center gap-1.5 text-blue-300">
                  Finished output
                </span>
                <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20">
                  Ready to send
                </span>
              </div>
              <div className="space-y-1.5 text-xs sm:text-sm text-zinc-300">
                <p className="font-semibold text-white">Subject: Project Timeline Update</p>
                <p className="text-zinc-400">Hi [Client Name],</p>
                <p className="text-zinc-300 leading-relaxed">
                  I wanted to let you know that we&apos;ll need approximately two additional days to complete the integration...
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side: Step-by-Step Explanation */}
        <div className="lg:col-span-6 space-y-6 lg:pl-6 text-left">
          <div className="inline-flex items-center px-3 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            How it works
          </div>

          <h2 className="text-4xl sm:text-5xl font-bold tracking-tight text-white leading-[1.12]">
            From thought<br />
            to <span className="font-serif italic font-normal text-white">finished</span> work.<br />
            In <span className="text-blue-500">seconds.</span>
          </h2>

          <p className="text-base text-zinc-400 leading-relaxed max-w-lg">
            Speak naturally, and let Own Voice handle the rest. Transcribes, understands and turns your speech into useful output.
          </p>

          <div className="space-y-5 pt-2">
            {/* Step 01 */}
            <div className="flex items-start gap-4">
              <div className="w-8 h-8 rounded-full bg-blue-600/20 border border-blue-500/40 text-blue-400 font-bold text-xs flex items-center justify-center flex-shrink-0 mt-0.5 shadow-[0_0_10px_rgba(59,130,246,0.2)]">
                01
              </div>
              <div className="space-y-0.5">
                <h3 className="text-sm font-semibold text-white">Speak</h3>
                <p className="text-xs sm:text-sm text-zinc-400">
                  Talk naturally. No need to be perfect.
                </p>
              </div>
            </div>

            {/* Step 02 */}
            <div className="flex items-start gap-4">
              <div className="w-8 h-8 rounded-full bg-blue-600/20 border border-blue-500/40 text-blue-400 font-bold text-xs flex items-center justify-center flex-shrink-0 mt-0.5 shadow-[0_0_10px_rgba(59,130,246,0.2)]">
                02
              </div>
              <div className="space-y-0.5">
                <h3 className="text-sm font-semibold text-white">AI understands</h3>
                <p className="text-xs sm:text-sm text-zinc-400">
                  Transcribes and improves your speech with AI.
                </p>
              </div>
            </div>

            {/* Step 03 */}
            <div className="flex items-start gap-4">
              <div className="w-8 h-8 rounded-full bg-blue-600/20 border border-blue-500/40 text-blue-400 font-bold text-xs flex items-center justify-center flex-shrink-0 mt-0.5 shadow-[0_0_10px_rgba(59,130,246,0.2)]">
                03
              </div>
              <div className="space-y-0.5">
                <h3 className="text-sm font-semibold text-white">Get finished work</h3>
                <p className="text-xs sm:text-sm text-zinc-400">
                  Copy, edit or use it anywhere.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
