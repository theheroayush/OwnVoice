"use client";

import React, { useState, useEffect } from "react";
import { Search, Sparkles, Mic, Copy, Check, Trash2, Calendar, FileText, X, ArrowUpDown } from "lucide-react";
import { useAppStore } from "@/lib/store";

interface GenerationItem {
  id: string;
  title: string;
  intent: string;
  targetTone: string;
  rawInput: string;
  outputText: string;
  wordCount: number;
  providerUsed: string;
  modelUsed: string;
  createdAt: string;
  versions?: Array<{
    id: string;
    actionTaken: string;
    outputText: string;
    createdAt: string;
  }>;
}

export default function HistoryPage() {
  const { showToast } = useAppStore();
  const [generations, setGenerations] = useState<GenerationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedIntent, setSelectedIntent] = useState<string>("ALL");
  const [activeModalItem, setActiveModalItem] = useState<GenerationItem | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (searchQuery) params.set("q", searchQuery);
      if (selectedIntent !== "ALL") params.set("intent", selectedIntent);

      const res = await fetch(`/api/history?${params.toString()}`);
      const data = await res.json();
      if (data.success) {
        setGenerations(data.generations || []);
      }
    } catch (err) {
      console.error("History fetch error:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, [searchQuery, selectedIntent]);

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    showToast("Copied to clipboard!", "success");
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to delete this voice note?")) return;
    try {
      const res = await fetch(`/api/generations/${id}`, { method: "DELETE" });
      if (res.ok) {
        setGenerations((prev) => prev.filter((g) => g.id !== id));
        if (activeModalItem?.id === id) setActiveModalItem(null);
        showToast("Voice note deleted.", "info");
      }
    } catch (err) {
      showToast("Failed to delete.", "error");
    }
  };

  // Group by date
  const groupByDate = (items: GenerationItem[]) => {
    const today: GenerationItem[] = [];
    const yesterday: GenerationItem[] = [];
    const older: GenerationItem[] = [];

    const now = new Date();
    const todayStr = now.toDateString();

    const yest = new Date();
    yest.setDate(now.getDate() - 1);
    const yestStr = yest.toDateString();

    items.forEach((item) => {
      const itemDateStr = new Date(item.createdAt).toDateString();
      if (itemDateStr === todayStr) {
        today.push(item);
      } else if (itemDateStr === yestStr) {
        yesterday.push(item);
      } else {
        older.push(item);
      }
    });

    return { today, yesterday, older };
  };

  const groups = groupByDate(generations);

  return (
    <div className="space-y-6">
      {/* Header & Search */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">Voice History</h1>
          <p className="text-xs sm:text-sm text-zinc-400">
            Search, review, and copy all your spoken thoughts and generated outputs.
          </p>
        </div>

        {/* Search Bar */}
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search transcripts, titles..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl bg-zinc-900 border border-white/10 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500 transition-colors"
          />
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
        {["ALL", "CLEAN", "EMAIL", "MESSAGE", "SUMMARY", "NOTE", "CODE"].map((intent) => (
          <button
            key={intent}
            onClick={() => setSelectedIntent(intent)}
            className={`px-3 py-1.5 rounded-lg font-medium transition-all ${
              selectedIntent === intent
                ? "bg-blue-600 text-white shadow-sm"
                : "bg-zinc-900/60 text-zinc-400 hover:text-white border border-white/5"
            }`}
          >
            {intent === "ALL" ? "All Formats" : intent}
          </button>
        ))}
      </div>

      {/* Empty State (PRD Section 40) */}
      {!loading && generations.length === 0 && (
        <div className="text-center py-16 space-y-3 rounded-2xl bg-zinc-900/20 border border-white/5 p-8">
          <FileText className="w-10 h-10 text-zinc-600 mx-auto" />
          <h3 className="text-base font-semibold text-zinc-300">
            {searchQuery ? "No matching voice notes found" : "Your voice history will appear here"}
          </h3>
          <p className="text-xs text-zinc-500 max-w-sm mx-auto">
            {searchQuery
              ? "Try searching for a different keyword or format."
              : "Your first thought is one click away. Head to the workspace and speak."}
          </p>
        </div>
      )}

      {/* Grouped History List */}
      <div className="space-y-6">
        {groups.today.length > 0 && (
          <div className="space-y-2.5">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-500 font-mono">Today</h3>
            <div className="space-y-2">
              {groups.today.map((item) => renderItemRow(item))}
            </div>
          </div>
        )}

        {groups.yesterday.length > 0 && (
          <div className="space-y-2.5">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-500 font-mono">Yesterday</h3>
            <div className="space-y-2">
              {groups.yesterday.map((item) => renderItemRow(item))}
            </div>
          </div>
        )}

        {groups.older.length > 0 && (
          <div className="space-y-2.5">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-500 font-mono">Older</h3>
            <div className="space-y-2">
              {groups.older.map((item) => renderItemRow(item))}
            </div>
          </div>
        )}
      </div>

      {/* Detail Modal */}
      {activeModalItem && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-2xl rounded-2xl bg-[#121215] border border-white/10 shadow-2xl p-6 space-y-5 animate-scale-in max-h-[90vh] overflow-y-auto">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-lg font-bold text-white tracking-tight">{activeModalItem.title}</h3>
                <span className="text-xs font-mono text-zinc-500">
                  {new Date(activeModalItem.createdAt).toLocaleString()} • {activeModalItem.intent} • {activeModalItem.wordCount} words
                </span>
              </div>
              <button
                onClick={() => setActiveModalItem(null)}
                className="text-zinc-400 hover:text-white p-1 rounded-lg hover:bg-zinc-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Original Speech */}
            <div className="space-y-1.5">
              <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider flex items-center gap-1.5">
                <Mic className="w-3.5 h-3.5 text-zinc-500" />
                Original Voice Input
              </span>
              <div className="p-3.5 rounded-xl bg-zinc-950/60 border border-white/5 text-xs text-zinc-400 font-mono italic">
                "{activeModalItem.rawInput}"
              </div>
            </div>

            {/* Output */}
            <div className="space-y-1.5">
              <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
                Finished Output
              </span>
              <div className="p-4 rounded-xl bg-zinc-900/60 border border-white/10 text-sm text-zinc-100 whitespace-pre-line leading-relaxed">
                {activeModalItem.outputText}
              </div>
            </div>

            {/* Modal Actions */}
            <div className="flex items-center justify-between pt-3 border-t border-white/5">
              <button
                onClick={() => handleDelete(activeModalItem.id)}
                className="flex items-center gap-1.5 text-xs text-rose-400 hover:text-rose-300 px-3 py-1.5 rounded-lg hover:bg-rose-500/10 transition-colors"
              >
                <Trash2 className="w-3.5 h-3.5" />
                Delete
              </button>

              <button
                onClick={() => handleCopy(activeModalItem.outputText, activeModalItem.id)}
                className="flex items-center gap-1.5 text-xs font-semibold px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white transition-all shadow-sm active:scale-95"
              >
                {copiedId === activeModalItem.id ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                {copiedId === activeModalItem.id ? "Copied!" : "Copy Output"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );

  function renderItemRow(item: GenerationItem) {
    return (
      <div
        key={item.id}
        onClick={() => setActiveModalItem(item)}
        className="flex items-center justify-between p-4 rounded-xl bg-zinc-900/40 border border-white/5 hover:border-white/15 hover:bg-zinc-900/80 transition-all cursor-pointer group"
      >
        <div className="truncate pr-4 space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-zinc-200 group-hover:text-blue-400 transition-colors">
              {item.title}
            </span>
            <span className="text-[10px] font-mono uppercase px-2 py-0.5 rounded-full bg-zinc-800 text-zinc-400">
              {item.intent}
            </span>
          </div>
          <p className="text-xs text-zinc-400 line-clamp-1">
            {item.outputText}
          </p>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <span className="text-xs font-mono text-zinc-500">
            {new Date(item.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
          </span>
          <button
            onClick={(e) => {
              e.stopPropagation();
              handleCopy(item.outputText, item.id);
            }}
            className="p-2 rounded-lg bg-zinc-800/80 hover:bg-zinc-700 text-zinc-300 hover:text-white transition-colors"
            title="Copy output"
          >
            {copiedId === item.id ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>
    );
  }
}
