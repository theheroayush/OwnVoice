import { MarketingNavbar } from "@/components/marketing/MarketingNavbar";
import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, BookOpen, KeyRound, ShieldCheck, CheckCircle2 } from "lucide-react";

interface DocContent {
  title: string;
  category: string;
  updatedAt: string;
  content: string;
}

const DOCS_DATABASE: Record<string, DocContent> = {
  "getting-started": {
    title: "Getting Started with Own Voice",
    category: "Quickstart",
    updatedAt: "September 2026",
    content: `
### 1. Welcome to Own Voice
Own Voice is an executive voice productivity application designed to make speaking your thoughts faster than typing. Rather than paying a recurring monthly fee to an AI SaaS company, you purchase a lifetime software license and connect your own AI provider keys (BYOK).

### 2. The 3-Step Setup
1. **Create an Account**: Register with your email or log in.
2. **Connect an AI Provider**: Go to **Settings → AI Providers** and connect your API key from OpenAI, Google Gemini, Anthropic, or Groq.
3. **Grant Microphone Access**: Allow your browser or Windows client to access your microphone.

### 3. Your First Voice Note
- Navigate to the **Workspace**.
- Select your desired format (Clean Polish, Email, Message, Summary, Note, or Code).
- Click the large blue microphone button or press \`Space\`.
- Speak naturally: *"Write an email to Rahul letting him know our demo moves to tomorrow afternoon."*
- Click **Stop**. In less than two seconds, your finalized, perfectly formatted email will appear on your screen ready to copy!
    `,
  },
  "connecting-api-key": {
    title: "Connecting Your AI API Key (BYOK)",
    category: "BYOK Setup",
    updatedAt: "September 2026",
    content: `
### Why Bring Your Own Key?
Traditional SaaS dictation tools charge $20 to $30 every month to cover their platform markup. When you bring your own key:
- You pay the AI provider wholesale prices (~$0.001 per voice note).
- Typical monthly API cost: **$0.50 to $1.50** for everyday power users.
- You have zero usage throttling during high-traffic hours.

### Obtaining API Keys
- **OpenAI**: Visit [platform.openai.com/api-keys](https://platform.openai.com/api-keys). Create a secret key starting with \`sk-proj-\`.
- **Groq**: Visit [console.groq.com/keys](https://console.groq.com/keys) for ultra-fast LPU inference. Keys start with \`gsk_\`.
- **Google Gemini**: Visit [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey).
- **Anthropic**: Visit [console.anthropic.com/settings/keys](https://console.anthropic.com/settings/keys).

### How Own Voice Protects Your Key
1. Your raw key is transmitted over TLS 1.3 encrypted HTTPS.
2. The server encrypts your key using authenticated **AES-256-GCM** with a distinct 128-bit IV and authentication tag.
3. The raw key is never stored in plaintext and never logged.
4. When displayed back in the UI, only a masked suffix hint (e.g. \`••••••••8F21\`) is shown.
    `,
  },
  "providers": {
    title: "AI Providers & Model Comparison",
    category: "Architecture",
    updatedAt: "September 2026",
    content: `
### Provider Compatibility Matrix

| Provider | Speech Transcription | Text Generation | Best Used For |
| :--- | :--- | :--- | :--- |
| **OpenAI** | Whisper-1 | GPT-4o, GPT-4o-mini | Gold standard accuracy & reasoning |
| **Groq** | Whisper Large V3 | Llama 3.3 70B | Real-time speed (< 0.8s total latency) |
| **Google Gemini** | Gemini 2.5 Flash | Gemini 2.5 Flash | Multimodal audio & deep context |
| **Anthropic** | via paired provider | Claude 3.5 Sonnet | Unmatched nuanced human prose |

*Tip: For maximum speed, connect Groq. For the most articulate business prose, connect OpenAI or Anthropic.*
    `,
  },
  "voice-settings": {
    title: "Voice Settings & Custom Writing Style",
    category: "Personalization",
    updatedAt: "September 2026",
    content: `
### Personal Writing Style Rules
In **Settings → AI Behavior**, you can define global writing instructions that Own Voice will enforce across every voice generation.

#### Examples of High-Impact Instructions:
- **Concise Executive Tone**: *"Keep all sentences under 20 words. Use active voice. Eliminate filler phrases like 'I hope this email finds you well'."*
- **Indian English Nuances**: *"Use natural Indian English phrasing when appropriate for domestic business communications (e.g. 'revert back', 'prepone')."*
- **Developer Formatting**: *"When describing technical issues, format all filenames, variables, and commands in markdown backticks."*

### Audio Capture Preferences
- **Auto-Punctuation**: Automatically detects pauses and vocal inflection to insert commas, periods, and question marks.
- **Immediate Audio Purge**: Enabled by default to delete raw audio buffers the millisecond transcription finishes.
    `,
  },
  "troubleshooting": {
    title: "Troubleshooting & Edge Cases",
    category: "Support",
    updatedAt: "September 2026",
    content: `
### Common Issues & Quick Fixes

#### 1. "Microphone Access Denied"
- **Cause**: Browser blocked audio permissions.
- **Fix**: Click the padlock or settings icon in your browser URL bar. Set **Microphone** to **Allow**, then refresh the page.

#### 2. "HTTP 401: Invalid API Key"
- **Cause**: The key entered was mistyped or revoked on the provider console.
- **Fix**: Go to Settings → AI Providers. Disconnect the provider, generate a fresh key on your provider dashboard, and click Connect.

#### 3. "HTTP 429: Rate Limit or Quota Exceeded"
- **Cause**: Your provider account ran out of credits or hit an organization tier rate limit.
- **Fix**: Check your billing balance on OpenAI or Groq. Add a prepaid balance ($5 is usually enough for months of dictation).

#### 4. "No Speech Detected"
- **Cause**: The recording was empty or microphone volume was muted in operating system settings.
- **Fix**: Check your input device in Windows/Mac Sound Settings and verify the VU meter moves when speaking.
    `,
  },
  "billing": {
    title: "Lifetime Licensing & Provider Billing",
    category: "Billing",
    updatedAt: "September 2026",
    content: `
### One-Time Purchase vs Recurring API Costs

#### What you pay Own Voice:
- A one-time purchase software license ($49 USD or ₹3,999 INR).
- Grants perpetual lifetime access to the software, updates, desktop builds, and features.
- Never renews. No subscription fees.

#### What you pay your AI Provider:
- You pay OpenAI, Groq, Google, or Anthropic directly for the tokens and audio minutes you consume.
- Estimated costs:
  - 100 voice notes/month: ~**$0.15**
  - 1,000 voice notes/month: ~**$1.50**
  - 5,000 voice notes/month: ~**$7.50**
    `,
  },
  "privacy": {
    title: "Privacy Architecture & Zero-Retention Policy",
    category: "Privacy",
    updatedAt: "September 2026",
    content: `
### Our Strict Privacy Commitments
1. **Ephemeral Audio**: Raw audio is streamed to the transcription provider in-memory and never written to permanent disk storage.
2. **Zero Audio Training**: We do not use your voice recordings to train proprietary models.
3. **Encrypted Keys**: AES-256-GCM authenticated encryption protects your provider credentials.
4. **Full Deletion Control**: You can wipe any individual transcript or delete your entire account with one click in Settings.
    `,
  },
  "terms": {
    title: "Terms of Service & Licensing Agreement",
    category: "Legal",
    updatedAt: "September 2026",
    content: `
### 1. Agreement to Terms
By downloading, installing, or accessing the Own Voice desktop or web applications, you agree to be bound by these Terms of Service. If you do not agree, do not use the software.

### 2. Lifetime License Grant
Own Voice grants you a revocable, non-exclusive, non-transferable, limited lifetime license to download, install, and execute the Own Voice application for personal and commercial use on up to three (3) personal hardware devices.

### 3. Bring Your Own Key (BYOK) Model
Own Voice is a local client workspace. You are responsible for maintaining and funding your independent accounts with third-party AI inference providers (such as Google Gemini, OpenAI, Anthropic, or Groq). Own Voice does not resell or mark up third-party API tokens.

### 4. Updates & Support
Your one-time purchase includes one (1) full year of free major software updates, bug fixes, and continuous compatibility maintenance.

### 5. Contact & Inquiries
For legal or license queries, contact us at legal@ownvoice.app or support@ownvoice.app.
    `,
  },
  "refund-policy": {
    title: "30-Day Money-Back Guarantee & Refund Policy",
    category: "Billing",
    updatedAt: "September 2026",
    content: `
### 1. Our 30-Day Guarantee
We stand behind the quality of Own Voice. If Own Voice does not significantly speed up your workflow or if you encounter hardware incompatibility that our support team cannot resolve, you are entitled to a full 100% refund within 30 days of your original purchase date.

### 2. How to Request a Refund
1. Send an email to **refunds@ownvoice.app** or **support@ownvoice.app** with the subject line: *"Refund Request - [Your Order ID or Email]"*.
2. Include your purchase email or the Cashfree/Stripe transaction ID.
3. Our team processes refunds within 24–48 business hours. Funds return to your original payment method (UPI account, credit card, or bank account) within 5–7 business days per banking settlement norms.

### 3. License Deactivation Upon Refund
Upon issuance of a refund, the associated lifetime license key will be marked as REVOKED in our licensing registry.
    `,
  },
  "contact": {
    title: "Contact & Customer Support",
    category: "Company",
    updatedAt: "September 2026",
    content: `
### Get in Touch
We are here to help you get the most out of your Own Voice license.

- **Customer Support**: support@ownvoice.app
- **Founder Direct**: ayush@ownvoice.app
- **Response Window**: Monday – Saturday, 9:00 AM – 7:00 PM IST (typically within 2 hours).
- **Business Entity**: Own Voice Software
- **Operating Jurisdiction**: New Delhi, India
    `,
  },
};

