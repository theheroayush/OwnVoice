import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";

export default function FAQPage() {
  const faqs = [
    {
      q: "What is Own Voice?",
      a: "Own Voice is an executive voice-first AI productivity application. You speak naturally at 150+ words per minute, and it transforms your thoughts into structured emails, messages, notes, and code. You purchase a lifetime software license and bring your own AI API keys.",
    },
    {
      q: "Why is BYOK (Bring Your Own Key) better than a subscription?",
      a: "SaaS companies charge $20 to $30 every month, mark up AI costs by up to 1000%, and apply artificial usage caps. With BYOK, you pay the AI provider directly at raw cost—usually less than $1.50 per month for heavy daily use.",
    },
    {
      q: "Which AI providers are supported?",
      a: "We currently support OpenAI (Whisper and GPT-4o models), Google Gemini (Gemini 2.5 Flash and 1.5 models), Anthropic (Claude 3.5 Sonnet and Haiku), and Groq (Whisper Large V3 and Llama 3.3). You can switch between them at any time.",
    },
    {
      q: "How are my API keys stored?",
      a: "All keys are encrypted at rest using AES-256-GCM. We never store them in plaintext, never log them, and never return the raw key to your browser after configuration.",
    },
    {
      q: "Do you keep my audio recordings?",
      a: "No. Raw audio is processed ephemerally in-memory for transcription and immediately purged. We never store your voice files permanently.",
    },
    {
      q: "Can I use Own Voice on my phone and desktop?",
      a: "Yes! Own Voice is accessible directly via any desktop or mobile browser, and can be installed as a PWA or desktop web application with microphone support.",
    },
    {
      q: "What if I need a refund or have an issue?",
      a: "We offer a 14-day no-questions-asked refund policy on lifetime license purchases. Simply contact support with your license key.",
    },
  ];

  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-3xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-12">
        <div className="text-center space-y-3">
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            Frequently Asked Questions
          </h1>
          <p className="text-sm text-zinc-400">
            Everything you need to know about Own Voice, lifetime licenses, and BYOK economics.
          </p>
        </div>

        <div className="space-y-4">
          {faqs.map((f, i) => (
            <div key={i} className="p-6 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-2">
              <h3 className="text-base font-semibold text-white">{f.q}</h3>
              <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed">{f.a}</p>
            </div>
          ))}
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
