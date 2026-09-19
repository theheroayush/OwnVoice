"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Check, ArrowRight, ShieldCheck, Zap, HelpCircle } from "lucide-react";

export function PricingCalculator() {
  const [currency, setCurrency] = useState<"USD" | "INR">("USD");

  const priceText = currency === "USD" ? "$49" : "₹3,999";
  const saasMonthly = currency === "USD" ? "$20" : "₹1,650";
  const saas3Year = currency === "USD" ? "$720" : "₹59,400";
  const ownVoice3Year = currency === "USD" ? "$95" : "₹7,500";
  const savings = currency === "USD" ? "$625+" : "₹51,000+";

  return (
    <div className="w-full max-w-5xl mx-auto space-y-12">
      {/* Currency Switcher */}
      <div className="flex justify-center">
        <div className="inline-flex p-1 rounded-xl bg-zinc-900 border border-white/10 text-xs font-semibold">
          <button
            onClick={() => setCurrency("USD")}
            className={`px-4 py-1.5 rounded-lg transition-all ${
              currency === "USD" ? "bg-blue-600 text-white shadow-sm" : "text-zinc-400 hover:text-white"
            }`}
          >
            USD ($)
          </button>
          <button
            onClick={() => setCurrency("INR")}
            className={`px-4 py-1.5 rounded-lg transition-all ${
              currency === "INR" ? "bg-blue-600 text-white shadow-sm" : "text-zinc-400 hover:text-white"
            }`}
          >
            INR (₹)
          </button>
        </div>
      </div>

      {/* Pricing Comparison Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-stretch">
        {/* Card 1: Traditional AI SaaS */}
        <div className="rounded-2xl bg-zinc-950/40 border border-white/5 p-6 sm:p-8 flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <div className="inline-block px-2.5 py-1 rounded text-[11px] font-mono font-medium text-zinc-400 bg-zinc-900 border border-white/5">
              Traditional Voice SaaS
            </div>
            <div className="space-y-1">
              <span className="text-3xl font-bold text-zinc-300 line-through decoration-rose-500/50">
                {saasMonthly}
              </span>
              <span className="text-xs text-zinc-500"> / month, every month forever</span>
            </div>
            <p className="text-xs text-zinc-400 leading-relaxed">
              You rent the software indefinitely. Platform throttles your requests during peak hours and marks up AI inference costs by 500% to 1000%.
            </p>

            <ul className="space-y-2.5 text-xs text-zinc-400 pt-2">
              <li className="flex items-center gap-2 text-zinc-500">
                <span className="text-rose-500 font-bold">✕</span> Monthly recurring credit card charge
              </li>
              <li className="flex items-center gap-2 text-zinc-500">
                <span className="text-rose-500 font-bold">✕</span> Cloud platform controls your AI models
              </li>
              <li className="flex items-center gap-2 text-zinc-500">
                <span className="text-rose-500 font-bold">✕</span> Usage caps and silent rate limits
              </li>
              <li className="flex items-center gap-2 text-zinc-500">
                <span className="text-rose-500 font-bold">✕</span> Lose your tool if you cancel
              </li>
            </ul>
          </div>

          <div className="p-4 rounded-xl bg-zinc-900/50 border border-white/5 text-center">
            <span className="text-xs text-zinc-400">3-Year Cost:</span>
            <div className="text-lg font-bold text-rose-400 mt-0.5">{saas3Year}</div>
          </div>
        </div>

        {/* Card 2: Own Voice (Lifetime + BYOK) */}
        <div className="rounded-2xl bg-[#121215] border-2 border-blue-500/40 shadow-2xl p-6 sm:p-8 flex flex-col justify-between space-y-6 relative overflow-hidden">
          <div className="absolute top-0 right-0 bg-blue-600 text-white text-[10px] font-bold uppercase tracking-wider px-3.5 py-1 rounded-bl-xl">
            You Own It
          </div>

          <div className="space-y-4">
            <div className="inline-block px-2.5 py-1 rounded text-[11px] font-mono font-medium text-blue-400 bg-blue-950/40 border border-blue-800/40">
              Own Voice Lifetime
            </div>
            <div className="space-y-1">
              <span className="text-4xl font-extrabold text-white tracking-tight">{priceText}</span>
              <span className="text-xs text-zinc-400 font-medium"> one-time purchase</span>
            </div>
            <p className="text-xs text-zinc-300 leading-relaxed">
              Buy the software once. Connect your OpenAI, Groq, Google, or Anthropic API key. Pay fractions of a cent per voice session directly to the provider.
            </p>

            <ul className="space-y-2.5 text-xs text-zinc-200 pt-2">
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> Lifetime software updates and desktop apps
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> Bring your own keys (OpenAI, Gemini, Anthropic, Groq)
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> Zero artificial usage caps or markup
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> AES-256-GCM encrypted API key security
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> Ephemeral audio processing (raw audio deleted immediately)
              </li>
            </ul>
          </div>

          <div className="space-y-3">
            <div className="p-4 rounded-xl bg-blue-950/30 border border-blue-500/20 text-center">
              <span className="text-xs text-blue-300">You Save over 3 Years:</span>
              <div className="text-xl font-extrabold text-emerald-400 mt-0.5">{savings}</div>
              <span className="text-[11px] text-zinc-400 mt-1 block">
                (Typical API inference cost: ~$1.20/month at 100 voice notes/day)
              </span>
            </div>

            <Link
              href="/signup"
              className="w-full py-3 px-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm flex items-center justify-center gap-2 transition-all shadow-lg hover:shadow-blue-500/25 active:scale-98"
            >
              Get Own Voice Lifetime Access
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
