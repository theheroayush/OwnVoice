import Link from "next/link";
import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { InteractiveDemo } from "@/components/marketing/InteractiveDemo";
import { PricingCalculator } from "@/components/marketing/PricingCalculator";
import {
  Mic,
  KeyRound,
  ShieldCheck,
  Zap,
  ArrowRight,
  CheckCircle2,
  Lock,
  Layers,
  Sparkles,
  ChevronDown,
  Terminal,
  Mail,
  MessageSquare,
  Users,
  Code,
  PenTool,
} from "lucide-react";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />

      {/* SECTION 1: HERO (PRD Section 27) */}
      <section className="relative pt-20 pb-16 sm:pt-28 sm:pb-24 px-4 sm:px-6 overflow-hidden border-b border-white/5">
        <div className="max-w-4xl mx-auto text-center space-y-7 relative z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wide">
            <Sparkles className="w-3.5 h-3.5" />
            <span>One-Time Software Purchase • Bring Your Own Key</span>
          </div>

          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white leading-[1.08]">
            Own your voice. <br />
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 via-indigo-300 to-sky-400">
              Own your AI.
            </span>
          </h1>

          <p className="text-base sm:text-xl text-zinc-400 max-w-2xl mx-auto leading-relaxed font-normal">
            Speak naturally. Own Voice turns your spoken thoughts into finished emails, messages, notes, and code. No monthly subscriptions. Connect your own API keys.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            <Link
              href="/pricing"
              className="w-full sm:w-auto px-7 py-3.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm flex items-center justify-center gap-2 transition-all shadow-xl hover:shadow-blue-500/25 active:scale-98"
            >
              Buy Own Voice Lifetime
              <ArrowRight className="w-4 h-4" />
            </Link>
            <Link
              href="#demo"
              className="w-full sm:w-auto px-6 py-3.5 rounded-xl bg-zinc-900/80 hover:bg-zinc-800 text-zinc-300 hover:text-white border border-white/10 font-semibold text-sm flex items-center justify-center gap-2 transition-all"
            >
              Watch Interactive Demo
            </Link>
          </div>

          {/* Social Proof / Guarantee */}
          <div className="pt-6 flex items-center justify-center gap-6 text-xs text-zinc-500 font-mono flex-wrap">
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Lifetime License
            </span>
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Zero SaaS Markup
            </span>
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> AES-256-GCM Encrypted
            </span>
          </div>
        </div>
      </section>

      {/* SECTION 2: INTERACTIVE PRODUCT DEMO (PRD Section 27) */}
      <section id="demo" className="py-20 px-4 sm:px-6 border-b border-white/5 bg-[#0c0c10]">
        <div className="max-w-4xl mx-auto space-y-8">
          <div className="text-center space-y-2">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Thought → Voice → Understanding → Finished Work
            </h2>
            <p className="text-xs sm:text-sm text-zinc-400 max-w-xl mx-auto">
              Not merely speech to text. See how raw human rambling transforms into ready-to-send output.
            </p>
          </div>

          <InteractiveDemo />
        </div>
      </section>

      {/* SECTION 3: THE PROBLEM (PRD Section 27) */}
      <section className="py-20 px-4 sm:px-6 border-b border-white/5">
        <div className="max-w-4xl mx-auto space-y-10">
          <div className="text-center space-y-2">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Stop paying every month for a tool you could own
            </h2>
            <p className="text-xs sm:text-sm text-zinc-400 max-w-xl mx-auto">
              Traditional AI subscriptions lock you into recurring credit card charges, artificial rate limits, and 500%+ inference markup.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="p-6 rounded-2xl bg-zinc-950/40 border border-white/5 space-y-4">
              <span className="text-xs font-mono uppercase tracking-wider text-rose-400 font-semibold">
                Traditional AI SaaS Model
              </span>
              <ul className="space-y-3 text-xs sm:text-sm text-zinc-400">
                <li className="flex items-start gap-2.5">
                  <span className="text-rose-400 font-bold">✕</span>
                  <span><strong>$240 to $360 every year:</strong> Continuous subscriptions that compound indefinitely.</span>
                </li>
                <li className="flex items-start gap-2.5">
                  <span className="text-rose-400 font-bold">✕</span>
                  <span><strong>Platform controls AI:</strong> You are locked to whatever model the vendor picks.</span>
                </li>
                <li className="flex items-start gap-2.5">
                  <span className="text-rose-400 font-bold">✕</span>
                  <span><strong>Usage limits & throttling:</strong> High-volume days trigger speed caps or extra fees.</span>
                </li>
              </ul>
            </div>

            <div className="p-6 rounded-2xl bg-blue-950/20 border border-blue-500/20 space-y-4">
              <span className="text-xs font-mono uppercase tracking-wider text-blue-400 font-semibold">
                The Own Voice Model
              </span>
              <ul className="space-y-3 text-xs sm:text-sm text-zinc-300">
                <li className="flex items-start gap-2.5">
                  <span className="text-emerald-400 font-bold">✓</span>
                  <span><strong>One-time purchase:</strong> Pay once, own the software forever. No subscriptions.</span>
                </li>
                <li className="flex items-start gap-2.5">
                  <span className="text-emerald-400 font-bold">✓</span>
                  <span><strong>Direct provider billing:</strong> You pay OpenAI or Groq wholesale prices (~$0.001 per note).</span>
                </li>
                <li className="flex items-start gap-2.5">
                  <span className="text-emerald-400 font-bold">✓</span>
                  <span><strong>Full model autonomy:</strong> Switch between GPT-4o, Claude 3.5, Gemini, or Llama 3.3 anytime.</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 4: HOW IT WORKS (PRD Section 27) */}
      <section className="py-20 px-4 sm:px-6 border-b border-white/5 bg-[#0b0b0e]">
        <div className="max-w-4xl mx-auto space-y-12">
          <div className="text-center space-y-2">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">How Own Voice Works</h2>
            <p className="text-xs sm:text-sm text-zinc-400">Three clean steps to finished output.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
              <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center text-sm font-bold font-mono">
                1
              </div>
              <h3 className="text-base font-semibold text-white">1. Speak Naturally</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Click or tap the microphone. Speak at 150+ WPM without worrying about structure, filler words, or punctuation.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
              <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center text-sm font-bold font-mono">
                2
              </div>
              <h3 className="text-base font-semibold text-white">2. Multi-Provider Intelligence</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Your connected AI provider transcribes and interprets your intent, applying your custom writing rules.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
              <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center text-sm font-bold font-mono">
                3
              </div>
              <h3 className="text-base font-semibold text-white">3. Finished Output</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Receive the finalized email, message, or document. Refine with single-click quick actions or copy instantly.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 5 & 6: BYOK & SUPPORTED PROVIDERS (PRD Section 27) */}
      <section className="py-20 px-4 sm:px-6 border-b border-white/5">
        <div className="max-w-4xl mx-auto space-y-12">
          <div className="text-center space-y-2">
            <span className="text-xs font-mono uppercase tracking-wider text-blue-400 font-semibold">
              Bring Your Own Key
            </span>
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Your AI. Your keys. Your choice.
            </h2>
            <p className="text-xs sm:text-sm text-zinc-400 max-w-xl mx-auto">
              We never hard-code you to one provider. Choose from the best AI engines in the world.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              {
                name: "OpenAI",
                desc: "Whisper audio speech-to-text and flagship GPT-4o intelligence.",
                badge: "Whisper + GPT-4o",
              },
              {
                name: "Google Gemini",
                desc: "Gemini 2.5 Flash with massive context and native multimodal audio.",
                badge: "Gemini 2.5 Flash",
              },
              {
                name: "Anthropic Claude",
                desc: "Claude 3.5 Sonnet for nuanced human prose and structured coding.",
                badge: "Claude 3.5 Sonnet",
              },
              {
                name: "Groq",
                desc: "Real-time Whisper Large V3 on LPUs with blazing 300+ tok/s output.",
                badge: "Whisper-v3 + Llama-3.3",
              },
            ].map((p) => (
              <div key={p.name} className="p-5 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
                <span className="text-xs font-mono px-2 py-0.5 rounded bg-blue-950/60 text-blue-400 border border-blue-800/40 font-semibold">
                  {p.badge}
                </span>
                <h3 className="text-base font-bold text-white">{p.name}</h3>
                <p className="text-xs text-zinc-400 leading-relaxed">{p.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* SECTION 7: TARGET USE CASES (PRD Section 3 & 27) */}
      <section className="py-20 px-4 sm:px-6 border-b border-white/5 bg-[#0b0b0e]">
        <div className="max-w-4xl mx-auto space-y-12">
          <div className="text-center space-y-2">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">Built for Builders</h2>
            <p className="text-xs sm:text-sm text-zinc-400">
              Designed specifically for founders, developers, creators, and knowledge workers.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 flex gap-4">
              <div className="w-10 h-10 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center shrink-0">
                <Users className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h3 className="text-sm font-bold text-white">Founders & Executives</h3>
                <p className="text-xs text-zinc-400 leading-relaxed">
                  Draft executive client emails, capture product roadmaps between calls, and respond to WhatsApp messages in seconds.
                </p>
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 flex gap-4">
              <div className="w-10 h-10 rounded-xl bg-emerald-600/20 text-emerald-400 flex items-center justify-center shrink-0">
                <Code className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h3 className="text-sm font-bold text-white">Software Engineers</h3>
                <p className="text-xs text-zinc-400 leading-relaxed">
                  Dictate commit messages, describe architecture bugs, and draft complex system prompts without typing fatigue.
                </p>
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 flex gap-4">
              <div className="w-10 h-10 rounded-xl bg-purple-600/20 text-purple-400 flex items-center justify-center shrink-0">
                <PenTool className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h3 className="text-sm font-bold text-white">Writers & Creators</h3>
                <p className="text-xs text-zinc-400 leading-relaxed">
                  Speak raw scripts, LinkedIn posts, and newsletter drafts. Let Own Voice clean the flow while preserving your unique voice.
                </p>
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 flex gap-4">
              <div className="w-10 h-10 rounded-xl bg-amber-600/20 text-amber-400 flex items-center justify-center shrink-0">
                <Sparkles className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h3 className="text-sm font-bold text-white">Knowledge Workers</h3>
                <p className="text-xs text-zinc-400 leading-relaxed">
                  Convert messy meeting brain-dumps into crisp action items, executive summaries, and organized project documents.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 8: PRICING (PRD Section 27) */}
      <section className="py-20 px-4 sm:px-6 border-b border-white/5">
        <div className="max-w-4xl mx-auto space-y-10">
          <div className="text-center space-y-2">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">Simple Lifetime Pricing</h2>
            <p className="text-xs sm:text-sm text-zinc-400">
              One product. One-time purchase. No subscription treadmill.
            </p>
          </div>

          <PricingCalculator />
        </div>
      </section>

      {/* SECTION 9: PRIVACY & SECURITY (PRD Section 2 & 35) */}
      <section className="py-20 px-4 sm:px-6 border-b border-white/5 bg-[#0b0b0e]">
        <div className="max-w-4xl mx-auto space-y-10">
          <div className="text-center space-y-2">
            <span className="text-xs font-mono uppercase tracking-wider text-emerald-400 font-semibold">
              Transparent Architecture
            </span>
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              No Fake Privacy Claims
            </h2>
            <p className="text-xs sm:text-sm text-zinc-400 max-w-xl mx-auto">
              We never claim "zero cloud" or "100% offline" when using cloud AI. Here is our exact, audited security model.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
              <Lock className="w-6 h-6 text-blue-400" />
              <h3 className="text-sm font-bold text-white">AES-256-GCM Encryption</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                API keys are encrypted at rest using authenticated AES-256-GCM. Raw keys are never stored in plaintext and never logged.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
              <Zap className="w-6 h-6 text-emerald-400" />
              <h3 className="text-sm font-bold text-white">Ephemeral Audio Lifecycle</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Raw audio is processed in-memory for transcription and immediately released. We never permanently store your voice recordings.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
              <ShieldCheck className="w-6 h-6 text-purple-400" />
              <h3 className="text-sm font-bold text-white">Full User Deletion Rights</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Delete any individual transcript, your entire history, or your full account with a single click. Deletion is instantaneous.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 10: FAQ (PRD Section 27) */}
      <section className="py-20 px-4 sm:px-6 border-b border-white/5">
        <div className="max-w-3xl mx-auto space-y-10">
          <div className="text-center space-y-2">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Frequently Asked Questions
            </h2>
            <p className="text-xs sm:text-sm text-zinc-400">
              Clear answers to the most common questions about Own Voice and BYOK.
            </p>
          </div>

          <div className="space-y-4">
            {[
              {
                q: "What does BYOK (Bring Your Own Key) mean?",
                a: "Instead of paying Own Voice a high monthly subscription to cover AI costs, you get an API key directly from OpenAI, Google, Anthropic, or Groq. Own Voice connects to that key. You pay the provider directly at raw wholesale prices (fractions of a cent per voice note).",
              },
              {
                q: "How much do the AI provider API calls cost?",
                a: "Extremely little. A typical 30-second speech transcription and GPT-4o formatting costs roughly $0.001 to $0.003. If you record 100 voice notes a day, your monthly API bill is usually under $1.50.",
              },
              {
                q: "Is this a one-time purchase or a subscription?",
                a: "Own Voice is a one-time purchase software license. You buy it once and get lifetime access to the desktop web app, updates, and features.",
              },
              {
                q: "Are my API keys secure?",
                a: "Yes. All API keys are encrypted at rest using industry-standard AES-256-GCM with distinct initialization vectors and authentication tags. They are only decrypted in-memory when sending requests to your chosen provider.",
              },
              {
                q: "Do you store my voice recordings?",
                a: "No. In accordance with our Ephemeral Audio Policy, audio is processed in-memory for transcription and immediately discarded. Only the text transcript and generated output are saved in your account history.",
              },
            ].map((faq, idx) => (
              <div key={idx} className="p-5 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-2">
                <h3 className="text-sm font-semibold text-white">{faq.q}</h3>
                <p className="text-xs text-zinc-400 leading-relaxed">{faq.a}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* SECTION 11: FINAL CTA (PRD Section 27) */}
      <section className="py-24 px-4 sm:px-6 text-center space-y-8">
        <div className="max-w-2xl mx-auto space-y-4">
          <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            Stop renting your tools.
          </h2>
          <p className="text-sm sm:text-base text-zinc-400">
            Own the software. Bring your own AI. Speak your thoughts into finished work.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link
            href="/signup"
            className="w-full sm:w-auto px-8 py-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-xl hover:shadow-blue-500/30 active:scale-98"
          >
            Get Own Voice Lifetime Access
            <ArrowRight className="w-4 h-4" />
          </Link>
          <Link
            href="/docs"
            className="w-full sm:w-auto px-6 py-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-zinc-300 hover:text-white border border-white/10 font-semibold text-sm transition-all"
          >
            Read Documentation
          </Link>
        </div>
      </section>

      <MarketingFooter />
    </div>
  );
}
