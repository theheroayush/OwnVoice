"use client";

import { useState } from "react";
import Link from "next/link";
import { Zap, Lock, Monitor, Youtube, Linkedin } from "lucide-react";

export function MarketingFooter() {
  const [email, setEmail] = useState("");
  const [subscribed, setSubscribed] = useState(false);

  const handleSubscribe = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;
    setSubscribed(true);
    setTimeout(() => {
      setEmail("");
      setSubscribed(false);
    }, 3500);
  };

  return (
    <footer className="w-full border-t border-white/[0.08] bg-[#07090e] text-[#a1a1aa] pt-16 pb-12 px-6 sm:px-10 lg:px-12 font-sans selection:bg-blue-600/30 selection:text-blue-200">
      <div className="max-w-[1360px] mx-auto">
        {/* TOP SECTION: 3-PART GRID (Brand + 4 Nav Columns + Subscribe Card) */}
        <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-10 lg:gap-8 pb-14 border-b border-white/[0.06]">
          {/* 1. LEFT BRAND COLUMN (Span 4) */}
          <div className="md:col-span-4 lg:col-span-3 space-y-6">
            {/* Logo + Name */}
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-2xl bg-gradient-to-b from-blue-600 to-blue-700 p-2.5 flex items-center justify-center shadow-[0_0_20px_rgba(37,99,235,0.4)] flex-shrink-0">
                {/* Voice Waveform Bars Icon */}
                <div className="flex items-center gap-[3px] h-full justify-center w-full">
                  <span className="w-[3px] h-3 bg-white rounded-full" />
                  <span className="w-[3px] h-5 bg-white rounded-full" />
                  <span className="w-[3px] h-7 bg-white rounded-full shadow-[0_0_6px_#fff]" />
                  <span className="w-[3px] h-4 bg-white rounded-full" />
                  <span className="w-[3px] h-6 bg-white rounded-full" />
                  <span className="w-[3px] h-2.5 bg-white rounded-full" />
                </div>
              </div>

              <div>
                <h3 className="text-xl font-bold text-white tracking-tight leading-none">
                  Own Voice
                </h3>
                <p className="text-xs text-zinc-400 mt-1">Your voice. Your AI.</p>
              </div>
            </div>

            {/* Description */}
            <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed max-w-[280px]">
              A private voice-to-text app that helps you think, write and get more done — using your own AI key.
            </p>

            {/* 3 Key Feature Bullets */}
            <div className="space-y-4 pt-1">
              {/* Bullet 1: One-time payment */}
              <div className="flex items-start gap-3">
                <div className="w-5 h-5 flex items-center justify-center flex-shrink-0 mt-0.5 text-sky-400">
                  <Zap className="w-4 h-4 fill-sky-400 text-sky-400" />
                </div>
                <div>
                  <h4 className="text-xs sm:text-sm font-semibold text-white leading-tight">
                    One-time payment
                  </h4>
                  <p className="text-xs text-zinc-400 mt-0.5">No monthly fees</p>
                </div>
              </div>

              {/* Bullet 2: Private & secure */}
              <div className="flex items-start gap-3">
                <div className="w-5 h-5 flex items-center justify-center flex-shrink-0 mt-0.5 text-emerald-400">
                  <Lock className="w-4 h-4 text-emerald-400" />
                </div>
                <div>
                  <h4 className="text-xs sm:text-sm font-semibold text-white leading-tight">
                    Private &amp; secure
                  </h4>
                  <p className="text-xs text-zinc-400 mt-0.5">Your data stays with you</p>
                </div>
              </div>

              {/* Bullet 3: Works offline */}
              <div className="flex items-start gap-3">
                <div className="w-5 h-5 flex items-center justify-center flex-shrink-0 mt-0.5 text-indigo-400">
                  <Monitor className="w-4 h-4 text-indigo-400" />
                </div>
                <div>
                  <h4 className="text-xs sm:text-sm font-semibold text-white leading-tight">
                    Works offline
                  </h4>
                  <p className="text-xs text-zinc-400 mt-0.5">Use it anywhere</p>
                </div>
              </div>
            </div>
          </div>

          {/* 2. CENTER 4 NAVIGATION COLUMNS (Span 5) */}
          <div className="md:col-span-8 lg:col-span-6 grid grid-cols-2 sm:grid-cols-4 gap-6 sm:gap-4 pt-2">
            {/* Column 1: Product */}
            <div className="space-y-3.5">
              <h4 className="text-xs sm:text-sm font-semibold text-white tracking-wide">
                Product
              </h4>
              <ul className="space-y-2.5 text-xs sm:text-sm">
                <li>
                  <Link href="/download" className="hover:text-white transition-colors">
                    Desktop App
                  </Link>
                </li>
                <li>
                  <Link href="#features" className="hover:text-white transition-colors">
                    Features
                  </Link>
                </li>
                <li>
                  <Link href="#providers" className="hover:text-white transition-colors">
                    AI Models
                  </Link>
                </li>
                <li>
                  <Link href="#pricing" className="hover:text-white transition-colors">
                    Pricing
                  </Link>
                </li>
                <li>
                  <Link href="/download" className="hover:text-white transition-colors">
                    Download
                  </Link>
                </li>
                <li>
                  <Link href="/docs/changelog" className="hover:text-white transition-colors">
                    What&apos;s New
                  </Link>
                </li>
              </ul>
            </div>

            {/* Column 2: AI Models */}
            <div className="space-y-3.5">
              <h4 className="text-xs sm:text-sm font-semibold text-white tracking-wide">
                AI Models
              </h4>
              <ul className="space-y-2.5 text-xs sm:text-sm">
                <li>
                  <Link href="/docs/connecting-api-key" className="hover:text-white transition-colors">
                    OpenAI (Whisper)
                  </Link>
                </li>
                <li>
                  <Link href="/docs/connecting-api-key" className="hover:text-white transition-colors">
                    Google Gemini
                  </Link>
                </li>
                <li>
                  <Link href="/docs/connecting-api-key" className="hover:text-white transition-colors">
                    Anthropic (Claude)
                  </Link>
                </li>
                <li>
                  <Link href="/docs/connecting-api-key" className="hover:text-white transition-colors">
                    Groq
                  </Link>
                </li>
                <li>
                  <Link href="/docs/providers" className="hover:text-white transition-colors">
                    Compare Models
                  </Link>
                </li>
              </ul>
            </div>

            {/* Column 3: Resources */}
            <div className="space-y-3.5">
              <h4 className="text-xs sm:text-sm font-semibold text-white tracking-wide">
                Resources
              </h4>
              <ul className="space-y-2.5 text-xs sm:text-sm">
                <li>
                  <Link href="/docs/user-guide" className="hover:text-white transition-colors">
                    User Guide
                  </Link>
                </li>
                <li>
                  <Link href="#how-it-works" className="hover:text-white transition-colors">
                    Examples
                  </Link>
                </li>
                <li>
                  <Link href="/docs" className="hover:text-white transition-colors">
                    Help Center
                  </Link>
                </li>
                <li>
                  <Link href="#faq" className="hover:text-white transition-colors">
                    FAQs
                  </Link>
                </li>
                <li>
                  <Link href="/docs/blog" className="hover:text-white transition-colors">
                    Blog
                  </Link>
                </li>
              </ul>
            </div>

            {/* Column 4: Company */}
            <div className="space-y-3.5">
              <h4 className="text-xs sm:text-sm font-semibold text-white tracking-wide">
                Company
              </h4>
              <ul className="space-y-2.5 text-xs sm:text-sm">
                <li>
                  <Link href="/about" className="hover:text-white transition-colors">
                    About Us
                  </Link>
                </li>
                <li>
                  <Link href="/security" className="hover:text-white transition-colors">
                    Privacy Policy
                  </Link>
                </li>
                <li>
                  <Link href="/docs/terms" className="hover:text-white transition-colors">
                    Terms of Use
                  </Link>
                </li>
                <li>
                  <a href="mailto:support@ownvoice.ai" className="hover:text-white transition-colors">
                    Contact Support
                  </a>
                </li>
              </ul>
            </div>
          </div>

          {/* 3. RIGHT GET UPDATES CARD + FOLLOW US (Span 3) */}
          <div className="md:col-span-12 lg:col-span-3 space-y-6 flex flex-col items-start lg:items-end">
            {/* Get Updates Card */}
            <div className="w-full max-w-sm rounded-2xl border border-white/[0.08] bg-[#0c101a] p-5 shadow-xl">
              <h4 className="text-sm font-semibold text-white">Get updates</h4>
              <p className="text-xs text-zinc-400 mt-0.5 mb-3.5">
                New features, tips and more.
              </p>

              <form onSubmit={handleSubscribe} className="space-y-2.5">
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  required
                  className="w-full px-3.5 py-2.5 rounded-xl bg-[#080b12] border border-white/[0.1] text-xs sm:text-sm text-white placeholder:text-zinc-500 focus:outline-none focus:border-blue-500 transition-colors shadow-inner"
                />
                <button
                  type="submit"
                  disabled={subscribed}
                  className="w-full py-2.5 px-4 rounded-xl bg-[#1d70f5] hover:bg-blue-600 text-white font-medium text-xs sm:text-sm transition-all shadow-md active:scale-98 disabled:bg-emerald-600"
                >
                  {subscribed ? "Subscribed!" : "Subscribe"}
                </button>
              </form>

              <p className="text-[11px] text-zinc-500 text-center mt-3 font-normal">
                No spam. Only important updates.
              </p>
            </div>

            {/* Follow Us Section */}
            <div className="w-full max-w-sm space-y-2.5">
              <h5 className="text-xs font-semibold text-white">Follow us</h5>
              <div className="flex items-center gap-2.5">
                {/* X / Twitter */}
                <a
                  href="https://x.com"
                  target="_blank"
                  rel="noreferrer"
                  className="w-9 h-9 rounded-xl bg-white/[0.05] hover:bg-white/[0.1] border border-white/[0.08] flex items-center justify-center text-zinc-300 hover:text-white transition-all"
                  aria-label="X (Twitter)"
                >
                  <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                    <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                  </svg>
                </a>

                {/* YouTube */}
                <a
                  href="https://youtube.com"
                  target="_blank"
                  rel="noreferrer"
                  className="w-9 h-9 rounded-xl bg-white/[0.05] hover:bg-white/[0.1] border border-white/[0.08] flex items-center justify-center text-zinc-300 hover:text-white transition-all"
                  aria-label="YouTube"
                >
                  <Youtube className="w-4 h-4" />
                </a>

                {/* Discord */}
                <a
                  href="https://discord.com"
                  target="_blank"
                  rel="noreferrer"
                  className="w-9 h-9 rounded-xl bg-white/[0.05] hover:bg-white/[0.1] border border-white/[0.08] flex items-center justify-center text-zinc-300 hover:text-white transition-all"
                  aria-label="Discord"
                >
                  <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                    <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994.021-.041.001-.09-.041-.106a13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.929 1.793 8.18 1.793 12.061 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.894.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.028zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z" />
                  </svg>
                </a>

                {/* LinkedIn */}
                <a
                  href="https://linkedin.com"
                  target="_blank"
                  rel="noreferrer"
                  className="w-9 h-9 rounded-xl bg-white/[0.05] hover:bg-white/[0.1] border border-white/[0.08] flex items-center justify-center text-zinc-300 hover:text-white transition-all"
                  aria-label="LinkedIn"
                >
                  <Linkedin className="w-4 h-4" />
                </a>
              </div>
            </div>
          </div>
        </div>

        {/* BOTTOM SECTION: COPYRIGHT + LEGAL LINKS + PAYMENT METHOD BADGES */}
        <div className="pt-8 flex flex-col lg:flex-row items-center justify-between gap-6 text-xs text-zinc-400">
          {/* Copyright */}
          <div>
            <p>© 2026 Own Voice. All rights reserved.</p>
          </div>

          {/* Legal Links with Divider Pipes */}
          <div className="flex items-center gap-3 text-xs text-zinc-400 flex-wrap justify-center">
            <Link href="/security" className="hover:text-white transition-colors">
              Privacy
            </Link>
            <span className="text-zinc-600">|</span>
            <Link href="/docs/terms" className="hover:text-white transition-colors">
              Terms
            </Link>
            <span className="text-zinc-600">|</span>
            <Link href="/security#guarantee" className="hover:text-white transition-colors">
              Refund Policy
            </Link>
            <span className="text-zinc-600">|</span>
            <a href="mailto:support@ownvoice.ai" className="hover:text-white transition-colors">
              Contact
            </a>
          </div>

          {/* Secure Payments Badges */}
          <div className="flex items-center gap-2.5 flex-wrap justify-center">
            <span className="text-[11px] text-zinc-400 font-medium mr-1">
              Secure payments via
            </span>

            {/* 1. UPI Badge */}
            <div className="h-6 px-2 rounded-md bg-white flex items-center justify-center shadow-sm">
              <span className="text-[11px] font-black tracking-tight text-[#097938]">
                UP<span className="text-[#E76C24]">I</span>
              </span>
            </div>

            {/* 2. RuPay Badge */}
            <div className="h-6 px-2 rounded-md bg-white flex items-center justify-center shadow-sm">
              <span className="text-[11px] font-black tracking-tight text-[#0F4A8A]">
                RuPay<span className="text-[#E55B23]">▸</span>
              </span>
            </div>

            {/* 3. VISA Badge */}
            <div className="h-6 px-2.5 rounded-md bg-white flex items-center justify-center shadow-sm">
              <span className="text-[11px] font-black italic tracking-wider text-[#1434CB]">
                VISA
              </span>
            </div>

            {/* 4. Mastercard Badge */}
            <div className="h-6 px-2.5 rounded-md bg-white flex items-center justify-center gap-0.5 shadow-sm">
              <div className="w-3.5 h-3.5 rounded-full bg-[#EB001B]" />
              <div className="w-3.5 h-3.5 rounded-full bg-[#F79E1B] -ml-2 opacity-90" />
            </div>

            {/* 5. Apple Pay Badge */}
            <div className="h-6 px-2.5 rounded-md bg-white flex items-center justify-center gap-1 shadow-sm">
              <svg className="w-2.5 h-2.5 fill-black" viewBox="0 0 170 170">
                <path d="M150.37 130.25c-2.45 5.66-5.35 10.87-8.71 15.66-4.58 6.53-8.33 11.05-11.22 13.56-4.48 4.12-9.28 6.23-14.42 6.35-3.69 0-8.14-1.05-13.32-3.18-5.19-2.12-9.97-3.17-14.34-3.17-4.58 0-9.49 1.05-14.75 3.17-5.26 2.13-9.5 3.24-12.74 3.35-4.35.13-9.16-1.9-14.42-6.08-3.69-3.08-7.77-7.86-12.23-14.34-6.04-8.76-10.8-18.78-14.28-30.06-3.48-11.28-5.22-22.18-5.22-32.7 0-14.65 3.66-26.68 10.97-36.1 7.31-9.42 16.59-14.22 27.84-14.41 4.67 0 10.02 1.25 16.06 3.75 6.04 2.5 9.94 3.79 11.71 3.86 1.5.07 5.74-1.32 12.73-4.17 6.99-2.85 12.87-4.13 17.65-3.86 13.08.79 23.3 5.48 30.66 14.07-11.38 6.84-16.92 16.27-16.63 28.29.29 9.38 3.96 17.29 11.02 23.73 7.06 6.44 15.48 10.15 25.26 11.13-2.22 6.53-4.99 13.25-8.31 20.17zM119.22 31.84c0-7.39 2.65-14.35 7.95-20.89 5.3-6.54 11.83-10.45 19.59-11.73.22 1.45.33 2.76.33 3.93 0 7.25-2.76 14.31-8.28 21.18-5.52 6.87-12.18 10.8-19.98 11.78-.22-1.45-.33-2.87-.33-4.27z" />
              </svg>
              <span className="text-[10px] font-bold text-black tracking-tight">Pay</span>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
