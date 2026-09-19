"use client";

import { useState } from "react";
import Link from "next/link";
import {
  Key,
  Mail,
  ShieldCheck,
  Copy,
  Check,
  Download,
  ArrowRight,
  Monitor,
  Laptop,
  Smartphone,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";

export default function LicenseLookupPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<{
    licenseKey: string;
    token: string;
    email: string;
    expiresAt: string;
  } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleLookup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const res = await fetch("/api/download/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim().toLowerCase() }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        setError(data.error || "No active license found for this email.");
      } else {
        setResult({
          licenseKey: data.licenseKey,
          token: data.token,
          email: data.email,
          expiresAt: data.expiresAt,
        });
      }
    } catch (err: any) {
      console.error(err);
      setError("Network error while looking up license. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (!result?.licenseKey) return;
    navigator.clipboard.writeText(result.licenseKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col justify-between">
      <MarketingNavbar />
      <main className="flex-1 max-w-2xl mx-auto px-4 sm:px-6 py-16 sm:py-20 w-full space-y-8">
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            <Key className="w-3.5 h-3.5" />
            License & Download Recovery
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Retrieve Your Own Voice License
          </h1>
          <p className="text-xs sm:text-sm text-zinc-400 max-w-md mx-auto">
            Switching machines or lost your key? Enter your purchase email to retrieve your lifetime key and a fresh 24-hour secure download link.
          </p>
        </div>

        {/* Form Card */}
        <div className="p-6 sm:p-8 rounded-3xl bg-[#0e121e]/90 border border-white/10 space-y-6">
          <form onSubmit={handleLookup} className="space-y-4">
            <div className="space-y-1.5 text-left">
              <label className="text-xs font-semibold text-zinc-300">Purchase Email</label>
              <div className="relative">
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full px-4 py-3 pl-10 rounded-xl bg-black/50 border border-white/10 text-white placeholder-zinc-500 text-sm focus:outline-none focus:border-blue-500 transition-colors"
                />
                <Mail className="w-4 h-4 text-zinc-400 absolute left-3.5 top-3.5" />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || !email}
              className="w-full py-3.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-95 disabled:opacity-50"
            >
              {loading ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  Find My License & Generate Download Links
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {error && (
            <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-xs text-red-300 flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Lookup Failed</p>
                <p>{error}</p>
                <p className="pt-1 text-zinc-400">
                  Haven't purchased yet?{" "}
                  <Link href="/#pricing" className="text-blue-400 underline">
                    Get Own Voice Lifetime License here
                  </Link>
                  .
                </p>
              </div>
            </div>
          )}

          {/* Success Box */}
          {result && (
            <div className="p-5 rounded-2xl bg-black/60 border border-emerald-500/30 space-y-5 animate-in fade-in duration-300">
              <div className="flex items-center justify-between border-b border-white/10 pb-3">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span className="text-xs font-bold text-white">Lifetime License Found</span>
                </div>
                <span className="text-[11px] font-mono text-emerald-400">ACTIVE</span>
              </div>

              {/* Key Row */}
              <div className="flex flex-col sm:flex-row items-center gap-3">
                <div className="font-mono text-base sm:text-lg font-bold text-blue-300 tracking-wider flex-1 select-all bg-zinc-900/80 px-3 py-2 rounded-lg border border-white/5 w-full text-center sm:text-left">
                  {result.licenseKey}
                </div>
                <button
                  type="button"
                  onClick={handleCopy}
                  className="w-full sm:w-auto px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-1.5 transition-all"
                >
                  {copied ? (
                    <>
                      <Check className="w-3.5 h-3.5" /> Copied!
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" /> Copy
                    </>
                  )}
                </button>
              </div>

              {/* Download Buttons */}
              <div className="space-y-2 pt-1">
                <div className="text-[11px] text-zinc-400">
                  Fresh 24-hour encrypted download links:
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <a
                    href={`/api/download/secure?token=${result.token}&os=windows`}
                    className="p-3 rounded-xl bg-zinc-900 hover:bg-blue-950/40 border border-white/10 hover:border-blue-500/40 text-left transition-all group flex items-center justify-between"
                  >
                    <div className="flex items-center gap-2">
                      <Monitor className="w-4 h-4 text-blue-400" />
                      <span className="text-xs font-semibold text-white">Windows</span>
                    </div>
                    <Download className="w-3.5 h-3.5 text-zinc-400 group-hover:text-blue-400" />
                  </a>

                  <a
                    href={`/api/download/secure?token=${result.token}&os=mac`}
                    className="p-3 rounded-xl bg-zinc-900 hover:bg-blue-950/40 border border-white/10 hover:border-blue-500/40 text-left transition-all group flex items-center justify-between"
                  >
                    <div className="flex items-center gap-2">
                      <Laptop className="w-4 h-4 text-indigo-400" />
                      <span className="text-xs font-semibold text-white">macOS</span>
                    </div>
                    <Download className="w-3.5 h-3.5 text-zinc-400 group-hover:text-blue-400" />
                  </a>

                  <a
                    href={`/api/download/secure?token=${result.token}&os=android`}
                    className="p-3 rounded-xl bg-zinc-900 hover:bg-blue-950/40 border border-white/10 hover:border-blue-500/40 text-left transition-all group flex items-center justify-between"
                  >
                    <div className="flex items-center gap-2">
                      <Smartphone className="w-4 h-4 text-emerald-400" />
                      <span className="text-xs font-semibold text-white">Android</span>
                    </div>
                    <Download className="w-3.5 h-3.5 text-zinc-400 group-hover:text-emerald-400" />
                  </a>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
