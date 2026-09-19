"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";

export function MarketingNavbar() {
  return (
    <header className="sticky top-0 z-50 w-full border-b border-white/[0.06] bg-[#07080b]/80 backdrop-blur-xl">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link href="/" className="flex items-center gap-2.5 group">
          <div className="w-7 h-7 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 group-hover:bg-blue-600/30 transition-all shadow-[0_0_12px_rgba(59,130,246,0.3)]">
            <svg
              className="w-4 h-4 text-blue-400 fill-current"
              viewBox="0 0 24 24"
            >
              <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" />
            </svg>
          </div>
          <span className="font-bold text-base tracking-tight text-white">
            Own Voice
          </span>
        </Link>

        {/* Navigation Links */}
        <nav className="hidden md:flex items-center gap-6 text-xs lg:text-sm font-medium text-zinc-400">
          <Link href="#demo" className="hover:text-blue-400 transition-colors">
            Live Demo
          </Link>
          <Link href="#features" className="hover:text-white transition-colors">
            Features
          </Link>
          <Link href="#comparison" className="hover:text-white transition-colors">
            Compare & Save
          </Link>
          <Link href="#byok-guide" className="hover:text-white transition-colors">
            BYOK Guide
          </Link>
          <Link href="#pricing" className="hover:text-white transition-colors">
            Pricing
          </Link>
          <Link href="#faq" className="hover:text-white transition-colors">
            FAQ
          </Link>
        </nav>

        {/* CTA Actions */}
        <div className="flex items-center gap-4">
          <Link
            href="/login"
            className="text-sm font-medium text-zinc-300 hover:text-white transition-colors"
          >
            Sign in
          </Link>
          <Link
            href="#pricing"
            className="inline-flex items-center gap-1.5 text-xs font-semibold px-4 py-2 rounded-full bg-white hover:bg-zinc-200 text-black transition-all shadow-sm active:scale-95"
          >
            Get Own Voice
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </header>
  );
}

