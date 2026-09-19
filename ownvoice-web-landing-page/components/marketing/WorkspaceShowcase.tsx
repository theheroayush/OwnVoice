"use client";

import { Mic, Clock, Star, Settings, Check, Sparkles, Sliders, Layers } from "lucide-react";

export function WorkspaceShowcase() {
  return (
    <section className="py-24 px-4 sm:px-6 relative overflow-hidden">
      {/* Blue ambient glow behind the app mockup */}
      <div className="absolute top-1/2 -left-20 w-96 h-96 bg-blue-600/20 rounded-full blur-[110px] pointer-events-none" />

      <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
        {/* Left Side: Desktop App Workspace Mockup */}
        <div className="lg:col-span-7 relative">
          <div className="rounded-2xl border border-white/[0.12] bg-[#0c0f17]/95 shadow-[0_25px_60px_rgba(0,0,0,0.8),0_0_30px_rgba(59,130,246,0.15)] backdrop-blur-2xl overflow-hidden">
            {/* Window Titlebar */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-white/[0.08] bg-[#090c13]/80">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-red-500/80" />
                <div className="w-2.5 h-2.5 rounded-full bg-yellow-500/80" />
                <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/80" />
                <span className="text-xs font-semibold text-zinc-300 ml-2">Own Voice</span>
              </div>
              <div className="text-[11px] text-zinc-500 font-mono flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                Saved • Today, 10:42 AM
              </div>
            </div>

            {/* Window Main Grid */}
            <div className="grid grid-cols-12 min-h-[380px]">
              {/* Left mini-sidebar */}
              <div className="col-span-3 border-r border-white/[0.06] p-3 flex flex-col justify-between bg-[#080b11]/70">
                <div className="space-y-1.5">
                  <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-blue-600/20 text-blue-400 text-xs font-medium border border-blue-500/20">
                    <Mic className="w-3.5 h-3.5" />
                    <span>Home</span>
                  </div>
                  <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-zinc-400 hover:text-white text-xs font-medium transition-colors">
                    <Clock className="w-3.5 h-3.5" />
                    <span>History</span>
                  </div>
                  <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-zinc-400 hover:text-white text-xs font-medium transition-colors">
                    <Star className="w-3.5 h-3.5" />
                    <span>Favorites</span>
                  </div>
                  <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-zinc-400 hover:text-white text-xs font-medium transition-colors">
                    <Settings className="w-3.5 h-3.5" />
                    <span>Settings</span>
                  </div>
                </div>

                <div className="pt-4 border-t border-white/[0.06]">
                  <div className="text-[10px] text-zinc-500 font-mono">Connected:</div>
                  <div className="text-[11px] font-semibold text-zinc-300 truncate">OpenAI / GPT-4o</div>
                </div>
              </div>

              {/* Main Content Area */}
              <div className="col-span-9 p-5 flex flex-col justify-between bg-[#0b0e16]/80">
                <div className="space-y-3">
                  <h4 className="text-sm font-semibold text-white tracking-tight border-b border-white/[0.06] pb-2">
                    Subject: Project Timeline Update
                  </h4>

                  <div className="text-xs text-zinc-300 leading-relaxed space-y-2">
                    <p className="text-zinc-400">Hi [Client Name],</p>
                    <p>
                      I wanted to let you know that we&apos;ll need approximately two additional days to complete the integration. The work is taking a bit longer than expected, but we&apos;re on track and will keep you updated on the progress.
                    </p>
                    <p className="pt-2 text-zinc-400">
                      Best regards,<br />
                      <span className="text-zinc-200 font-medium">Ayush</span>
                    </p>
                  </div>
                </div>

                {/* Quick Actions Bar */}
                <div className="pt-4 border-t border-white/[0.06] space-y-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <button className="px-3 py-1 rounded-full bg-blue-600 text-white font-medium text-xs flex items-center gap-1 shadow-sm">
                      <Sparkles className="w-3 h-3" />
                      AI Fix
                    </button>
                    <button className="px-3 py-1 rounded-full bg-zinc-800 text-zinc-300 hover:text-white text-xs transition-colors border border-white/5">
                      Rewrite
                    </button>
                    <button className="px-3 py-1 rounded-full bg-zinc-800 text-zinc-300 hover:text-white text-xs transition-colors border border-white/5">
                      Shorten
                    </button>
                    <button className="px-3 py-1 rounded-full bg-zinc-800 text-zinc-300 hover:text-white text-xs transition-colors border border-white/5">
                      Expand
                    </button>
                  </div>

                  <div className="flex items-center gap-2 text-[11px] text-zinc-400 flex-wrap">
                    <span className="px-2.5 py-0.5 rounded-md bg-blue-950/40 text-blue-300 border border-blue-500/20 font-medium">
                      Professional
                    </span>
                    <span className="px-2.5 py-0.5 rounded-md bg-zinc-900 text-zinc-400 border border-white/5 hover:text-zinc-200 cursor-pointer">
                      Casual
                    </span>
                    <span className="px-2.5 py-0.5 rounded-md bg-zinc-900 text-zinc-400 border border-white/5 hover:text-zinc-200 cursor-pointer">
                      Friendly
                    </span>
                    <span className="text-zinc-500 hover:text-zinc-300 cursor-pointer">
                      More +
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side: Copy & Benefits */}
        <div className="lg:col-span-5 space-y-6 lg:pl-6 text-left">
          <div className="inline-flex items-center px-3 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            A closer look
          </div>

          <h2 className="text-4xl sm:text-5xl font-bold tracking-tight text-white leading-[1.12]">
            A workspace<br />
            designed for voice.
          </h2>

          <p className="text-base text-zinc-400 leading-relaxed">
            Minimal, fast and focused. Everything you need, exactly where you need it.
          </p>

          <div className="space-y-5 pt-2">
            {/* Feature 1 */}
            <div className="flex items-start gap-4">
              <div className="w-8 h-8 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center flex-shrink-0 mt-0.5">
                <Layers className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-white">Clean, distraction-free interface</h3>
                <p className="text-xs sm:text-sm text-zinc-400">Dark and light modes</p>
              </div>
            </div>

            {/* Feature 2 */}
            <div className="flex items-start gap-4">
              <div className="w-8 h-8 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center flex-shrink-0 mt-0.5">
                <Sliders className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-white">Quick actions (rewrite, shorten, expand)</h3>
                <p className="text-xs sm:text-sm text-zinc-400">Keyboard shortcuts</p>
              </div>
            </div>

            {/* Feature 3 */}
            <div className="flex items-start gap-4">
              <div className="w-8 h-8 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center flex-shrink-0 mt-0.5">
                <Clock className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-white">Access your full history</h3>
                <p className="text-xs sm:text-sm text-zinc-400">Export and use anywhere.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
