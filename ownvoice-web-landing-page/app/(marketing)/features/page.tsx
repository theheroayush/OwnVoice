import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { Mic, Sparkles, KeyRound, Zap, Shield, Wand2, ArrowRight } from "lucide-react";
import Link from "next/link";

export default function FeaturesPage() {
  const features = [
    {
      icon: Mic,
      title: "Voice-First Input Engine",
      desc: "Instant microphone capture with low-latency Web Audio API and fallback. Pause, resume, and retry freely.",
    },
    {
      icon: Wand2,
      title: "Zero-Filler Polishing",
      desc: "Automatically strips out 'ums', 'ahs', repetitions, and false starts without altering your original meaning.",
    },
    {
      icon: KeyRound,
      title: "True BYOK Multi-Provider",
      desc: "Connect OpenAI Whisper + GPT-4o, Google Gemini 2.5 Flash, Anthropic Claude 3.5, or Groq LPUs.",
    },
    {
      icon: Sparkles,
      title: "Context & Intent Classification",
      desc: "Converts thoughts into emails with subjects, WhatsApp chats, bulleted summaries, or code snippets.",
    },
    {
      icon: Zap,
      title: "Custom Writing Style Rules",
      desc: "Enforce your personal writing rules across all outputs: concise phrasing, natural tone, or Indian English nuances.",
    },
    {
      icon: Shield,
      title: "AES-256-GCM Cryptographic Security",
      desc: "Your keys are encrypted at rest with distinct IVs. Audio is deleted ephemerally as soon as transcription finishes.",
    },
  ];

  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-16">
        <div className="text-center space-y-3 max-w-2xl mx-auto">
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            Engineered for Thought-Speed
          </h1>
          <p className="text-sm sm:text-base text-zinc-400">
            Every layer of Own Voice is designed to convert spoken thoughts into production-ready work faster than typing.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((f, i) => {
            const Icon = f.icon;
            return (
              <div key={i} className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-3">
                <div className="w-10 h-10 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center">
                  <Icon className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-white">{f.title}</h3>
                <p className="text-xs text-zinc-400 leading-relaxed">{f.desc}</p>
              </div>
            );
          })}
        </div>

        <div className="p-8 rounded-2xl bg-[#121215] border border-white/10 text-center space-y-4">
          <h3 className="text-xl font-bold text-white">Experience the difference today</h3>
          <p className="text-xs text-zinc-400 max-w-md mx-auto">
            One-time purchase software license. No monthly subscription.
          </p>
          <Link
            href="/signup"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs transition-all shadow-lg active:scale-95"
          >
            Get Lifetime Access
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
