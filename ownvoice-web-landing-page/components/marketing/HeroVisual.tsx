"use client";

import { useState, useEffect } from "react";
import { Mic, Clock, Star, Settings, Pause, Square, X, Sparkles } from "lucide-react";

export function HeroVisual() {
  const [seconds, setSeconds] = useState(17);
  const [isRecording, setIsRecording] = useState(true);

  useEffect(() => {
    if (!isRecording) return;
    const interval = setInterval(() => {
      setSeconds((s) => (s >= 59 ? 0 : s + 1));
    }, 1000);
    return () => clearInterval(interval);
  }, [isRecording]);

  const formattedTime = `00:${seconds.toString().padStart(2, "0")}`;

  const bars = [
    24, 38, 55, 72, 85, 60, 42, 68, 92, 100, 84, 62, 78, 95, 88, 70, 52, 75,
    90, 65, 45, 30,
  ];

  return (
    <div className="relative w-full max-w-lg lg:max-w-none mx-auto select-none">
      {/* Ambient background glow */}
      <div className="absolute -top-16 -right-16 w-80 h-80 bg-blue-600/25 rounded-full blur-[90px] pointer-events-none" />
      <div className="absolute -bottom-10 -left-10 w-72 h-72 bg-indigo-600/20 rounded-full blur-[80px] pointer-events-none" />

      {/* Floating handwritten note in top right */}
      <div className="absolute -top-8 right-4 z-20 hidden sm:flex items-center gap-1.5 text-zinc-300 transform rotate-[-5deg]">
        <span className="font-handwriting text-2xl text-blue-200 tracking-wide">
          Your ideas, but faster
        </span>
        <svg
          className="w-7 h-7 text-blue-300 transform translate-y-1.5"
          viewBox="0 0 50 50"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M10 15 C 25 10, 35 25, 40 40 M 35 35 L 40 40 L 45 32" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>

      {/* Main Desktop App Window Mockup */}
      <div className="relative z-10 rounded-2xl border border-white/[0.12] bg-[#0c0f17]/95 shadow-[0_20px_50px_rgba(0,0,0,0.8),0_0_30px_rgba(59,130,246,0.15)] backdrop-blur-2xl overflow-hidden">
        {/* Window Titlebar */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-white/[0.08] bg-[#0a0d14]/80">
          <div className="flex items-center gap-2">
            <div className="w-2.5 h-2.5 rounded-full bg-red-500/80" />
            <div className="w-2.5 h-2.5 rounded-full bg-yellow-500/80" />
            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/80" />
            <span className="text-xs font-semibold text-zinc-300 ml-2">Own Voice</span>
          </div>
          <div className="px-2.5 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-[10px] font-medium tracking-wide">
            Ready
          </div>
        </div>

        {/* Window Content */}
        <div className="grid grid-cols-12 min-h-[330px]">
          {/* Sidebar */}
          <div className="col-span-3 border-r border-white/[0.06] p-3 flex flex-col justify-between bg-[#080b11]/70">
            <div className="space-y-1.5">
              <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-blue-600/20 text-blue-400 text-xs font-medium border border-blue-500/20">
                <Mic className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Home</span>
              </div>
              <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-zinc-400 hover:text-white text-xs font-medium transition-colors">
                <Clock className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">History</span>
              </div>
              <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-zinc-400 hover:text-white text-xs font-medium transition-colors">
                <Star className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Favorites</span>
              </div>
              <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-zinc-400 hover:text-white text-xs font-medium transition-colors">
                <Settings className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Settings</span>
              </div>
            </div>

            <div className="pt-4 border-t border-white/[0.06]">
              <div className="text-[9px] text-zinc-500 font-mono">Connected:</div>
              <div className="text-[10px] font-semibold text-zinc-300 truncate">OpenAI/GPT-4o</div>
            </div>
          </div>

          {/* Main Recording Workspace Area */}
          <div className="col-span-9 p-5 flex flex-col justify-between relative bg-gradient-to-b from-transparent to-blue-950/10">
            {/* Top State Bar */}
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <div className="text-[10px] uppercase tracking-wider text-zinc-400 font-medium">Recording...</div>
                <div className="text-2xl sm:text-3xl font-bold font-mono text-white tracking-tight">
                  {formattedTime}
                </div>
              </div>
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/[0.07] border border-white/[0.1] text-zinc-300 text-[11px] font-medium">
                <Sparkles className="w-3 h-3 text-blue-400 animate-pulse" />
                <span>Thinking...</span>
              </div>
            </div>

            {/* Glowing Soundwave Bars */}
            <div className="py-6 flex items-center justify-center gap-1.5 h-24">
              {bars.map((height, i) => (
                <div
                  key={i}
                  className="w-1.5 rounded-full bg-gradient-to-t from-blue-600 via-indigo-400 to-purple-400 shadow-[0_0_8px_rgba(99,102,241,0.5)] transition-all duration-300"
                  style={{
                    height: `${isRecording ? Math.max(14, height * (0.6 + 0.4 * Math.sin((i + seconds) * 0.8))) : 10}%`,
                  }}
                />
              ))}
            </div>

            {/* Bottom Controls */}
            <div className="flex items-center justify-center gap-4 pt-1">
              <button
                type="button"
                onClick={() => setIsRecording(!isRecording)}
                className="w-8 h-8 rounded-full bg-zinc-800/80 hover:bg-zinc-700 text-zinc-300 flex items-center justify-center border border-white/10 transition-all text-xs"
                title="Pause"
              >
                <Pause className="w-3 h-3" />
              </button>

              <button
                type="button"
                onClick={() => setIsRecording(!isRecording)}
                className="w-11 h-11 rounded-full bg-gradient-to-tr from-red-600 to-rose-500 hover:from-red-500 hover:to-rose-400 text-white flex items-center justify-center shadow-[0_0_20px_rgba(239,68,68,0.5)] transition-all active:scale-95"
                title="Stop & Process"
              >
                <Square className="w-3.5 h-3.5 fill-white" />
              </button>

              <button
                type="button"
                onClick={() => setSeconds(0)}
                className="w-8 h-8 rounded-full bg-zinc-800/80 hover:bg-zinc-700 text-zinc-300 flex items-center justify-center border border-white/10 transition-all text-xs"
                title="Cancel"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Floating 3D Frosted Glass Key Widget */}
      <div className="absolute -bottom-5 -right-3 sm:-bottom-7 sm:-right-5 z-30 w-40 sm:w-44 p-3.5 rounded-2xl bg-gradient-to-b from-zinc-800/95 to-zinc-900/95 backdrop-blur-2xl border border-white/20 shadow-[0_20px_40px_rgba(0,0,0,0.7),inset_0_1px_0_rgba(255,255,255,0.25)] text-center transform hover:scale-105 transition-transform duration-300">
        <div className="flex items-center justify-center gap-1.5 mb-1">
          <span className="text-2xl sm:text-3xl font-black text-white tracking-wider">⌘</span>
          <span className="text-base sm:text-lg font-bold text-zinc-200">Space</span>
        </div>
        <p className="text-[10px] sm:text-[11px] text-zinc-400 leading-tight font-medium">
          Press <span className="text-white font-semibold">⌘ Space</span><br />To start speaking
        </p>
      </div>
    </div>
  );
}
