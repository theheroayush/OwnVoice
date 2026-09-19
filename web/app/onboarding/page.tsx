"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { Mic, KeyRound, CheckCircle2, ArrowRight, ShieldCheck, RefreshCw, Sparkles, Volume2 } from "lucide-react";

export default function OnboardingPage() {
  const router = useRouter();
  const [currentStep, setCurrentStep] = useState(1);
  const [provider, setProvider] = useState<"openai" | "groq" | "google" | "anthropic">("openai");
  const [apiKey, setApiKey] = useState("");
  const [isTesting, setIsTesting] = useState(false);
  const [connectionSuccess, setConnectionSuccess] = useState(false);
  const [testError, setTestError] = useState<string | null>(null);
  const [micGranted, setMicGranted] = useState(false);

  const testAndConnect = async () => {
    if (!apiKey.trim()) {
      setTestError("Please enter an API key.");
      return;
    }

    setIsTesting(true);
    setTestError(null);

    try {
      const res = await fetch("/api/providers", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          provider,
          apiKey: apiKey.trim(),
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        setTestError(data.error || "Connection failed. Please check your key.");
      } else {
        setConnectionSuccess(true);
        setTimeout(() => setCurrentStep(5), 600);
      }
    } catch (err: any) {
      setTestError(err.message || "Network error.");
    } finally {
      setIsTesting(false);
    }
  };

  const requestMicPermission = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((track) => track.stop());
      setMicGranted(true);
      setTimeout(() => setCurrentStep(6), 600);
    } catch (err) {
      alert("Microphone permission was denied. Please allow microphone access in your browser settings to proceed.");
    }
  };

  return (
    <div className="min-h-screen bg-[#09090b] text-white flex flex-col items-center justify-center p-4 sm:p-6">
      <div className="w-full max-w-lg rounded-2xl bg-[#121215] border border-white/10 shadow-2xl p-6 sm:p-8 space-y-6 animate-scale-in">
        {/* Progress Bar (7 steps) */}
        <div className="flex items-center justify-between gap-1.5">
          {[1, 2, 3, 4, 5, 6, 7].map((s) => (
            <div
              key={s}
              className={`h-1.5 flex-1 rounded-full transition-all ${
                s <= currentStep ? "bg-blue-600" : "bg-zinc-800"
              }`}
            />
          ))}
        </div>

        {/* Step 1: Welcome */}
        {currentStep === 1 && (
          <div className="text-center space-y-5 py-4">
            <div className="w-16 h-16 rounded-2xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center mx-auto">
              <Mic className="w-8 h-8" />
            </div>
            <div className="space-y-1.5">
              <h2 className="text-2xl font-bold tracking-tight">Welcome to Own Voice</h2>
              <p className="text-xs text-zinc-400 max-w-sm mx-auto leading-relaxed">
                You own the software. You bring your own AI keys. In less than 2 minutes, you will speak your first thought into finished work.
              </p>
            </div>
            <button
              onClick={() => setCurrentStep(2)}
              className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-98"
            >
              Get Started
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Step 2: Choose Provider */}
        {currentStep === 2 && (
          <div className="space-y-5">
            <div className="space-y-1">
              <span className="text-[11px] font-mono uppercase tracking-wider text-blue-400">Step 2 of 7</span>
              <h2 className="text-xl font-bold">Choose your AI Provider</h2>
              <p className="text-xs text-zinc-400">
                Pick the AI provider whose API key you want to use. You can add more later.
              </p>
            </div>

            <div className="grid grid-cols-1 gap-2.5">
              {[
                { id: "openai", name: "OpenAI", desc: "Whisper speech transcription + GPT-4o intelligence" },
                { id: "groq", name: "Groq", desc: "Ultra-fast LPU inference (Whisper-v3 + Llama-3.3)" },
                { id: "google", name: "Google Gemini", desc: "Multimodal audio reasoning + Gemini 2.5 Flash" },
                { id: "anthropic", name: "Anthropic", desc: "Claude 3.5 Sonnet writing and reasoning" },
              ].map((prov) => (
                <button
                  key={prov.id}
                  onClick={() => {
                    setProvider(prov.id as any);
                    setCurrentStep(3);
                  }}
                  className="flex items-center justify-between p-3.5 rounded-xl border border-white/5 bg-zinc-900/60 hover:bg-zinc-800/80 hover:border-white/15 transition-all text-left"
                >
                  <div>
                    <span className="text-xs font-semibold text-white block">{prov.name}</span>
                    <span className="text-[11px] text-zinc-400">{prov.desc}</span>
                  </div>
                  <ArrowRight className="w-4 h-4 text-zinc-500" />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Step 3 & 4: Enter Key and Test Connection */}
        {(currentStep === 3 || currentStep === 4) && (
          <div className="space-y-5">
            <div className="space-y-1">
              <span className="text-[11px] font-mono uppercase tracking-wider text-blue-400">Step 3 & 4 of 7</span>
              <h2 className="text-xl font-bold capitalize">Connect your {provider} key</h2>
              <p className="text-xs text-zinc-400">
                Your key will be encrypted at rest using AES-256-GCM. We never see or log your secret.
              </p>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-medium text-zinc-300">API Key</label>
              <input
                type="password"
                placeholder={provider === "openai" ? "sk-proj-..." : "Key..."}
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-zinc-900 border border-white/10 text-xs text-white placeholder-zinc-500 font-mono focus:outline-none focus:border-blue-500"
              />
            </div>

            {testError && (
              <div className="p-3 rounded-lg bg-rose-950/40 border border-rose-500/30 text-rose-300 text-xs">
                {testError}
              </div>
            )}

            <button
              onClick={testAndConnect}
              disabled={isTesting || !apiKey.trim()}
              className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shadow-md disabled:opacity-50 active:scale-98"
            >
              {isTesting ? <RefreshCw className="w-4 h-4 animate-spin" /> : <ShieldCheck className="w-4 h-4" />}
              <span>{isTesting ? "Validating key with provider..." : "Test & Save Connection"}</span>
            </button>
          </div>
        )}

        {/* Step 5: Microphone Permission */}
        {currentStep === 5 && (
          <div className="text-center space-y-5 py-4">
            <div className="w-16 h-16 rounded-2xl bg-emerald-600/20 border border-emerald-500/30 text-emerald-400 flex items-center justify-center mx-auto">
              <Volume2 className="w-8 h-8" />
            </div>
            <div className="space-y-1.5">
              <span className="text-[11px] font-mono uppercase tracking-wider text-blue-400">Step 5 of 7</span>
              <h2 className="text-xl font-bold">Enable Microphone</h2>
              <p className="text-xs text-zinc-400 max-w-sm mx-auto leading-relaxed">
                Own Voice requires microphone access to capture your speech. Audio is processed ephemerally and never permanently stored.
              </p>
            </div>
            <button
              onClick={requestMicPermission}
              className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-98"
            >
              Allow Microphone Access
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Step 6: First Voice */}
        {currentStep === 6 && (
          <div className="text-center space-y-5 py-4">
            <div className="w-16 h-16 rounded-2xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center mx-auto animate-pulse">
              <Mic className="w-8 h-8" />
            </div>
            <div className="space-y-1.5">
              <span className="text-[11px] font-mono uppercase tracking-wider text-blue-400">Step 6 of 7</span>
              <h2 className="text-xl font-bold">Say Something!</h2>
              <p className="text-xs text-zinc-400 max-w-sm mx-auto leading-relaxed">
                Everything is configured. Your microphone and AI key are verified and ready.
              </p>
            </div>
            <button
              onClick={() => setCurrentStep(7)}
              className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-98"
            >
              Continue to Workspace
              <Sparkles className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Step 7: Success! */}
        {currentStep === 7 && (
          <div className="text-center space-y-5 py-4">
            <div className="w-16 h-16 rounded-2xl bg-emerald-600/20 border border-emerald-500/30 text-emerald-400 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <div className="space-y-1.5">
              <h2 className="text-2xl font-bold">Your Voice is Ready</h2>
              <p className="text-xs text-zinc-400 max-w-sm mx-auto leading-relaxed">
                Setup took under 2 minutes. Welcome to your voice-first AI productivity workspace.
              </p>
            </div>
            <button
              onClick={() => router.push("/app")}
              className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-98"
            >
              Open Own Voice Workspace
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
