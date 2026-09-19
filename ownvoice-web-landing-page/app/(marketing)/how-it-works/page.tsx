import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { Mic, ArrowDown, Cpu, FileCheck, ShieldAlert, CheckCircle2, ArrowRight } from "lucide-react";
import Link from "next/link";

export default function HowItWorksPage() {
  const steps = [
    {
      num: "01",
      title: "Audio Capture & Waveform Stream",
      desc: "When you speak, the browser captures raw PCM audio through the Web Audio API with active noise suppression. A live VU meter visualizes your vocal frequencies.",
    },
    {
      num: "02",
      title: "Audio Validation & Ephemeral Ingestion",
      desc: "Empty recordings and silent background noise are rejected. The valid audio buffer is streamed securely over TLS to the transcription pipeline.",
    },
    {
      num: "03",
      title: "Speech Transcription (Whisper / Gemini / Groq)",
      desc: "Your chosen AI provider performs high-precision acoustic speech recognition. As soon as the raw transcript is generated, the audio buffer is completely purged from memory.",
    },
    {
      num: "04",
      title: "Intent Parsing & Writing Style Injection",
      desc: "Own Voice evaluates your requested output format (Email, WhatsApp message, Meeting summary, Code) and injects your custom writing style instructions.",
    },
    {
      num: "05",
      title: "Production Output & Quick Actions",
      desc: "The final work is rendered in your workspace. You can refine it with 1-click pills (Professional, Casual, Shorten, Expand) or copy it directly into any app.",
    },
  ];

  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-4xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-16">
        <div className="text-center space-y-3">
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            The Voice Pipeline
          </h1>
          <p className="text-sm sm:text-base text-zinc-400 max-w-xl mx-auto">
            From raw vocal chords to finished work in under two seconds. Here is the technical flow.
          </p>
        </div>

        <div className="space-y-6">
          {steps.map((step, idx) => (
            <div
              key={step.num}
              className="p-6 sm:p-7 rounded-2xl bg-zinc-900/40 border border-white/5 flex flex-col sm:flex-row items-start sm:items-center gap-6"
            >
              <div className="text-3xl font-extrabold font-mono text-blue-400/80 shrink-0">
                {step.num}
              </div>
              <div className="space-y-1.5">
                <h3 className="text-base font-bold text-white">{step.title}</h3>
                <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed">{step.desc}</p>
              </div>
            </div>
          ))}
        </div>

        <div className="p-8 rounded-2xl bg-[#121215] border border-white/10 text-center space-y-4">
          <h3 className="text-xl font-bold text-white">Ready to test it yourself?</h3>
          <p className="text-xs text-zinc-400">
            Set up your account and connect your first AI key in under 2 minutes.
          </p>
          <Link
            href="/signup"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs transition-all shadow-lg active:scale-95"
          >
            Start with Own Voice
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
