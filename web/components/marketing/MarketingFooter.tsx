import Link from "next/link";
import { Mic, ShieldCheck, KeyRound, Sparkles } from "lucide-react";

export function MarketingFooter() {
  return (
    <footer className="w-full border-t border-white/5 bg-[#09090b] text-zinc-400 text-xs py-14">
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-12">
          {/* Col 1: Brand */}
          <div className="md:col-span-1 space-y-3">
            <div className="flex items-center gap-2 text-white font-medium text-sm">
              <Mic className="w-4 h-4 text-blue-400" />
              Own Voice
            </div>
            <p className="text-zinc-500 leading-relaxed text-xs">
              Voice-first AI workspace. Speak naturally, turn thoughts into finished work. Bring your own keys and pay providers directly.
            </p>
            <div className="flex items-center gap-2 text-[11px] text-zinc-500 pt-2">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
              <span>AES-256-GCM Encrypted at rest</span>
            </div>
          </div>

          {/* Col 2: Product */}
          <div className="space-y-2.5">
            <h4 className="font-semibold text-zinc-300 uppercase tracking-wider text-[11px]">Product</h4>
            <ul className="space-y-2">
              <li><Link href="/features" className="hover:text-white transition-colors">Features</Link></li>
              <li><Link href="/how-it-works" className="hover:text-white transition-colors">How It Works</Link></li>
              <li><Link href="/pricing" className="hover:text-white transition-colors">Lifetime Pricing</Link></li>
              <li><Link href="/download" className="hover:text-white transition-colors">Desktop App (Windows/Mac)</Link></li>
            </ul>
          </div>

          {/* Col 3: BYOK & Security */}
          <div className="space-y-2.5">
            <h4 className="font-semibold text-zinc-300 uppercase tracking-wider text-[11px]">Architecture</h4>
            <ul className="space-y-2">
              <li><Link href="/security" className="hover:text-white transition-colors">BYOK Threat Model</Link></li>
              <li><Link href="/docs/connecting-api-key" className="hover:text-white transition-colors">Supported Providers</Link></li>
              <li><Link href="/docs/privacy" className="hover:text-white transition-colors">Zero Fake Privacy Claims</Link></li>
              <li><Link href="/security#retention" className="hover:text-white transition-colors">Ephemeral Audio Retention</Link></li>
            </ul>
          </div>

          {/* Col 4: Documentation */}
          <div className="space-y-2.5">
            <h4 className="font-semibold text-zinc-300 uppercase tracking-wider text-[11px]">Documentation</h4>
            <ul className="space-y-2">
              <li><Link href="/docs" className="hover:text-white transition-colors">Getting Started</Link></li>
              <li><Link href="/docs/providers" className="hover:text-white transition-colors">Provider Comparison</Link></li>
              <li><Link href="/faq" className="hover:text-white transition-colors">Frequently Asked Questions</Link></li>
              <li><Link href="/login" className="hover:text-white transition-colors">User Portal</Link></li>
            </ul>
          </div>
        </div>

        <div className="pt-8 border-t border-white/5 flex flex-col sm:flex-row items-center justify-between gap-4 text-zinc-600 text-[11px]">
          <p>© {new Date().getFullYear()} Own Voice. One-time software license. All rights reserved.</p>
          <p className="flex items-center gap-1.5">
            <KeyRound className="w-3 h-3 text-zinc-500" />
            AI usage inference is billed directly by your connected provider (OpenAI, Google, Anthropic, Groq).
          </p>
        </div>
      </div>
    </footer>
  );
}
