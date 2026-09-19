"use client";

import { useState } from "react";
import { Check, ArrowRight, ShieldCheck, Loader2, X, Mail, User, Lock } from "lucide-react";
import { load } from "@cashfreepayments/cashfree-js";

export function PricingCardRefined() {
  const [currency, setCurrency] = useState<"INR" | "USD">("INR");
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const priceFormatted = currency === "INR" ? "₹1,499" : "$49";
  const originalPriceFormatted = currency === "INR" ? "₹2,499" : "$99";

  const handleOpenModal = () => {
    setError(null);
    setIsModalOpen(true);
  };

  const handleCheckoutSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setError("Please enter your email address.");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch("/api/checkout/session", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          planId: "lifetime",
          currency,
          email: email.trim().toLowerCase(),
          name: name.trim() || undefined,
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || "Failed to initialize payment.");
      }

      // 1. Cashfree Production Hosted Checkout via official JS SDK
      if (data.gateway === "cashfree" && data.paymentSessionId) {
        try {
          const cashfree = await load({
            mode: "production",
          });
          if (cashfree) {
            await cashfree.checkout({
              paymentSessionId: data.paymentSessionId,
              redirectTarget: "_self",
            });
            return;
          }
        } catch (sdkErr: any) {
          console.warn("Cashfree SDK initialization fallback:", sdkErr);
        }
      }

      // 2. Fallback / Stripe Checkout URL
      if (data.checkoutUrl) {
        window.location.href = data.checkoutUrl;
      } else {
        throw new Error("No checkout URL returned from payment server.");
      }
    } catch (err: any) {
      console.error(err);
      setError(err.message || "Checkout error. Please try again.");
      setLoading(false);
    }
  };

  return (
    <section id="pricing" className="py-24 px-4 sm:px-6 relative">
      <div className="max-w-5xl mx-auto space-y-8">
        <div className="text-center space-y-3">
          <div className="inline-flex items-center px-3.5 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            Simple pricing
          </div>

          {/* Currency Toggle */}
          <div className="flex items-center justify-center gap-2 pt-1">
            <div className="inline-flex items-center p-1 rounded-full bg-zinc-900 border border-white/10 text-xs font-medium">
              <button
                type="button"
                onClick={() => setCurrency("INR")}
                className={`px-4 py-1.5 rounded-full transition-all ${
                  currency === "INR"
                    ? "bg-blue-600 text-white font-semibold shadow-md"
                    : "text-zinc-400 hover:text-white"
                }`}
              >
                INR (₹) • Cashfree UPI
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
                USD ($) • Stripe Global
              </button>
            </div>
          </div>
        </div>

        {/* The Main Pricing Container */}
        <div className="rounded-[32px] border border-white/[0.12] bg-[#0c0f17]/95 p-8 sm:p-12 lg:p-14 shadow-[0_25px_60px_rgba(0,0,0,0.8),0_0_30px_rgba(59,130,246,0.1)] relative overflow-hidden backdrop-blur-2xl">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
            {/* Left: Price & CTA */}
            <div className="lg:col-span-5 space-y-6">
              <div className="space-y-1">
                <h3 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
                  Own Voice
                </h3>
                <p className="text-xs sm:text-sm text-zinc-400">
                  One-time purchase. No subscription.
                </p>
              </div>

              <div className="flex items-baseline gap-3">
                <span className="text-4xl sm:text-5xl font-black text-white tracking-tight">
                  {priceFormatted}
                </span>
                <span className="text-base sm:text-lg text-zinc-500 line-through">
                  {originalPriceFormatted}
                </span>
              </div>

              <div>
                <button
                  type="button"
                  onClick={handleOpenModal}
                  className="w-full sm:w-auto px-8 py-4 rounded-full bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-[0_0_20px_rgba(59,130,246,0.4)] active:scale-95"
                >
                  Get Own Voice
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>

              <p className="text-[11px] text-zinc-500 flex items-center gap-1.5 pt-1">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                <span>30-day money back guarantee. Lifetime software ownership.</span>
              </p>
            </div>

            {/* Middle: Feature Checklist */}
            <div className="lg:col-span-4 space-y-3.5 border-t lg:border-t-0 lg:border-l border-white/[0.08] pt-6 lg:pt-0 lg:pl-8">
              {[
                "Desktop & web app",
                "Bring your own API key",
                "Multiple AI providers",
                "All core features",
                "Future updates (1 year)",
              ].map((feat, i) => (
                <div key={i} className="flex items-center gap-3 text-xs sm:text-sm text-zinc-300">
                  <div className="w-5 h-5 rounded-full bg-blue-600/20 text-blue-400 flex items-center justify-center flex-shrink-0">
                    <Check className="w-3.5 h-3.5" />
                  </div>
                  <span>{feat}</span>
                </div>
              ))}
            </div>

            {/* Right: 3D Software Box Mockup */}
            <div className="lg:col-span-3 flex flex-col items-center justify-center relative pt-4 lg:pt-0">
              <div className="relative w-36 h-48 sm:w-40 sm:h-52 rounded-xl bg-gradient-to-tr from-slate-950 via-zinc-900 to-blue-950 border border-blue-500/30 shadow-[15px_20px_35px_rgba(0,0,0,0.8),0_0_25px_rgba(59,130,246,0.2)] p-4 flex flex-col justify-between transform rotate-[-4deg] hover:rotate-0 transition-transform duration-300">
                <div className="text-[10px] text-zinc-500 uppercase tracking-wider font-mono">
                  Own Voice v1.0
                </div>

                <div className="space-y-2 text-center">
                  <div className="text-base font-black text-white tracking-tight">
                    Own Voice
                  </div>
                  {/* Glowing Soundwave on Box */}
                  <div className="flex items-center justify-center gap-1 h-8">
                    {[12, 22, 35, 18, 28, 14, 30, 20].map((h, idx) => (
                      <div
                        key={idx}
                        className="w-1 bg-gradient-to-t from-blue-500 to-indigo-300 rounded-full"
                        style={{ height: `${h}px` }}
                      />
                    ))}
                  </div>
                </div>

                <div className="text-[9px] text-blue-400 text-center font-mono">
                  BYOK Edition
                </div>
              </div>

              <div className="absolute -bottom-6 right-0 hidden sm:flex items-center gap-1 transform rotate-[-4deg]">
                <span className="font-handwriting text-xl text-blue-300 tracking-wide whitespace-nowrap">
                  Your voice. New possibilities.
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Guest 1-Click Checkout Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="relative w-full max-w-md rounded-3xl bg-[#0c101c] border border-blue-500/30 p-6 sm:p-8 shadow-2xl space-y-6 text-left">
            {/* Close Button */}
            <button
              type="button"
              onClick={() => !loading && setIsModalOpen(false)}
              className="absolute right-4 top-4 p-2 rounded-full text-zinc-400 hover:text-white hover:bg-white/5 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>

            {/* Header */}
            <div className="space-y-2">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-600/10 text-blue-400 text-xs font-mono font-medium">
                <Lock className="w-3.5 h-3.5" />
                1-Click Secure Checkout
              </div>
              <h3 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
                Get Own Voice Lifetime License
              </h3>
              <p className="text-xs text-zinc-400">
                Enter your email address to receive your lifetime license key and secure download access.
              </p>
            </div>

            {/* Price & Gateway Reminder */}
            <div className="p-3.5 rounded-2xl bg-zinc-900/80 border border-white/5 flex items-center justify-between">
              <div>
                <div className="text-xs text-zinc-400">One-time payment</div>
                <div className="text-lg font-bold text-white">{priceFormatted}</div>
              </div>
              <div className="text-right">
                <div className="text-[11px] text-zinc-400">Secure payment via</div>
                <div className="text-xs font-semibold text-blue-400">
                  {currency === "INR" ? "Cashfree (UPI/Cards)" : "Stripe (Cards/Apple Pay)"}
                </div>
              </div>
            </div>

            {/* Form */}
            <form onSubmit={handleCheckoutSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-zinc-300">
                  Email Address <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    type="email"
                    required
                    autoFocus
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="you@example.com"
                    className="w-full px-4 py-3 pl-10 rounded-xl bg-black/60 border border-white/10 text-white placeholder-zinc-500 text-sm focus:outline-none focus:border-blue-500 transition-colors"
                  />
                  <Mail className="w-4 h-4 text-zinc-400 absolute left-3.5 top-3.5" />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium text-zinc-300">
                  Full Name <span className="text-zinc-500 font-normal">(Optional)</span>
                </label>
                <div className="relative">
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Your Name"
                    className="w-full px-4 py-3 pl-10 rounded-xl bg-black/60 border border-white/10 text-white placeholder-zinc-500 text-sm focus:outline-none focus:border-blue-500 transition-colors"
                  />
                  <User className="w-4 h-4 text-zinc-400 absolute left-3.5 top-3.5" />
                </div>
              </div>

              {error && (
                <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-xs text-red-300">
                  {error}
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95 disabled:opacity-50"
              >
                {loading ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <>
                    Proceed to Payment ({priceFormatted})
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>

            <div className="flex items-center justify-center gap-2 text-[11px] text-zinc-500">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
              <span>256-bit encrypted checkout • 30-day money back guarantee</span>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
