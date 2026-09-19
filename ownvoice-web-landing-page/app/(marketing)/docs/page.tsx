import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import Link from "next/link";
import { BookOpen, KeyRound, Mic, ShieldCheck, HelpCircle, CreditCard, Wrench, ArrowRight } from "lucide-react";

export default function DocsIndexPage() {
  const docSections = [
    {
      slug: "getting-started",
      title: "Getting Started",
      desc: "Account creation, license activation, and speaking your first thought in under 2 minutes.",
      icon: BookOpen,
    },
    {
      slug: "connecting-api-key",
      title: "Connecting Your API Key (BYOK)",
      desc: "Step-by-step guides for obtaining and connecting keys from OpenAI, Gemini, Anthropic, and Groq.",
      icon: KeyRound,
    },
    {
      slug: "providers",
      title: "AI Providers & Model Selection",
      desc: "Deep comparison of OpenAI Whisper, Groq LPUs, Gemini 2.5 Flash, and Claude 3.5 Sonnet.",
      icon: Wrench,
    },
    {
      slug: "voice-settings",
      title: "Voice Settings & Writing Style",
      desc: "How to configure custom instructions, natural tone, and punctuation preferences.",
      icon: Mic,
    },
    {
      slug: "troubleshooting",
      title: "Troubleshooting & Edge Cases",
      desc: "Resolving microphone permission issues, provider 429 rate limits, and audio errors.",
      icon: HelpCircle,
    },
    {
      slug: "billing",
      title: "Lifetime License & Billing",
      desc: "Understanding one-time software licensing, upgrades, and how provider API billing works.",
      icon: CreditCard,
    },
    {
      slug: "privacy",
      title: "Data Privacy & Ephemeral Audio",
      desc: "Detailed architectural guarantees on AES-256-GCM encryption and zero audio persistence.",
      icon: ShieldCheck,
    },
  ];

  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-12">
        <div className="space-y-3">
          <span className="text-xs font-mono uppercase tracking-wider text-blue-400 font-semibold">
            Documentation & Guides
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
            Own Voice Documentation
          </h1>
          <p className="text-sm text-zinc-400 max-w-xl">
            Everything you need to configure your BYOK providers, master voice formatting, and manage your license.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {docSections.map((sec) => {
            const Icon = sec.icon;
            return (
              <Link
                key={sec.slug}
                href={`/docs/${sec.slug}`}
                className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 hover:border-white/15 hover:bg-zinc-800/40 transition-all space-y-3 group"
              >
                <div className="w-10 h-10 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center group-hover:scale-105 transition-transform">
                  <Icon className="w-5 h-5" />
                </div>
                <div className="space-y-1">
                  <h3 className="text-base font-bold text-white group-hover:text-blue-400 transition-colors flex items-center justify-between">
                    {sec.title}
                    <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                  </h3>
                  <p className="text-xs text-zinc-400 leading-relaxed">{sec.desc}</p>
                </div>
              </Link>
            );
          })}
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
