"use client";

export function ProviderRibbon() {
  return (
    <section className="py-14 border-y border-white/[0.04] bg-[#07080b]/50">
      <div className="max-w-5xl mx-auto px-4 text-center space-y-5">
        <p className="text-[11px] uppercase tracking-[0.2em] text-zinc-500 font-semibold font-mono">
          WORKS WITH YOUR FAVOURITE AI PROVIDERS
        </p>

        <div className="flex items-center justify-center gap-6 sm:gap-10 flex-wrap">
          {/* OpenAI */}
          <div className="flex items-center gap-2 text-zinc-300 hover:text-white transition-colors">
            <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
              <path d="M22.28 10.37a5.58 5.58 0 0 0-.47-4.52 5.67 5.67 0 0 0-4.9-2.73c-.3 0-.6.03-.89.09A5.62 5.62 0 0 0 11.5 1a5.67 5.67 0 0 0-5.18 3.39 5.56 5.56 0 0 0-3.6 2.6 5.64 5.64 0 0 0 .52 6.55 5.58 5.58 0 0 0 .47 4.52 5.67 5.67 0 0 0 4.9 2.73c.3 0 .6-.03.89-.09A5.62 5.62 0 0 0 13.5 23a5.67 5.67 0 0 0 5.18-3.39 5.56 5.56 0 0 0 3.6-2.6 5.64 5.64 0 0 0-.52-6.55z" />
            </svg>
            <span className="text-sm font-semibold tracking-tight">OpenAI</span>
          </div>

          {/* Anthropic */}
          <div className="flex items-center gap-2 text-zinc-300 hover:text-white transition-colors">
            <div className="w-5 h-5 rounded-md bg-amber-500/20 text-amber-300 font-bold text-[10px] flex items-center justify-center">
              AI
            </div>
            <span className="text-sm font-semibold tracking-tight">Anthropic</span>
          </div>

          {/* Google */}
          <div className="flex items-center gap-2 text-zinc-300 hover:text-white transition-colors">
            <div className="w-5 h-5 rounded-full bg-white/10 flex items-center justify-center text-xs font-black">
              <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 via-rose-400 to-amber-300">
                G
              </span>
            </div>
            <span className="text-sm font-semibold tracking-tight">Google</span>
          </div>

          {/* OpenRouter */}
          <div className="flex items-center gap-2 text-zinc-300 hover:text-white transition-colors">
            <div className="w-5 h-5 rounded-md bg-indigo-500/20 text-indigo-300 flex items-center justify-center">
              <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="12" cy="12" r="3" />
                <path d="M12 3v3m0 12v3M3 12h3m12 0h3" />
              </svg>
            </div>
            <span className="text-sm font-semibold tracking-tight">OpenRouter</span>
          </div>

          {/* More coming soon pill */}
          <div className="px-3 py-1 rounded-full bg-zinc-900 border border-white/10 text-zinc-400 text-xs font-medium">
            More coming soon
          </div>
        </div>
      </div>
    </section>
  );
}
