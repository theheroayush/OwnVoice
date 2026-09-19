"use client";

import React, { useState, useEffect } from "react";
import { KeyRound, ShieldCheck, Sparkles, Mic, Trash2, CheckCircle2, AlertCircle, RefreshCw, ExternalLink, Plus } from "lucide-react";
import { useAppStore } from "@/lib/store";

interface ProviderData {
  id: string;
  provider: string;
  keyHint: string;
  selectedModel: string;
  isActive: boolean;
  isValid: boolean;
  lastTestedAt: string | null;
}

interface CustomInstruction {
  id: string;
  title: string;
  instruction: string;
}

export default function SettingsPage() {
  const { showToast } = useAppStore();
  const [profile, setProfile] = useState<any>(null);
  const [providers, setProviders] = useState<ProviderData[]>([]);
  const [instructions, setInstructions] = useState<CustomInstruction[]>([]);
  const [loading, setLoading] = useState(true);

  // New Provider Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newProvider, setNewProvider] = useState<"openai" | "google" | "anthropic" | "groq">("openai");
  const [newApiKey, setNewApiKey] = useState("");
  const [testingConnection, setTestingConnection] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // New Custom Instruction State
  const [newInstTitle, setNewInstTitle] = useState("");
  const [newInstBody, setNewInstBody] = useState("");

  const loadData = async () => {
    try {
      const res = await fetch("/api/auth/me");
      const data = await res.json();
      if (data.success) {
        setProfile(data.user);
        setProviders(data.user.providers || []);
        setInstructions(data.user.instructions || []);
      }
    } catch (err) {
      console.error("Load settings error:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleTestAndSaveProvider = async () => {
    if (!newApiKey.trim()) {
      showToast("Please enter your API key.", "error");
      return;
    }

    setTestingConnection(true);
    setTestResult(null);

    try {
      const res = await fetch("/api/providers", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          provider: newProvider,
          apiKey: newApiKey.trim(),
        }),
      });

      const data = await res.json();

      if (!res.ok || !data.success) {
        setTestResult({ success: false, message: data.error || "Connection failed." });
        showToast(data.error || "Connection failed", "error");
      } else {
        setTestResult({ success: true, message: "Connected successfully!" });
        showToast(`Connected ${newProvider.toUpperCase()}!`, "success");
        setNewApiKey("");
        setIsModalOpen(false);
        await loadData();
      }
    } catch (err: any) {
      setTestResult({ success: false, message: err.message || "Failed to connect." });
    } finally {
      setTestingConnection(false);
    }
  };

  const handleDisconnectProvider = async (providerName: string) => {
    if (!confirm(`Disconnect ${providerName.toUpperCase()}? Your stored key will be removed.`)) return;
    try {
      const res = await fetch(`/api/providers/${providerName}`, { method: "DELETE" });
      if (res.ok) {
        showToast(`Disconnected ${providerName.toUpperCase()}`, "info");
        await loadData();
      }
    } catch (err) {
      showToast("Failed to disconnect provider.", "error");
    }
  };

  const handleAddInstruction = async () => {
    if (!newInstTitle.trim() || !newInstBody.trim()) {
      showToast("Please enter title and instruction.", "error");
      return;
    }

    try {
      const res = await fetch("/api/settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: newInstTitle.trim(),
          instruction: newInstBody.trim(),
        }),
      });

      const data = await res.json();
      if (res.ok && data.success) {
        setInstructions((prev) => [...prev, data.instruction]);
        setNewInstTitle("");
        setNewInstBody("");
        showToast("Writing style rule added!", "success");
      }
    } catch (err) {
      showToast("Failed to add instruction.", "error");
    }
  };

  const handleDeleteInstruction = async (id: string) => {
    try {
      const res = await fetch(`/api/settings/instructions/${id}`, { method: "DELETE" });
      if (res.ok) {
        setInstructions((prev) => prev.filter((i) => i.id !== id));
        showToast("Rule removed.", "info");
      }
    } catch (err) {
      showToast("Failed to delete rule.", "error");
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-10 pb-12">
      {/* Title */}
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Settings & AI Configuration</h1>
        <p className="text-xs sm:text-sm text-zinc-400">
          Manage your BYOK keys, custom writing instructions, and lifetime license.
        </p>
      </div>

      {/* Section 1: BYOK AI Providers (PRD Section 14 & 15) */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold text-white uppercase tracking-wider flex items-center gap-2">
              <KeyRound className="w-4 h-4 text-blue-400" />
              Connected AI Providers (BYOK)
            </h2>
            <p className="text-xs text-zinc-500">
              API keys are encrypted using AES-256-GCM. Raw keys are never stored or exposed.
            </p>
          </div>

          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white transition-all shadow-sm active:scale-95"
          >
            <Plus className="w-3.5 h-3.5" />
            Connect Provider
          </button>
        </div>

        {/* Providers List */}
        <div className="space-y-3">
          {providers.length === 0 ? (
            <div className="p-6 rounded-xl bg-zinc-900/30 border border-white/5 text-center space-y-2">
              <p className="text-xs text-zinc-400">No AI providers connected yet.</p>
              <button
                onClick={() => setIsModalOpen(true)}
                className="text-xs text-blue-400 hover:underline font-medium"
              >
                Connect OpenAI, Gemini, Anthropic, or Groq
              </button>
            </div>
          ) : (
            providers.map((p) => (
              <div
                key={p.id}
                className="flex items-center justify-between p-4 rounded-xl bg-zinc-900/50 border border-white/5"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-white capitalize">{p.provider}</span>
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-950/60 text-emerald-400 border border-emerald-500/20 font-medium">
                      Connected
                    </span>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-zinc-500 font-mono">
                    <span>Key: {p.keyHint}</span>
                    <span>Model: {p.selectedModel}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleDisconnectProvider(p.provider)}
                    className="text-xs text-zinc-400 hover:text-rose-400 p-2 rounded-lg hover:bg-zinc-800 transition-colors"
                    title="Disconnect key"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      {/* Section 2: AI Behavior & Custom Instructions (PRD Section 19) */}
      <section className="space-y-4">
        <div>
          <h2 className="text-sm font-semibold text-white uppercase tracking-wider flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-emerald-400" />
            AI Behavior & Custom Writing Style
          </h2>
          <p className="text-xs text-zinc-500">
            Define your global writing rules (e.g. "concise, direct, Indian English where appropriate").
          </p>
        </div>

        {/* Existing Instructions */}
        <div className="space-y-2">
          {instructions.map((inst) => (
            <div
              key={inst.id}
              className="flex items-start justify-between p-3.5 rounded-xl bg-zinc-900/40 border border-white/5 gap-3"
            >
              <div className="space-y-0.5">
                <span className="text-xs font-semibold text-zinc-200 block">{inst.title}</span>
                <p className="text-xs text-zinc-400 font-mono">{inst.instruction}</p>
              </div>
              <button
                onClick={() => handleDeleteInstruction(inst.id)}
                className="text-zinc-500 hover:text-rose-400 p-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          ))}
        </div>

        {/* Add Instruction Inputs */}
        <div className="p-4 rounded-xl bg-zinc-950/40 border border-white/5 space-y-3">
          <input
            type="text"
            placeholder="Rule Title (e.g. 'Concise Indian English')"
            value={newInstTitle}
            onChange={(e) => setNewInstTitle(e.target.value)}
            className="w-full px-3 py-2 rounded-lg bg-zinc-900 border border-white/10 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500"
          />
          <textarea
            placeholder="Instruction details: Keep phrasing direct, eliminate corporate buzzwords, use Indian English nuances when appropriate..."
            value={newInstBody}
            onChange={(e) => setNewInstBody(e.target.value)}
            rows={2}
            className="w-full px-3 py-2 rounded-lg bg-zinc-900 border border-white/10 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500 resize-none"
          />
          <button
            onClick={handleAddInstruction}
            className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-zinc-800 hover:bg-zinc-700 text-white transition-colors"
          >
            Add Style Rule
          </button>
        </div>
      </section>

      {/* Section 3: License Information (PRD Section 25 & 31) */}
      <section className="p-5 rounded-2xl bg-[#121215] border border-white/10 space-y-3">
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <h2 className="text-sm font-semibold text-white flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-blue-400" />
              Own Voice Lifetime License
            </h2>
            <p className="text-xs text-zinc-400">
              Your software license gives you permanent access to the desktop web app and updates.
            </p>
          </div>
          <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-blue-950/80 text-blue-400 border border-blue-500/30">
            LIFETIME ACTIVE
          </span>
        </div>

        {profile?.licenses?.[0] && (
          <div className="p-3 rounded-xl bg-zinc-950/60 border border-white/5 text-xs font-mono flex items-center justify-between text-zinc-300">
            <span>Key: {profile.licenses[0].licenseKey}</span>
            <span className="text-zinc-500">
              Activated: {new Date(profile.licenses[0].activatedAt).toLocaleDateString()}
            </span>
          </div>
        )}
      </section>

      {/* Connect Provider Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md rounded-2xl bg-[#141418] border border-white/10 shadow-2xl p-6 space-y-5 animate-scale-in">
            <div className="space-y-1">
              <h3 className="text-base font-bold text-white tracking-tight">Connect AI Provider</h3>
              <p className="text-xs text-zinc-400">
                Enter your secret API key. We test the connection immediately and store it using AES-256-GCM.
              </p>
            </div>

            {/* Provider Selector */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-zinc-300">Choose Provider:</label>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { id: "openai", name: "OpenAI (Whisper + GPT-4o)" },
                  { id: "groq", name: "Groq (Fast Whisper + Llama)" },
                  { id: "google", name: "Google Gemini" },
                  { id: "anthropic", name: "Anthropic Claude" },
                ].map((prov) => (
                  <button
                    key={prov.id}
                    onClick={() => setNewProvider(prov.id as any)}
                    className={`p-2.5 rounded-xl border text-xs font-medium text-left transition-all ${
                      newProvider === prov.id
                        ? "bg-blue-600/20 border-blue-500 text-white"
                        : "bg-zinc-900 border-white/5 text-zinc-400 hover:text-white"
                    }`}
                  >
                    {prov.name}
                  </button>
                ))}
              </div>
            </div>

            {/* API Key Input */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-zinc-300">Secret API Key:</label>
              <input
                type="password"
                placeholder={
                  newProvider === "openai" ? "sk-proj-..." : newProvider === "groq" ? "gsk_..." : "Key..."
                }
                value={newApiKey}
                onChange={(e) => setNewApiKey(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-zinc-900 border border-white/10 text-xs text-white placeholder-zinc-500 font-mono focus:outline-none focus:border-blue-500"
              />
            </div>

            {testResult && !testResult.success && (
              <div className="p-3 rounded-lg bg-rose-950/40 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
                <span>{testResult.message}</span>
              </div>
            )}

            {/* Modal Buttons */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => {
                  setIsModalOpen(false);
                  setTestResult(null);
                }}
                className="px-3 py-2 rounded-xl text-xs font-medium text-zinc-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                onClick={handleTestAndSaveProvider}
                disabled={testingConnection}
                className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white transition-all shadow-sm disabled:opacity-50"
              >
                {testingConnection ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <ShieldCheck className="w-3.5 h-3.5" />}
                <span>{testingConnection ? "Testing Connection..." : "Test & Save"}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
