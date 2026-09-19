"use client";

import React, { useState } from "react";
import { useAppStore } from "@/lib/store";
import { Copy, Check, Sparkles, Wand2, Minimize2, Maximize2, Briefcase, Smile, ListCollapse, RotateCcw } from "lucide-react";

export function QuickActions() {
  const { currentGeneration, setCurrentGeneration, showToast } = useAppStore();
  const [loadingAction, setLoadingAction] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  if (!currentGeneration) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(currentGeneration.outputText);
    setCopied(true);
    showToast("Copied to clipboard!", "success");
    setTimeout(() => setCopied(false), 2000);
  };

  const handleQuickAction = async (action: string) => {
    setLoadingAction(action);
    try {
      const res = await fetch(`/api/generations/${currentGeneration.id}/quick-action`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || "Action failed.");
      }

      setCurrentGeneration(data.generation);
      showToast(`Refined: ${action.toLowerCase()}`, "success");
    } catch (err: any) {
      showToast(err.message || "Failed to update generation", "error");
    } finally {
      setLoadingAction(null);
    }
  };

  const actions = [
    { id: "PROFESSIONAL", label: "Professional", icon: Briefcase },
    { id: "CASUAL", label: "Casual", icon: Smile },
    { id: "SHORTEN", label: "Shorten", icon: Minimize2 },
    { id: "EXPAND", label: "Expand", icon: Maximize2 },
    { id: "SUMMARIZE", label: "Summarize", icon: ListCollapse },
    { id: "TRY_AGAIN", label: "Try again", icon: RotateCcw },
  ];

  return (
    <div className="w-full flex items-center justify-between gap-2 flex-wrap pt-3 border-t border-white/5">
      <div className="flex items-center gap-1.5 flex-wrap">
        {actions.map((act) => {
          const Icon = act.icon;
          const isLoading = loadingAction === act.id;
          return (
            <button
              key={act.id}
              onClick={() => handleQuickAction(act.id)}
              disabled={loadingAction !== null}
              className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium bg-zinc-900/80 hover:bg-zinc-800 text-zinc-300 hover:text-white border border-white/5 hover:border-white/15 transition-all disabled:opacity-50"
            >
              <Icon className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`} />
              <span>{act.label}</span>
            </button>
          );
        })}
      </div>

      <button
        onClick={handleCopy}
        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white transition-all shadow-sm active:scale-95 ml-auto"
      >
        {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
        <span>{copied ? "Copied!" : "Copy Output"}</span>
      </button>
    </div>
  );
}
