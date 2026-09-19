"use client";

import React, { useState } from "react";
import { useAppStore } from "@/lib/store";
import { QuickActions } from "./QuickActions";
import { Sparkles, Mic, History, Check, Copy } from "lucide-react";

export function OutputDisplay() {
  const { currentGeneration, rawTranscript } = useAppStore();
  const [activeTab, setActiveTab] = useState<"finished" | "original">("finished");
  const [copied, setCopied] = useState(false);

  if (!currentGeneration && !rawTranscript) return null;

  const copyText = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="w-full max-w-3xl mx-auto rounded-2xl bg-[#121215] border border-white/10 shadow-2xl p-5 sm:p-6 space-y-4 animate-fade-in">
      {/* Header Info */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div>
          <h3 className="text-base font-semibold text-white tracking-tight flex items-center gap-2">
            {currentGeneration?.title || "Voice Dictation"}
            <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded-full bg-blue-950/60 text-blue-400 border border-blue-800/40">
              {currentGeneration?.intent || "CLEAN"}
            </span>
          </h3>
          <span className="text-xs text-zinc-500 font-mono">
            {currentGeneration?.providerUsed?.toUpperCase()} • {currentGeneration?.modelUsed} • {currentGeneration?.wordCount || 0} words
          </span>
        </div>

        {/* View Switcher */}
        <div className="inline-flex p-1 rounded-lg bg-zinc-900 border border-white/5 text-xs font-medium">
          <button
            onClick={() => setActiveTab("finished")}
            className={`flex items-center gap-1.5 px-3 py-1 rounded-md transition-all ${
              activeTab === "finished" ? "bg-zinc-800 text-white shadow-sm" : "text-zinc-400 hover:text-white"
            }`}
          >
            <Sparkles className="w-3.5 h-3.5 text-blue-400" />
            <span>Finished Work</span>
          </button>
          <button
            onClick={() => setActiveTab("original")}
            className={`flex items-center gap-1.5 px-3 py-1 rounded-md transition-all ${
              activeTab === "original" ? "bg-zinc-800 text-white shadow-sm" : "text-zinc-400 hover:text-white"
            }`}
          >
            <Mic className="w-3.5 h-3.5 text-zinc-400" />
            <span>Original Voice</span>
          </button>
        </div>
      </div>

      {/* Main Text Content */}
      <div className="rounded-xl bg-zinc-950/50 border border-white/5 p-4 sm:p-5 relative min-h-[140px]">
        {activeTab === "finished" ? (
          <div className="text-sm sm:text-base text-zinc-100 whitespace-pre-line leading-relaxed font-sans select-text">
            {currentGeneration?.outputText || "Waiting for generation..."}
          </div>
        ) : (
          <div className="text-sm text-zinc-400 italic font-mono whitespace-pre-line leading-relaxed select-text">
            "{currentGeneration?.rawInput || rawTranscript || "No speech captured."}"
          </div>
        )}
      </div>

      {/* Quick Actions Component */}
      <QuickActions />
    </div>
  );
}
