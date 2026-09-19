import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { ShieldCheck, Lock, KeyRound, Database, EyeOff, AlertTriangle } from "lucide-react";

export default function SecurityPage() {
  const threatModel = [
    {
      risk: "Risk 1: Accidental Frontend Exposure",
      solution: "Raw keys are never sent back to the browser after initial configuration. The UI only displays a masked suffix hint (e.g. ••••••••8F21).",
      icon: EyeOff,
    },
    {
      risk: "Risk 2: Database Breach Protection",
      solution: "All provider API keys are encrypted at rest using AES-256-GCM with PBKDF2 derived keys, unique 128-bit initialization vectors, and authentication tags.",
      icon: Lock,
    },
    {
      risk: "Risk 3: Audio Snooping & Cloud Storage",
      solution: "We follow an Ephemeral Audio Lifecycle. Audio buffers are kept in-memory only during transcription and immediately released. No permanent raw audio files exist on our servers.",
      icon: Database,
    },
    {
      risk: "Risk 4: Accidental Telemetry & Log Capture",
      solution: "All internal API request logs, audit events, and telemetry pipelines strictly redact authentication tokens and keys.",
      icon: KeyRound,
    },
  ];

  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-4xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-16">
        <div className="text-center space-y-3 max-w-2xl mx-auto">
          <span className="text-xs font-mono uppercase tracking-wider text-emerald-400 font-semibold">
            Security & Threat Model
          </span>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            Honest, Verifiable Security
          </h1>
          <p className="text-sm sm:text-base text-zinc-400">
            We make zero fake claims. We explain the exact mechanics of how your data and API credentials are protected.
          </p>
        </div>

        <div className="p-6 rounded-2xl bg-amber-950/20 border border-amber-500/20 space-y-2 text-xs text-amber-300/90 leading-relaxed">
          <span className="font-bold flex items-center gap-1.5 text-amber-400">
            <AlertTriangle className="w-4 h-4" />
            Our No-Fake-Privacy Guarantee
          </span>
          <p>
            Many AI apps claim "100% private" or "zero third-party data access" while secretly piping your audio to cloud APIs. When you use Own Voice with BYOK, your speech and prompts are sent directly from our secure proxy to your chosen provider (OpenAI, Google, Anthropic, or Groq) using your own account. You govern your own data retention agreement with your AI provider.
          </p>
        </div>

        <div className="space-y-4">
          <h2 className="text-lg font-bold text-white">The Own Voice Threat Model</h2>
          <div className="grid grid-cols-1 gap-4">
            {threatModel.map((item, idx) => {
              const Icon = item.icon;
              return (
                <div key={idx} className="p-5 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-2">
                  <div className="flex items-center gap-2 text-sm font-semibold text-white">
                    <Icon className="w-4 h-4 text-blue-400" />
                    <span>{item.risk}</span>
                  </div>
                  <p className="text-xs text-zinc-400 leading-relaxed pl-6">
                    {item.solution}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