export default async function DocPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const doc = DOCS_DATABASE[slug];

  if (!doc) {
    notFound();
  }

  return (
    <div className="min-h-screen bg-[#09090b] text-[#EDEDED] flex flex-col">
      <MarketingNavbar />
      <main className="flex-1 max-w-3xl mx-auto px-4 sm:px-6 py-16 sm:py-24 space-y-8">
        <Link
          href="/docs"
          className="inline-flex items-center gap-1.5 text-xs text-zinc-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to all documentation
        </Link>

        <div className="space-y-2 border-b border-white/5 pb-6">
          <div className="flex items-center gap-2 text-xs font-mono text-blue-400">
            <span>{doc.category}</span>
            <span>•</span>
            <span className="text-zinc-500">Updated {doc.updatedAt}</span>
          </div>
          <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-white">
            {doc.title}
          </h1>
        </div>

        {/* Formatted Documentation Content */}
        <div className="prose prose-invert prose-zinc max-w-none text-xs sm:text-sm leading-relaxed space-y-4">
          <div
            className="whitespace-pre-line text-zinc-300 space-y-4"
            dangerouslySetInnerHTML={{
              __html: doc.content
                .replace(/^### (.*$)/gim, '<h3 class="text-base font-bold text-white mt-6 mb-2">$1</h3>')
                .replace(/^#### (.*$)/gim, '<h4 class="text-sm font-semibold text-zinc-200 mt-4 mb-1">$1</h4>')
                .replace(/\*\*(.*?)\*\*/gim, '<strong class="text-white font-semibold">$1</strong>')
                .replace(/\*(.*?)\*/gim, '<em class="text-zinc-300">$1</em>')
                .replace(/`([^`]+)`/gim, '<code class="px-1.5 py-0.5 rounded bg-zinc-800 text-blue-300 font-mono text-xs">$1</code>'),
            }}
          />
        </div>

        <div className="pt-8 border-t border-white/5 flex items-center justify-between text-xs text-zinc-500">
          <span>Need more help? Check our FAQ or reach out to support.</span>
          <Link href="/faq" className="text-blue-400 hover:underline">
            View FAQ →
          </Link>
        </div>
      </main>
      <MarketingFooter />
    </div>
  );
}
