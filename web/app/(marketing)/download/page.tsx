import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { Download, Monitor, Globe, CheckCircle2, ArrowRight } from "lucide-react";
import Link from "next/link";

export default function DownloadPage() {
  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-4xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-16">
        <div className="text-center space-y-3">
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            Get Own Voice on your Devices
          </h1>
          <p className="text-sm sm:text-base text-zinc-400 max-w-lg mx-auto">
            Run Own Voice natively on your Windows PC or access the full executive web workspace from any browser.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Card 1: Web App (Instant) */}
          <div className="p-7 rounded-2xl bg-zinc-900/40 border border-white/10 space-y-5 flex flex-col justify-between">
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center">
                <Globe className="w-5 h-5" />
              </div>
              <h3 className="text-lg font-bold text-white">Own Voice Web Workspace</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Zero installation required. Works in Chrome, Edge, Brave, and Safari with full Web Audio API support and BYOK encryption.
              </p>
              <ul className="space-y-2 text-xs text-zinc-300 pt-2">
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Instant access anywhere
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Real-time waveform visualizer
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Searchable history and exports
                </li>
              </ul>
            </div>

            <Link
              href="/app"
              className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-98"
            >
              Launch Web Workspace
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {/* Card 2: Windows Native Desktop App */}
          <div className="p-7 rounded-2xl bg-[#121215] border border-blue-500/30 space-y-5 flex flex-col justify-between relative overflow-hidden">
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-600/20 text-emerald-400 flex items-center justify-center">
                <Monitor className="w-5 h-5" />
              </div>
              <h3 className="text-lg font-bold text-white">Own Voice for Windows</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Floating obsidian pill that floats anywhere on Windows. Press <code>F8</code> in any application (VS Code, Word, Slack, WhatsApp) to dictate instantly.
              </p>
              <ul className="space-y-2 text-xs text-zinc-300 pt-2">
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> System-wide global hotkey (F8)
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Types directly at your active cursor
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> ARM64 & Intel/AMD optimized
                </li>
              </ul>
            </div>

            <a
              href="/releases/OwnVoice-v1.0.0-Windows-ARM64.zip"
              className="w-full py-3 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all border border-white/10"
            >
              <Download className="w-4 h-4" />
              Download Windows Client (.zip)
            </a>
          </div>
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
