"use client";

import Link from "next/link";
import { Mic, Shield, Sparkles, ChevronRight } from "lucide-react";

export function MarketingNavbar() {
  return (
    <header className="sticky top-0 z-40 w-full border-b border-white/5 bg-[#09090b]/80 backdrop-blur-md">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link href="/" className="flex items-center gap-2.5 group">
          <div className="w-8 h-8 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 group-hover:bg-blue-600/30 transition-all">
            <Mic className="w-4 h-4" />
          </div>
          <span className="font-semibold text-base tracking-tight text-white flex items-center gap-1.5">
            Own Voice
            <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-zinc-800 text-zinc-400 border border-zinc-700">
              BYOK
            </span>
          </span>
        </Link>

        {/* Navigation Links */}
        <nav className="hidden md:flex items-center gap-7 text-sm font-medium text-zinc-400">
          <Link href="/features" className="hover:text-white transition-colors">
            Features
          </Link>
          <Link href="/how-it-works" className="hover:text-white transition-colors">
            How It Works
          </Link>
          <Link href="/pricing" className="hover:text-white transition-colors">
            Pricing
          </Link>
          <Link href="/security" className="hover:text-white transition-colors">
            Security & BYOK
          </Link>
          <Link href="/docs" className="hover:text-white transition-colors">
            Docs
          </Link>
        </nav>

        {/* CTA Actions */}
        <div className="flex items-center gap-3">
          <Link
            href="/login"
            className="text-sm font-medium text-zinc-300 hover:text-white px-3 py-1.5 transition-colors"
          >
            Sign in
          </Link>
          <Link
            href="/pricing"
            className="inline-flex items-center gap-1.5 text-xs font-semibold px-3.5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white transition-all shadow-sm hover:shadow-blue-500/25 active:scale-95"
          >
            Buy Lifetime
            <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </header>
  );
}
