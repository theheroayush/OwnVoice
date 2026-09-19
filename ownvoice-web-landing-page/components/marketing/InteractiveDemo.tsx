"use client";

import React, { useState } from "react";
import { Play, Copy, Check, Sparkles, MessageSquare, Mail, Terminal, Lightbulb, RefreshCw } from "lucide-react";

interface DemoSample {
  id: string;
  label: string;
  category: "email" | "message" | "code" | "idea";
  rawSpeech: string;
  intent: string;
  tone: string;
  finishedOutput: string;
}

const DEMO_SAMPLES: DemoSample[] = [
  {
    id: "1",
    label: "Client Reschedule Email",
    category: "email",
    rawSpeech: "okay so write an email to the client saying we are going to need another two days because the integration is taking longer than expected and we want to ensure zero bugs",
    intent: "EMAIL",
    tone: "PROFESSIONAL",
    finishedOutput: `Subject: Project Timeline Update — Integration Testing

Hi [Client Name],

I wanted to provide a quick update on our progress. We are taking an additional two days to finalize the integration and run complete edge-case test suites.

Ensuring zero production bugs is our highest priority, and we will have everything ready for your review by Wednesday afternoon.

Best regards,
Ayush`,
  },
  {
    id: "2",
    label: "Casual WhatsApp to Rahul",
    category: "message",
    rawSpeech: "write a casual WhatsApp message to Rahul telling him that today's meeting needs to move to tomorrow afternoon something urgent came up",
    intent: "MESSAGE",
    tone: "CASUAL",
    finishedOutput: `Hey Rahul, can we push our meeting to tomorrow afternoon? Something urgent came up today. Let me know what time works best for you!`,
  },
  {
    id: "3",
    label: "Developer Bug Explanation",
    category: "code",
    rawSpeech: "explain to the team in Slack why the auth token refresh was failing with status 401 when the client opened multiple tabs simultaneously and how we fixed it with mutex locking",
    intent: "NOTE",
    tone: "CONCISE",
    finishedOutput: `**Auth Token Race Condition Resolved**

- **Issue:** Simultaneous tab launches triggered parallel refresh requests, causing the authorization server to invalidate rotated refresh tokens with 401 Unauthorized.
- **Fix:** Implemented an in-memory client mutex (\`navigator.locks\`) to guarantee only one refresh request executes concurrently while pending requests await the fresh token.
- **Impact:** Zero unexpected logouts across multi-tab sessions.`,
  },
  {
    id: "4",
    label: "Product Feature Idea",
    category: "idea",
    rawSpeech: "idea for Own Voice what if we have custom voice snippets where saying my calendly link automatically pastes my full url and meeting details",
    intent: "PROMPT",
    tone: "NATURAL",
    finishedOutput: `**Feature Concept: Dynamic Voice Macros**

- **Trigger:** Configurable spoken trigger phrases (e.g. *"my calendly link"*, *"support docs"*).
- **Behavior:** Instant inline text expansion to full destination URLs, meeting blurbs, or code templates.
- **Value Proposition:** Eliminates repetitive copy-pasting during voice dictation.`,
  },
];

export function InteractiveDemo() {
  const [selectedSample, setSelectedSample] = useState<DemoSample>(DEMO_SAMPLES[0]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [copied, setCopied] = useState(false);
  const [step, setStep] = useState<"idle" | "transcribing" | "understanding" | "generating" | "done">("done");

  const runSimulation = (sample: DemoSample) => {
    setSelectedSample(sample);
    setIsProcessing(true);
    setStep("transcribing");

    setTimeout(() => {
      setStep("understanding");
      setTimeout(() => {
        setStep("generating");
        setTimeout(() => {
          setStep("done");
          setIsProcessing(false);
        }, 400);
      }, 350);
    }, 350);
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(selectedSample.finishedOutput);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="w-full max-w-4xl mx-auto rounded-2xl bg-[#121215] border border-white/10 shadow-2xl overflow-hidden p-5 sm:p-7">
      {/* Sample Selector Pills */}
      <div className="flex items-center justify-between gap-2 mb-6 flex-wrap">
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Try an example:</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {DEMO_SAMPLES.map((sample) => (
            <button
              key={sample.id}
              onClick={() => runSimulation(sample)}
              disabled={isProcessing}
              className={`text-xs px-3 py-1.5 rounded-lg border font-medium transition-all ${
                selectedSample.id === sample.id
                  ? "bg-blue-600 text-white border-blue-500 shadow-sm"
                  : "bg-zinc-900/60 text-zinc-400 border-white/5 hover:border-white/20 hover:text-white"
              }`}
            >
              {sample.label}
            </button>
          ))}
        </div>
      </div>

      {/* Main Interactive Board */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Left Column: What You Said */}
        <div className="flex flex-col justify-between p-4 sm:p-5 rounded-xl bg-zinc-950/60 border border-white/5 space-y-4">
          <div>
            <div className="flex items-center justify-between text-xs text-zinc-400 mb-3">
              <span className="font-semibold text-zinc-300 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse" />
                Raw Spoken Thought
              </span>
              <span className="font-mono text-[11px] text-zinc-500">Audio In</span>
            </div>
            <p className="text-sm text-zinc-300 italic font-mono leading-relaxed bg-zinc-900/40 p-3.5 rounded-lg border border-white/5">
              "{selectedSample.rawSpeech}"
            </p>
          </div>

          <div className="pt-3 border-t border-white/5 flex items-center justify-between text-xs text-zinc-500">
            <span>Detected: {selectedSample.intent}</span>
            <span>Tone: {selectedSample.tone}</span>
          </div>
        </div>

        {/* Right Column: Finished Work */}
        <div className="flex flex-col justify-between p-4 sm:p-5 rounded-xl bg-zinc-900/40 border border-white/10 space-y-4 relative">
          <div>
            <div className="flex items-center justify-between text-xs text-zinc-400 mb-3">
              <span className="font-semibold text-emerald-400 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
                Finished Work (Ready to Send)
              </span>
              <button
                onClick={copyToClipboard}
                className="flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-md bg-zinc-800 hover:bg-zinc-700 text-zinc-200 transition-colors"
              >
                {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                {copied ? "Copied" : "Copy"}
              </button>
            </div>

            {isProcessing ? (
              <div className="h-44 flex flex-col items-center justify-center space-y-3">
                <RefreshCw className="w-6 h-6 text-blue-400 animate-spin" />
                <span className="text-xs font-mono text-zinc-400 uppercase tracking-wider">
                  {step === "transcribing" && "1. Transcribing audio stream..."}
                  {step === "understanding" && "2. Understanding intent & context..."}
                  {step === "generating" && "3. Generating finished output..."}
                </span>
              </div>
            ) : (
              <div className="text-sm text-zinc-100 whitespace-pre-line leading-relaxed font-sans bg-zinc-950/40 p-3.5 rounded-lg border border-white/5 min-h-[140px]">
                {selectedSample.finishedOutput}
              </div>
            )}
          </div>

          <div className="pt-3 border-t border-white/5 flex items-center justify-between text-[11px] text-zinc-500">
            <span>Zero filler words</span>
            <span className="text-emerald-400 font-medium">Ready in ~1.2s</span>
          </div>
        </div>
      </div>
    </div>
  );
}
