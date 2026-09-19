"use client";

import React, { useState } from "react";
import { Check, X, ShieldCheck, ArrowRight, Zap, TrendingDown, Sparkles } from "lucide-react";

export function SaaSComparisonTable() {
  const [currency, setCurrency] = useState<"INR" | "USD">("INR");
  const [minutesPerDay, setMinutesPerDay] = useState<number>(15);

  // Financial calculations
  // Average words per minute: 150
  // Monthly minutes: minutesPerDay * 25 working days
  const monthlyMinutes = minutesPerDay * 25;
  
  // Raw API Wholesale Cost (Gemini 2.5 Flash / Groq): ~$0.0001 per word or ~$0.0005 per audio minute
  // Average monthly API fee:
  const monthlyApiUsd = Math.max(0.1, Number((monthlyMinutes * 0.006).toFixed(2)));
  const monthlyApiInr = Math.round(monthlyApiUsd * 85);

  // 3-Year Comparison
  // Wispr Flow: $15/mo ($180/yr = $540/3yr or ₹45,900)
  // Superwhisper: $8/mo ($96/yr = $288/3yr or ₹24,480)
  // Own Voice: $49 one-time + (monthlyApiUsd * 36)
  const wispr3Year = currency === "INR" ? "₹45,900" : "$540";
  const otter3Year = currency === "INR" ? "₹52,000" : "$612";
  
  const ownVoiceOneTime = currency === "INR" ? "₹1,499" : "$49";
  const ownVoiceApi3Year = currency === "INR" ? monthlyApiInr * 36 : Number((monthlyApiUsd * 36).toFixed(0));
  const ownVoiceTotal3Year =
    currency === "INR"
      ? `₹${(1499 + ownVoiceApi3Year).toLocaleString("en-IN")}`
      : `$${49 + Number(ownVoiceApi3Year)}`;

  const savings3Year =
    currency === "INR"
      ? `₹${(45900 - (1499 + ownVoiceApi3Year)).toLocaleString("en-IN")}+`
      : `$${540 - (49 + Number(ownVoiceApi3Year))}+`;

  return (
    <section id="comparison" className="py-24 px-4 sm:px-6 relative overflow-hidden">
      <div className="max-w-5xl mx-auto space-y-12 relative z-10">
        {/* Header */}
        <div className="text-center space-y-3 max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-emerald-950/60 border border-emerald-500/30 text-emerald-400 text-xs font-mono tracking-wider uppercase">
            <TrendingDown className="w-3.5 h-3.5" />
            Kill The SaaS Tax
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold text-white tracking-tight">
            Stop paying ₹15,000/year for voice AI.
          </h2>
          <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed">
            Other platforms markup raw AI inference costs by 1,000% and charge you every single month. With Own Voice, you buy the software once and pay wholesale rates.
          </p>

          {/* Currency Switcher */}
          <div className="flex justify-center pt-2">
            <div className="inline-flex p-1 rounded-full bg-zinc-900 border border-white/10 text-xs font-medium">
              <button
                type="button"
                onClick={() => setCurrency("INR")}
                className={`px-4 py-1.5 rounded-full transition-all ${
                  currency === "INR"
                    ? "bg-blue-600 text-white font-semibold shadow-md"
                    : "text-zinc-400 hover:text-white"
                }`}
              >
                INR (₹) India
              </button>
              <button
                type="button"
                onClick={() => setCurrency("USD")}
                className={`px-4 py-1.5 rounded-full transition-all ${
                  currency === "USD"
                    ? "bg-blue-600 text-white font-semibold shadow-md"
                    : "text-zinc-400 hover:text-white"
                }`}
              >
                USD ($) Global
              </button>
            </div>
          </div>
        </div>

        {/* Interactive Usage Slider Box */}
        <div className="p-6 sm:p-8 rounded-3xl bg-[#0e121e]/90 border border-blue-500/30 shadow-[0_20px_50px_rgba(0,0,0,0.8)] space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.08] pb-5">
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white">
                Calculate Your Real 3-Year Savings
              </h3>
              <p className="text-xs text-zinc-400">
                Adjust how many minutes you speak per working day:
              </p>
            </div>
            <div className="text-left sm:text-right">
              <span className="text-2xl sm:text-3xl font-black text-blue-400 font-mono">
                {minutesPerDay} mins
              </span>
              <span className="text-xs text-zinc-400 block font-normal">
                (~{(minutesPerDay * 150).toLocaleString()} words per day)
              </span>
            </div>
          </div>

          {/* Range Slider */}
          <div className="space-y-2">
            <input
              type="range"
              min={5}
              max={60}
              step={5}
              value={minutesPerDay}
              onChange={(e) => setMinutesPerDay(Number(e.target.value))}
              className="w-full h-2 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-blue-500"
            />
            <div className="flex justify-between text-[11px] font-mono text-zinc-500">
              <span>Light (5 mins)</span>
              <span>Average (15 mins)</span>
              <span>Heavy (30 mins)</span>
              <span>Power User (60 mins)</span>
            </div>
          </div>

          {/* Savings Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
            <div className="p-4 rounded-2xl bg-zinc-900/60 border border-white/5 space-y-1">
              <span className="text-[11px] text-zinc-400 uppercase tracking-wider font-mono">
                Raw AI Cost (BYOK)
              </span>
              <div className="text-xl font-bold text-white">
                {currency === "INR" ? `~₹${monthlyApiInr}/mo` : `~$${monthlyApiUsd}/mo`}
              </div>
              <p className="text-[11px] text-zinc-500">
                Paid directly to Google/Groq wholesale
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-rose-950/20 border border-rose-500/20 space-y-1">
              <span className="text-[11px] text-rose-300 uppercase tracking-wider font-mono">
                Wispr Flow (3 Years)
              </span>
              <div className="text-xl font-bold text-rose-400 line-through">
                {wispr3Year}
              </div>
              <p className="text-[11px] text-zinc-500">
                Rented at $15/month forever
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-emerald-950/30 border border-emerald-500/30 space-y-1">
              <span className="text-[11px] text-emerald-400 uppercase tracking-wider font-mono font-bold">
                Your 3-Year Savings
              </span>
              <div className="text-2xl font-black text-emerald-400">
                {savings3Year}
              </div>
              <p className="text-[11px] text-emerald-300/80">
                Kept in your own pocket
              </p>
            </div>
          </div>
        </div>

        {/* Feature Comparison Matrix */}
        <div className="rounded-3xl bg-[#090b12] border border-white/10 overflow-hidden shadow-2xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-white/10 bg-zinc-900/50">
                  <th className="py-4 px-5 text-zinc-400 font-semibold uppercase text-[11px] tracking-wider">
                    Feature
                  </th>
                  <th className="py-4 px-5 text-white font-bold bg-blue-950/40 border-x border-blue-500/30">
                    <div className="flex items-center gap-1.5 text-blue-400">
                      <Sparkles className="w-3.5 h-3.5" />
                      Own Voice
                    </div>
                  </th>
                  <th className="py-4 px-5 text-zinc-400 font-medium">
                    Wispr Flow
                  </th>
                  <th className="py-4 px-5 text-zinc-400 font-medium">
                    Otter.ai
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-zinc-300">
                <tr>
                  <td className="py-3.5 px-5 font-medium text-white">Pricing Model</td>
                  <td className="py-3.5 px-5 font-bold text-emerald-400 bg-blue-950/20 border-x border-blue-500/20">
                    Pay Once (Lifetime)
                  </td>
                  <td className="py-3.5 px-5 text-zinc-400">$15 / month subscription</td>
                  <td className="py-3.5 px-5 text-zinc-400">$16.99 / month subscription</td>
                </tr>

                <tr>
                  <td className="py-3.5 px-5 font-medium text-white">Total Cost (3 Years)</td>
                  <td className="py-3.5 px-5 font-bold text-emerald-400 bg-blue-950/20 border-x border-blue-500/20">
                    {ownVoiceTotal3Year}
                  </td>
                  <td className="py-3.5 px-5 text-rose-400">{wispr3Year}</td>
                  <td className="py-3.5 px-5 text-rose-400">{otter3Year}</td>
                </tr>

                <tr>
                  <td className="py-3.5 px-5 font-medium text-white">Bring Your Own Key (BYOK)</td>
                  <td className="py-3.5 px-5 bg-blue-950/20 border-x border-blue-500/20">
                    <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                      <Check className="w-4 h-4" /> Gemini, OpenAI, Groq, Claude
                    </span>
                  </td>
                  <td className="py-3.5 px-5 text-zinc-500">
                    <span className="flex items-center gap-1 text-zinc-500">
                      <X className="w-4 h-4 text-rose-500" /> Locked to their cloud
                    </span>
                  </td>
                  <td className="py-3.5 px-5 text-zinc-500">
                    <span className="flex items-center gap-1 text-zinc-500">
                      <X className="w-4 h-4 text-rose-500" /> Proprietary cloud only
                    </span>
                  </td>
                </tr>

                <tr>
                  <td className="py-3.5 px-5 font-medium text-white">Desktop Floating Widget</td>
                  <td className="py-3.5 px-5 bg-blue-950/20 border-x border-blue-500/20">
                    <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                      <Check className="w-4 h-4" /> Global F8 hotkey anywhere
                    </span>
                  </td>
                  <td className="py-3.5 px-5 text-emerald-400">
                    <span className="flex items-center gap-1">
                      <Check className="w-4 h-4" /> Yes
                    </span>
                  </td>
                  <td className="py-3.5 px-5 text-zinc-500">
                    <span className="flex items-center gap-1">
                      <X className="w-4 h-4 text-rose-500" /> Web meeting bot only
                    </span>
                  </td>
                </tr>

                <tr>
                  <td className="py-3.5 px-5 font-medium text-white">Offline Local Fallback</td>
                  <td className="py-3.5 px-5 bg-blue-950/20 border-x border-blue-500/20">
                    <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                      <Check className="w-4 h-4" /> Offline Whisper included
                    </span>
                  </td>
                  <td className="py-3.5 px-5 text-zinc-500">
                    <span className="flex items-center gap-1">
                      <X className="w-4 h-4 text-rose-500" /> Requires active internet
                    </span>
                  </td>
                  <td className="py-3.5 px-5 text-zinc-500">
                    <span className="flex items-center gap-1">
                      <X className="w-4 h-4 text-rose-500" /> Requires cloud upload
                    </span>
                  </td>
                </tr>

                <tr>
                  <td className="py-3.5 px-5 font-medium text-white">Device Licenses</td>
                  <td className="py-3.5 px-5 bg-blue-950/20 border-x border-blue-500/20">
                    <span className="text-zinc-200 font-medium">Up to 3 personal devices</span>
                  </td>
                  <td className="py-3.5 px-5 text-zinc-400">1 user account</td>
                  <td className="py-3.5 px-5 text-zinc-400">1 user account</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Action Button */}
        <div className="text-center pt-2">
          <a
            href="#pricing"
            className="inline-flex items-center gap-2 px-8 py-4 rounded-full bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm transition-all shadow-[0_0_25px_rgba(59,130,246,0.4)] active:scale-95"
          >
            Claim Your Lifetime Access ({ownVoiceOneTime})
            <ArrowRight className="w-4 h-4" />
          </a>
        </div>
      </div>
    </section>
  );
}
