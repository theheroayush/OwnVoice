"use client";

import { useEffect, useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  CheckCircle2,
  Copy,
  Check,
  Download,
  Key,
  ShieldCheck,
  ArrowRight,
  Sparkles,
  Monitor,
  Laptop,
  Smartphone,
  Globe,
  Loader2,
  AlertCircle,
} from "lucide-react";

function SuccessContent() {
  const searchParams = useSearchParams();
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [licenseKey, setLicenseKey] = useState<string>("");
  const [customerEmail, setCustomerEmail] = useState<string>("");
  const [downloadToken, setDownloadToken] = useState<string>("");
  const [expiresAt, setExpiresAt] = useState<string>("");

  useEffect(() => {
    const emailParam = searchParams.get("email");
    const licenseParam = searchParams.get("license_key");
    const sessionId = searchParams.get("session_id") || searchParams.get("order_id");

    async function resolveLicense() {
      try {
        setLoading(true);
        // Request signed token and license details from secure API
        const res = await fetch("/api/download/token", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: emailParam || undefined,
            licenseKey: licenseParam || undefined,
            orderId: sessionId || undefined,
          }),
        });

        const data = await res.json();
        if (data.success && data.token) {
          setLicenseKey(data.licenseKey);
          setCustomerEmail(data.email);
          setDownloadToken(data.token);
          setExpiresAt(data.expiresAt);
        } else {
          // If direct token generation couldn't match (e.g. freshly returned from webhook),
          // fallback to display parameters passed from checkout session
          if (licenseParam) {
            setLicenseKey(licenseParam);
            setCustomerEmail(emailParam || "your email");
          } else {
            setError(
              data.error ||
                "Payment was successful! Your license is being finalized. Please check your email or refresh in a moment."
            );
          }
        }
      } catch (err: any) {
        console.error(err);
        if (licenseParam) {
          setLicenseKey(licenseParam);
          setCustomerEmail(emailParam || "your email");
        } else {
          setError("Failed to fetch license credentials. Please refresh this page.");
        }
      } finally {
        setLoading(false);
      }
    }

    resolveLicense();
  }, [searchParams]);

  const handleCopy = () => {
    if (!licenseKey) return;
    navigator.clipboard.writeText(licenseKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div className="min-h-screen bg-[#07090e] text-[#EDEDED] flex flex-col items-center justify-center px-4 py-12 sm:px-6">
      <div className="max-w-3xl w-full space-y-8">
        {/* Top Celebration Badge */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold tracking-wide uppercase">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            Payment Successful • Lifetime Access Unlocked
          </div>
          <h1 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
            Welcome to Own Voice
          </h1>
          <p className="text-sm sm:text-base text-zinc-400 max-w-xl mx-auto">
            Your lifetime software license has been provisioned. A confirmation receipt has also been sent to{" "}
            <span className="text-blue-400 font-mono font-medium">{customerEmail || "your email"}</span>.
          </p>
        </div>

        {loading ? (
          <div className="p-12 rounded-3xl bg-[#0e121e]/80 border border-white/10 flex flex-col items-center justify-center space-y-4">
            <Loader2 className="w-8 h-8 text-blue-500 animate-spin" />
            <p className="text-sm text-zinc-400">Generating secure activation keys and download links...</p>
          </div>
        ) : error && !licenseKey ? (
          <div className="p-8 rounded-3xl bg-amber-500/10 border border-amber-500/20 text-center space-y-4">
            <AlertCircle className="w-8 h-8 text-amber-400 mx-auto" />
            <p className="text-sm text-amber-200">{error}</p>
            <Link
              href="/download/lookup"
              className="inline-flex items-center gap-2 px-6 py-2.5 rounded-full bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition-all"
            >
              Lookup License by Email
            </Link>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Card 1: Lifetime License Key Display */}
            <div className="p-6 sm:p-8 rounded-3xl bg-[#0e121e]/90 border border-blue-500/30 shadow-[0_20px_50px_rgba(0,0,0,0.7),0_0_30px_rgba(59,130,246,0.15)] relative overflow-hidden space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/[0.08] pb-4">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-blue-600/20 text-blue-400 flex items-center justify-center">
                    <Key className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-white uppercase tracking-wider">
                      Your Lifetime License Key
                    </h3>
                    <p className="text-xs text-zinc-400">
                      Valid for lifetime access across up to 3 personal devices
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 text-[11px] text-emerald-400 font-mono bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20 w-fit">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>ACTIVE • BYOK EDITION</span>
                </div>
              </div>

              {/* Monospace Key Box with Copy Button */}
              <div className="flex flex-col sm:flex-row items-center gap-3 bg-black/60 border border-white/10 rounded-2xl p-3 sm:p-4">
                <div className="font-mono text-lg sm:text-2xl font-bold tracking-widest text-blue-300 text-center sm:text-left flex-1 select-all break-all">
                  {licenseKey || "OV-XXXX-XXXX-XXXX-XXXX"}
                </div>
                <button
                  type="button"
                  onClick={handleCopy}
                  className={`w-full sm:w-auto px-5 py-2.5 rounded-xl font-semibold text-xs flex items-center justify-center gap-2 transition-all ${
                    copied
                      ? "bg-emerald-600 text-white shadow-[0_0_15px_rgba(16,185,129,0.4)]"
                      : "bg-blue-600 hover:bg-blue-500 text-white shadow-[0_0_15px_rgba(59,130,246,0.3)]"
                  }`}
                >
                  {copied ? (
                    <>
                      <Check className="w-4 h-4" />
                      Copied!
                    </>
                  ) : (
                    <>
                      <Copy className="w-4 h-4" />
                      Copy Key
                    </>
                  )}
                </button>
              </div>

              <p className="text-[11px] text-zinc-500">
                ⚠️ Store this key safely. You will paste this key when you first launch the Own Voice desktop application.
              </p>
            </div>

            {/* Card 2: Anti-Piracy Secure Downloads */}
            <div className="p-6 sm:p-8 rounded-3xl bg-[#0a0d14] border border-white/10 space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h3 className="text-base font-bold text-white">Download Own Voice</h3>
                  <p className="text-xs text-zinc-400">
                    Links are cryptographically signed for your account and expire in 24 hours.
                  </p>
                </div>
                <div className="inline-flex items-center gap-1.5 text-[11px] text-zinc-400 bg-zinc-900 px-3 py-1 rounded-full border border-white/10 w-fit">
                  <ShieldCheck className="w-3.5 h-3.5 text-blue-400" />
                  <span>Tamper-proof signed URL</span>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {/* Windows Download */}
                <a
                  href={downloadToken ? `/api/download/secure?token=${downloadToken}&os=windows` : "#"}
                  className="p-4 rounded-2xl bg-zinc-900/80 hover:bg-blue-950/40 border border-white/10 hover:border-blue-500/50 transition-all flex flex-col justify-between space-y-4 group"
                >
                  <div className="space-y-2">
                    <div className="w-8 h-8 rounded-lg bg-blue-600/20 text-blue-400 flex items-center justify-center group-hover:scale-110 transition-transform">
                      <Monitor className="w-4 h-4" />
                    </div>
                    <div className="font-bold text-sm text-white">Windows</div>
                    <div className="text-[11px] text-zinc-400">
                      Windows 10 / 11 (64-bit & ARM64)
                    </div>
                  </div>
                  <div className="pt-2 flex items-center gap-1.5 text-xs font-semibold text-blue-400 group-hover:text-blue-300">
                    <Download className="w-3.5 h-3.5" />
                    <span>Download (.zip)</span>
                  </div>
                </a>

                {/* macOS Workspace & Beta */}
                <Link
                  href="/app"
                  className="p-4 rounded-2xl bg-zinc-900/80 hover:bg-blue-950/40 border border-white/10 hover:border-blue-500/50 transition-all flex flex-col justify-between space-y-4 group text-left"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="w-8 h-8 rounded-lg bg-indigo-600/20 text-indigo-400 flex items-center justify-center group-hover:scale-110 transition-transform">
                        <Laptop className="w-4 h-4" />
                      </div>
                      <span className="text-[10px] font-mono text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded-full border border-indigo-500/20">
                        WEB LIVE • BETA
                      </span>
                    </div>
                    <div className="font-bold text-sm text-white">macOS</div>
                    <div className="text-[11px] text-zinc-400 leading-snug">
                      Full Web Workspace live now in Safari & Chrome. Native .dmg is in private beta testing.
                    </div>
                  </div>
                  <div className="pt-2 flex items-center gap-1.5 text-xs font-semibold text-indigo-400 group-hover:text-indigo-300">
                    <span>Open Web Workspace</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </div>
                </Link>

                {/* Android Companion APK */}
                <a
                  href={downloadToken ? `/api/download/secure?token=${downloadToken}&os=android` : "#"}
                  className="p-4 rounded-2xl bg-zinc-900/80 hover:bg-blue-950/40 border border-white/10 hover:border-blue-500/50 transition-all flex flex-col justify-between space-y-4 group"
                >
                  <div className="space-y-2">
                    <div className="w-8 h-8 rounded-lg bg-emerald-600/20 text-emerald-400 flex items-center justify-center group-hover:scale-110 transition-transform">
                      <Smartphone className="w-4 h-4" />
                    </div>
                    <div className="font-bold text-sm text-white">Android</div>
                    <div className="text-[11px] text-zinc-400">
                      Direct APK Companion
                    </div>
                  </div>
                  <div className="pt-2 flex items-center gap-1.5 text-xs font-semibold text-emerald-400 group-hover:text-emerald-300">
                    <Download className="w-3.5 h-3.5" />
                    <span>Download (.apk)</span>
                  </div>
                </a>
              </div>

              {/* Windows SmartScreen Reassurance Tip */}
              <div className="p-4 rounded-2xl bg-blue-950/20 border border-blue-500/20 flex items-start gap-3.5 text-left">
                <div className="w-7 h-7 rounded-lg bg-blue-600/20 text-blue-400 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <div className="space-y-1">
                  <div className="text-xs font-bold text-white">Windows Installation Note (SmartScreen)</div>
                  <div className="text-[11px] text-zinc-300 leading-relaxed">
                    Because Own Voice is an indie tool without an expensive corporate enterprise signature, Windows may show: <em>"Windows protected your PC"</em>. Simply click <strong>"More info"</strong> and then <strong>"Run anyway"</strong>. The app runs 100% locally with zero tracking.
                  </div>
                </div>
              </div>

              {/* Instant Web Workspace Alternative */}
              <div className="p-4 rounded-2xl bg-blue-950/30 border border-blue-500/20 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center gap-3 text-left">
                  <div className="w-8 h-8 rounded-lg bg-blue-600/30 text-blue-300 flex items-center justify-center flex-shrink-0">
                    <Globe className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-white">Don't want to install anything right now?</div>
                    <div className="text-[11px] text-zinc-400">
                      You can start using Own Voice directly inside your browser right away!
                    </div>
                  </div>
                </div>

                <Link
                  href="/app"
                  className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-95 whitespace-nowrap"
                >
                  Launch Web Workspace
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>

            {/* Quick 4-Step Setup Guide */}
            <div className="p-6 rounded-3xl bg-zinc-950/60 border border-white/[0.08] space-y-4">
              <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">
                Quick Setup (Takes 60 seconds)
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 text-xs">
                <div className="p-3 rounded-xl bg-zinc-900/50 border border-white/5 space-y-1">
                  <div className="text-blue-400 font-mono font-bold">1. Install</div>
                  <p className="text-zinc-400 text-[11px]">Run the downloaded app installer on your machine.</p>
                </div>
                <div className="p-3 rounded-xl bg-zinc-900/50 border border-white/5 space-y-1">
                  <div className="text-blue-400 font-mono font-bold">2. Activate</div>
                  <p className="text-zinc-400 text-[11px]">Paste your license key above into the app prompt.</p>
                </div>
                <div className="p-3 rounded-xl bg-zinc-900/50 border border-white/5 space-y-1">
                  <div className="text-blue-400 font-mono font-bold">3. Add Key</div>
                  <p className="text-zinc-400 text-[11px]">Enter your API key (OpenAI, Gemini, Groq, etc.).</p>
                </div>
                <div className="p-3 rounded-xl bg-zinc-900/50 border border-white/5 space-y-1">
                  <div className="text-blue-400 font-mono font-bold">4. Speak</div>
                  <p className="text-zinc-400 text-[11px]">Press F8 anywhere and speak your thoughts freely.</p>
                </div>
              </div>
            </div>

            {/* Bottom Links */}
            <div className="flex items-center justify-between text-xs text-zinc-500 pt-2">
              <Link href="/" className="hover:text-white transition-colors">
                ← Back to Home
              </Link>
              <Link href="/download/lookup" className="hover:text-blue-400 transition-colors">
                Need to retrieve your license later? Use License Lookup
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function CheckoutSuccessPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#07090e] flex items-center justify-center text-white">
          <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
        </div>
      }
    >
      <SuccessContent />
    </Suspense>
  );
}
