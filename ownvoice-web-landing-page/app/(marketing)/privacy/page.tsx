import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import Link from "next/link";
import { ShieldCheck, Lock, Cpu, Trash2, ArrowLeft } from "lucide-react";

export const metadata = {
  title: "Privacy Policy | OwnVoice",
  description: "Our privacy commitments, zero data retention architecture, and transparent permission disclosures.",
};

export default function PrivacyPolicyPage() {
  return (
    <div className="min-h-screen bg-[#0B0F17] text-zinc-100 flex flex-col font-sans selection:bg-blue-600 selection:text-white">
      <MarketingNavbar />

      <main className="flex-1 max-w-4xl mx-auto px-6 py-16 w-full">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-white transition mb-8 group"
        >
          <ArrowLeft className="w-4 h-4 transition-transform group-hover:-translate-x-1" />
          Back to Home
        </Link>

        <div className="border border-zinc-800/80 bg-zinc-900/40 rounded-2xl p-8 sm:p-12 backdrop-blur-xl shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 right-0 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl pointer-events-none" />

          <div className="flex items-center gap-3 text-blue-400 mb-4">
            <ShieldCheck className="w-6 h-6" />
            <span className="text-xs uppercase tracking-widest font-mono font-semibold">Privacy-First Architecture</span>
          </div>

          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white mb-2">
            OwnVoice Privacy Policy
          </h1>
          <p className="text-sm font-mono text-zinc-400 mb-8">
            Effective Date: October 14, 2026 • Domain: ownvoice.in • Support: support@ownvoice.in
          </p>

          <div className="prose prose-invert prose-zinc max-w-none space-y-8 text-zinc-300 leading-relaxed text-sm sm:text-base">
            <section className="bg-zinc-950/60 border border-zinc-800/60 rounded-xl p-6">
              <h2 className="text-lg font-bold text-white flex items-center gap-2 mt-0">
                <Lock className="w-5 h-5 text-blue-400" /> 1. Core Operating Philosophy
              </h2>
              <p className="mb-0">
                Your voice, keystrokes, and thoughts belong solely to you. OwnVoice is engineered from the ground up as a zero-retention, zero-ad utility. We do not sell your personal data, we do not monetize your information, and we do not bundle third-party tracking or behavioral advertising SDKs.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-bold text-white mb-3">2. Audio & Voice Data Processing</h2>
              <ul className="list-disc pl-5 space-y-2">
                <li>
                  <strong className="text-white">Ephemeral Processing:</strong> Voice audio is captured in high-fidelity (48kHz) and processed ephemerally in volatile memory (RAM). Audio buffers are immediately purged once transcription finishes.
                </li>
                <li>
                  <strong className="text-white">Direct AI Streaming:</strong> Transcriptions stream over TLS 1.3 encrypted connections directly to your configured AI provider (Google Gemini, OpenAI Whisper, Groq, or Anthropic).
                </li>
                <li>
                  <strong className="text-white">No Cloud Storage of Audio:</strong> Raw voice audio is never stored or archived on our servers.
                </li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-bold text-white mb-3">3. Android Permissions & Disclosures</h2>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs sm:text-sm border-collapse border border-zinc-800">
                  <thead>
                    <tr className="bg-zinc-800/50 text-white">
                      <th className="p-3 border border-zinc-800">Permission</th>
                      <th className="p-3 border border-zinc-800">Purpose & Strict Scope</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td className="p-3 border border-zinc-800 font-mono text-blue-400">RECORD_AUDIO</td>
                      <td className="p-3 border border-zinc-800">Voice transcription and Meeting Mode recording. Never activates without explicit touch.</td>
                    </tr>
                    <tr>
                      <td className="p-3 border border-zinc-800 font-mono text-blue-400">BIND_ACCESSIBILITY_SERVICE</td>
                      <td className="p-3 border border-zinc-800">
                        Used solely to detect active, editable input fields and paste transcribed text hands-free when you tap the Floating Bubble. Does NOT read passwords, log keystrokes, or monitor user actions.
                      </td>
                    </tr>
                    <tr>
                      <td className="p-3 border border-zinc-800 font-mono text-blue-400">SYSTEM_ALERT_WINDOW</td>
                      <td className="p-3 border border-zinc-800">Enables the optional floating microphone capsule widget over active applications.</td>
                    </tr>
                    <tr>
                      <td className="p-3 border border-zinc-800 font-mono text-blue-400">ACCESS_WIFI_STATE / MULTICAST</td>
                      <td className="p-3 border border-zinc-800">Used strictly on your local private Wi-Fi network for Air-Typing discovery (UDP 8766) to your laptop.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section>
              <h2 className="text-xl font-bold text-white mb-3">4. Bring Your Own Key (BYOK) Security</h2>
              <p>
                When you connect your own AI provider keys (Google Gemini, OpenAI, Groq, Anthropic), your credentials are encrypted locally on your device using hardware-backed <strong>Android Keystore</strong> and <code className="text-blue-400">EncryptedSharedPreferences</code>. Your keys are used solely for direct communication with the provider&apos;s official APIs and are never stored on our servers.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-bold text-white mb-3">5. Data Retention & One-Tap Deletion</h2>
              <p>
                All meeting notes, dictation history, and custom vocabulary snippets are saved locally on your device in private SQLite storage. You can view, export to Markdown, or permanently purge all stored data anytime via <strong>Settings → Clear Data</strong>.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-bold text-white mb-3">6. Contact Information</h2>
              <p>
                For questions regarding this policy or data practices, contact us at:
              </p>
              <div className="bg-zinc-950/40 border border-zinc-800/80 rounded-lg p-4 font-mono text-sm space-y-1">
                <p>Email: <a href="mailto:support@ownvoice.in" className="text-blue-400 hover:underline">support@ownvoice.in</a></p>
                <p>Website: <a href="https://ownvoice.in" className="text-blue-400 hover:underline">https://ownvoice.in</a></p>
                <p>GitHub: <a href="https://github.com/theheroayush/OwnVoice" className="text-blue-400 hover:underline">theheroayush/OwnVoice</a></p>
              </div>
            </section>
          </div>
        </div>
      </main>

      <MarketingFooter />
    </div>
  );
}
