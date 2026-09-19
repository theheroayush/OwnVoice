import Link from "next/link";
import { ArrowRight, Sparkles, Ban, KeyRound, Monitor } from "lucide-react";
import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { HeroVisual } from "@/components/marketing/HeroVisual";
import { ProviderRibbon } from "@/components/marketing/ProviderRibbon";
import { InteractiveVoiceSandbox } from "@/components/marketing/InteractiveVoiceSandbox";
import { TransformationSection } from "@/components/marketing/TransformationSection";
import { ByokShowcase } from "@/components/marketing/ByokShowcase";
import { ByokGuideSection } from "@/components/marketing/ByokGuideSection";
import { FeaturesGrid } from "@/components/marketing/FeaturesGrid";
import { WorkspaceShowcase } from "@/components/marketing/WorkspaceShowcase";
import { UseCasesSection } from "@/components/marketing/UseCasesSection";
import { SaaSComparisonTable } from "@/components/marketing/SaaSComparisonTable";
import { TestimonialsSection } from "@/components/marketing/TestimonialsSection";
import { PricingCardRefined } from "@/components/marketing/PricingCardRefined";
import { FaqAndCtaSection } from "@/components/marketing/FaqAndCtaSection";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-[#07080b] text-[#EDEDED] flex flex-col font-sans selection:bg-blue-600/30 selection:text-blue-200">
      {/* 1. TOP NAVBAR */}
      <MarketingNavbar />

      {/* 2. HERO SECTION */}
      <section className="relative pt-28 pb-20 sm:pt-36 sm:pb-28 px-4 sm:px-6 overflow-hidden">
        {/* Soft atmospheric blue glow */}
        <div className="absolute top-10 left-1/4 w-[500px] h-[350px] bg-blue-600/10 rounded-full blur-[120px] pointer-events-none" />

        <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center relative z-10">
          {/* Left Column: Value Prop & Copy */}
          <div className="lg:col-span-6 space-y-7 text-left">
            {/* Top Pill Badge */}
            <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-zinc-900/90 border border-white/10 text-zinc-300 text-xs font-medium shadow-sm">
              <svg className="w-3.5 h-3.5 text-blue-400 fill-current" viewBox="0 0 24 24">
                <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" />
              </svg>
              <span>One-time purchase. No subscription.</span>
            </div>

            {/* Main Headline */}
            <h1 className="text-5xl sm:text-6xl lg:text-7xl font-bold tracking-tight text-white leading-[1.06]">
              Own your voice.<br />
              Own your AI.
            </h1>

            {/* Sub-headline */}
            <p className="text-base sm:text-lg text-zinc-400 max-w-lg leading-relaxed font-normal">
              A powerful voice-to-text workspace you buy once and run with your own AI keys. Speak naturally. Turn your thoughts into finished work.
            </p>

            {/* CTA Action Buttons */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 pt-2">
              <Link
                href="#pricing"
                className="px-7 py-3.5 rounded-full bg-white hover:bg-zinc-200 text-black font-semibold text-sm flex items-center justify-center gap-2 transition-all shadow-[0_10px_25px_rgba(255,255,255,0.15)] active:scale-95"
              >
                Get Own Voice
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                href="#demo"
                className="px-6 py-3.5 rounded-full bg-zinc-900/90 hover:bg-zinc-800 text-zinc-200 hover:text-white border border-white/10 font-medium text-sm flex items-center justify-center gap-2 transition-all"
              >
                <Sparkles className="w-3.5 h-3.5 text-blue-400" />
                Try live demo
              </Link>
            </div>

            {/* Trust Badges */}
            <div className="pt-4 flex items-center gap-6 text-xs text-zinc-400 flex-wrap">
              <span className="flex items-center gap-1.5">
                <Ban className="w-3.5 h-3.5 text-zinc-500" /> No subscription
              </span>
              <span className="flex items-center gap-1.5">
                <KeyRound className="w-3.5 h-3.5 text-zinc-500" /> Bring your own key
              </span>
              <span className="flex items-center gap-1.5">
                <Monitor className="w-3.5 h-3.5 text-blue-400" /> Windows Desktop & Web Workspace (macOS Beta)
              </span>
            </div>
          </div>

          {/* Right Column: Interactive App Mockup Visual */}
          <div className="lg:col-span-6 relative">
            <HeroVisual />
          </div>
        </div>
      </section>

      {/* 3. PROVIDER RIBBON */}
      <ProviderRibbon />

      {/* 4. INTERACTIVE LIVE VOICE SANDBOX (Hero Conversion Booster) */}
      <InteractiveVoiceSandbox />

      {/* 5. TRANSFORMATION ("From thought to finished work. In seconds.") */}
      <TransformationSection />

      {/* 6. BYOK HIGH-CONTRAST SECTION ("Your AI. Your keys. Your choice.") */}
      <ByokShowcase />

      {/* 7. 60-SECOND ZERO-CONFUSION BYOK GUIDE */}
      <ByokGuideSection />

      {/* 8. 6-CARD FEATURES GRID */}
      <FeaturesGrid />

      {/* 9. DESKTOP WORKSPACE SHOWCASE ("A workspace designed for voice.") */}
      <WorkspaceShowcase />

      {/* 10. USE CASES SECTION ("Built for how you actually work.") */}
      <UseCasesSection />

      {/* 11. AGGRESSIVE SAAS COMPARISON TABLE ("Kill the SaaS Tax") */}
      <SaaSComparisonTable />

      {/* 12. TESTIMONIALS ("Simple. Powerful. A game changer.") */}
      <TestimonialsSection />

      {/* 13. REFINED PRICING CARD (Cashfree INR ₹1,499 & Stripe USD $49) */}
      <PricingCardRefined />

      {/* 14. FAQ & RADIANT BLUE CTA BANNER */}
      <FaqAndCtaSection />

      {/* 15. FOOTER */}
      <MarketingFooter />
    </div>
  );
}
