"use client";

import React from "react";
import { useAppStore } from "@/lib/store";
import { IntentType, ToneType } from "@/lib/providers/types";
import { Sparkles, Mail, MessageSquare, ListFilter, FileText, Code2, Mic } from "lucide-react";

const INTENTS: Array<{ id: IntentType; label: string; icon: any }> = [
  { id: "CLEAN", label: "Clean Polish", icon: Sparkles },
  { id: "EMAIL", label: "Email", icon: Mail },
  { id: "MESSAGE", label: "Message", icon: MessageSquare },
  { id: "SUMMARY", label: "Summary", icon: ListFilter },
  { id: "NOTE", label: "Note", icon: FileText },
  { id: "CODE", label: "Code", icon: Code2 },
  { id: "TRANSCRIBE", label: "Raw Dictate", icon: Mic },
];

const TONES: Array<{ id: ToneType; label: string }> = [
  { id: "NATURAL", label: "Natural" },
  { id: "PROFESSIONAL", label: "Professional" },
  { id: "CASUAL", label: "Casual" },
  { id: "CONCISE", label: "Concise" },
  { id: "EXPANDED", label: "Detailed" },
];

export function IntentSelector() {
  const { activeIntent, setActiveIntent, activeTone, setActiveTone } = useAppStore();

  return (
    <div className="w-full max-w-2xl mx-auto space-y-3">
      {/* Intent Selector */}
      <div className="flex items-center justify-center gap-1.5 flex-wrap">
        {INTENTS.map((item) => {
          const Icon = item.icon;
          const isSelected = activeIntent === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveIntent(item.id)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                isSelected
                  ? "bg-blue-600 text-white shadow-sm border border-blue-500"
                  : "bg-zinc-900/60 text-zinc-400 hover:text-white border border-white/5 hover:border-white/10"
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tone Selector */}
      <div className="flex items-center justify-center gap-1">
        <span className="text-[11px] font-mono text-zinc-500 mr-1.5">Tone:</span>
        {TONES.map((tone) => {
          const isSelected = activeTone === tone.id;
          return (
            <button
              key={tone.id}
              onClick={() => setActiveTone(tone.id)}
              className={`px-2 py-0.5 rounded text-[11px] transition-colors ${
                isSelected
                  ? "text-blue-400 font-semibold bg-blue-950/40 border border-blue-800/40"
                  : "text-zinc-500 hover:text-zinc-300"
              }`}
            >
              {tone.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
