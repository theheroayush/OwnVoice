"use client";

import { Mail, MessageSquare, FileText, Code, Lightbulb, PenTool, Terminal, Linkedin } from "lucide-react";

export function UseCasesSection() {
  const tags = [
    { label: "Emails", icon: Mail },
    { label: "Messages", icon: MessageSquare },
    { label: "Notes", icon: FileText },
    { label: "Content", icon: PenTool },
    { label: "Prompts", icon: Terminal },
    { label: "Code", icon: Code },
    { label: "Ideas", icon: Lightbulb },
    { label: "and more...", icon: null },
  ];

  return (
    <section id="use-cases" className="py-24 px-4 sm:px-6 relative overflow-hidden">
      {/* Subtle background glow */}
      <div className="absolute top-1/2 -left-10 w-72 h-72 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
        {/* Left Side: 3D Translucent Floating Cubes */}
        <div className="lg:col-span-6 relative flex items-center justify-center py-8">
          <div className="relative w-72 sm:w-80 h-72 sm:h-80 flex items-center justify-center">
            {/* Ambient inner glow */}
            <div className="absolute inset-0 bg-blue-500/10 rounded-full blur-2xl pointer-events-none" />

            {/* Cube 1: Email (Top Left) */}
            <div className="absolute top-4 left-6 w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-br from-blue-500/30 to-blue-900/40 backdrop-blur-xl border border-blue-400/40 shadow-[0_10px_25px_rgba(59,130,246,0.3)] flex items-center justify-center text-blue-300 transform -rotate-12 hover:scale-110 transition-transform duration-300">
              <Mail className="w-8 h-8 sm:w-9 sm:h-9" />
            </div>

            {/* Cube 2: Document / Notes (Center Top) */}
            <div className="absolute top-8 right-10 w-18 h-18 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-br from-slate-600/30 to-zinc-900/50 backdrop-blur-xl border border-white/20 shadow-[0_10px_25px_rgba(0,0,0,0.5)] flex items-center justify-center text-zinc-200 transform rotate-6 hover:scale-110 transition-transform duration-300">
              <FileText className="w-8 h-8 sm:w-9 sm:h-9" />
            </div>

            {/* Cube 3: LinkedIn (Center Right) */}
            <div className="absolute top-28 right-0 w-16 h-16 sm:w-18 sm:h-18 rounded-2xl bg-gradient-to-br from-blue-600/40 to-sky-900/50 backdrop-blur-xl border border-blue-400/30 shadow-[0_10px_25px_rgba(14,165,233,0.3)] flex items-center justify-center text-sky-200 transform rotate-12 hover:scale-110 transition-transform duration-300">
              <Linkedin className="w-7 h-7 sm:w-8 sm:h-8" />
            </div>

            {/* Cube 4: Code (Bottom Left) */}
            <div className="absolute bottom-6 left-10 w-18 h-18 sm:w-22 sm:h-22 rounded-2xl bg-gradient-to-br from-indigo-500/30 to-slate-900/60 backdrop-blur-xl border border-indigo-400/30 shadow-[0_10px_25px_rgba(99,102,241,0.3)] flex items-center justify-center text-indigo-300 transform -rotate-6 hover:scale-110 transition-transform duration-300">
              <Code className="w-8 h-8 sm:w-10 sm:h-10" />
            </div>

            {/* Cube 5: Finished Writing (Bottom Right) */}
            <div className="absolute bottom-4 right-14 w-16 h-16 sm:w-18 sm:h-18 rounded-2xl bg-gradient-to-br from-zinc-700/30 to-zinc-900/50 backdrop-blur-xl border border-white/20 shadow-[0_10px_25px_rgba(0,0,0,0.5)] flex items-center justify-center text-zinc-300 transform rotate-12 hover:scale-110 transition-transform duration-300">
              <PenTool className="w-7 h-7 sm:w-8 sm:h-8" />
            </div>
          </div>
        </div>

        {/* Right Side: Copy & Interactive Pills */}
        <div className="lg:col-span-6 space-y-6 lg:pl-6 text-left">
          <div className="inline-flex items-center px-3 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            Use cases
          </div>

          <h2 className="text-4xl sm:text-5xl font-bold tracking-tight text-white leading-[1.12]">
            Built for how<br />
            you actually work.
          </h2>

          <p className="text-base text-zinc-400 leading-relaxed max-w-lg">
            From emails to ideas to code, Own Voice adapts to your workflow.
          </p>

          {/* Interactive Tag Pills */}
          <div className="flex flex-wrap gap-2.5 pt-3">
            {tags.map((tag, idx) => (
              <div
                key={idx}
                className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-zinc-900/90 hover:bg-zinc-800 border border-white/[0.08] hover:border-blue-500/40 text-xs font-medium text-zinc-300 transition-all cursor-pointer shadow-sm hover:text-white"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-blue-500 shadow-[0_0_6px_rgba(59,130,246,0.8)]" />
                <span>{tag.label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
