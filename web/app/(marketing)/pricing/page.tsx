import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { PricingCalculator } from "@/components/marketing/PricingCalculator";
import { ShieldCheck, HelpCircle } from "lucide-react";

export default function PricingPage() {
  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-16">
        <div className="text-center space-y-3 max-w-2xl mx-auto">
          <span className="text-xs font-mono uppercase tracking-wider text-blue-400 font-semibold">
            One-Time Purchase Software License
          </span>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
            Pay once. Own forever.
          </h1>
          <p className="text-sm sm:text-base text-zinc-400">
            Own Voice eliminates the recurring SaaS fee. You purchase the software once and connect your own AI key.
          </p>
        </div>

        <PricingCalculator />

        {/* Detailed Breakdown FAQ */}
        <div className="max-w-3xl mx-auto space-y-4 pt-8 border-t border-white/5">
          <h3 className="text-base font-bold text-white mb-4">Why BYOK is financially superior</h3>
          <div className="p-5 rounded-2xl bg-zinc-900/40 border border-white/5 space-y-2 text-xs text-zinc-400 leading-relaxed">
            <p>
              Traditional AI tools charge $20 to $30 every month regardless of whether you speak 5 times or 500 times. Most of that fee is platform profit and markup.
            </p>
            <p>
              With BYOK, you pay OpenAI, Anthropic, or Groq directly at their raw API wholesale rates. A 30-second note costs roughly <strong>$0.001</strong>. Even power users rarely exceed $2.00/month in actual API fees.
            </p>
          </div>
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
