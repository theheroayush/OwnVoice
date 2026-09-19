"use client";

import React, { useState, useRef, useEffect } from "react";
import {
  Mic,
  Square,
  Sparkles,
  Copy,
  Check,
  RotateCcw,
  ArrowRight,
  Volume2,
  Zap,
  Mail,
  MessageSquare,
  FileCode,
  FileText,
} from "lucide-react";

interface PresetThought {
  id: string;
  icon: any;
  label: string;
  intent: "EMAIL" | "MESSAGE" | "NOTE" | "CLEAN";
  raw: string;
  formatted: string;
}

const PRESETS: PresetThought[] = [
  {
    id: "email",
    icon: Mail,
    label: "Client Email",
    intent: "EMAIL",
    raw: "um hey so write an email to Alex saying we need two more days to complete the Stripe integration testing because we want to ensure zero bugs before rollout",
    formatted: `Subject: Project Update: Stripe Integration Testing

Hi Alex,

I wanted to give you a quick update on our progress. We are taking an additional two days to finalize our Stripe integration testing to ensure zero edge-case bugs before rollout.

Everything will be ready for your final walkthrough by Wednesday afternoon. Thank you for your patience!

Best regards,
Ayush`,
  },
  {
    id: "message",
    icon: MessageSquare,
    label: "Slack / WhatsApp",
    intent: "MESSAGE",
    raw: "write a quick message to Rahul saying can we move our sync to four pm something urgent came up with the client call thanks",
    formatted: `Hey Rahul, can we move our sync to 4:00 PM today? Something urgent came up with the client call. Let me know if that works! 👍`,
  },
  {
    id: "note",
    icon: FileText,
    label: "Meeting Note",
    intent: "NOTE",
    raw: "note for the team meeting basically we agreed that the landing page needs a live interactive mic demo pricing should stay at fourteen ninety nine and desktop apps should have offline whisper fallback",
    formatted: `**Key Action Items:**

- Launch interactive live microphone sandbox on landing page.
- Lock one-time pricing at ₹1,499 (India) / $49 (Global).
- Verify zero-latency local Whisper fallback in desktop client.`,
  },
  {
    id: "code",
    icon: FileCode,
    label: "Developer Update",
    intent: "CLEAN",
    raw: "so the token refresh race condition was fixed by implementing an in memory mutex using navigator dot locks across multi tab sessions",
    formatted: `Fixed the auth token refresh race condition by implementing an in-memory mutex via \`navigator.locks\` across multi-tab browser sessions.`,
  },
];

export function InteractiveVoiceSandbox() {
  const [selectedPreset, setSelectedPreset] = useState<PresetThought>(PRESETS[0]);
  const [rawText, setRawText] = useState(PRESETS[0].raw);
  const [outputText, setOutputText] = useState(PRESETS[0].formatted);
  const [isRecording, setIsRecording] = useState(false);
  const [isTransforming, setIsTransforming] = useState(false);
  const [copied, setCopied] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [hasMicSupport, setHasMicSupport] = useState(true);

  const recognitionRef = useRef<any>(null);
  const timerRef = useRef<any>(null);

  // Initialize Web Speech API if supported in user browser
  useEffect(() => {
    if (typeof window !== "undefined") {
      const SpeechRecognition =
        (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      if (!SpeechRecognition) {
        setHasMicSupport(false);
      }
    }
  }, []);

  const startVoiceRecording = () => {
    if (typeof window === "undefined") return;

    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      alert("Microphone voice recognition is not supported in this browser. Please use Chrome, Edge, or Safari, or click any sample preset below.");
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = "en-US";

      let speechBuffer = "";

      recognition.onstart = () => {
        setIsRecording(true);
        setRawText("");
        setOutputText("");
        setRecordingSeconds(0);
        timerRef.current = setInterval(() => {
          setRecordingSeconds((prev) => prev + 1);
        }, 1000);
      };

      recognition.onresult = (event: any) => {
        let interimTranscript = "";
        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            speechBuffer += event.results[i][0].transcript + " ";
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }
        setRawText(speechBuffer || interimTranscript);
      };

      recognition.onerror = (e: any) => {
        console.error("Speech recognition error:", e);
        stopVoiceRecording();
      };

      recognition.onend = () => {
        setIsRecording(false);
        if (timerRef.current) clearInterval(timerRef.current);
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch (err) {
      console.error(err);
      setIsRecording(false);
    }
  };

  const stopVoiceRecording = async () => {
    if (recognitionRef.current) {
      recognitionRef.current.stop();
    }
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    setIsRecording(false);

    // Now transform the captured voice
    const textToProcess = rawText.trim();
    if (!textToProcess) {
      setRawText(selectedPreset.raw);
      setOutputText(selectedPreset.formatted);
      return;
    }

    setIsTransforming(true);
    try {
      const res = await fetch("/api/demo/transform", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text: textToProcess, intent: selectedPreset.intent }),
      });
      const data = await res.json();
      if (data.success && data.cleanedOutput) {
        setOutputText(data.cleanedOutput);
      } else {
        setOutputText(textToProcess);
      }
    } catch (err) {
      console.error(err);
      setOutputText(textToProcess);
    } finally {
      setIsTransforming(false);
    }
  };

  const selectPreset = (preset: PresetThought) => {
    if (isRecording) stopVoiceRecording();
    setSelectedPreset(preset);
    setIsTransforming(true);
    setRawText(preset.raw);

    setTimeout(() => {
      setOutputText(preset.formatted);
      setIsTransforming(false);
    }, 250);
  };

  const handleCopy = () => {
    if (!outputText) return;
    navigator.clipboard.writeText(outputText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <section id="demo" className="py-20 px-4 sm:px-6 relative overflow-hidden">
      {/* Background ambient lighting */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[350px] bg-blue-600/10 rounded-full blur-[140px] pointer-events-none" />

      <div className="max-w-5xl mx-auto space-y-10 relative z-10">
        {/* Section Header */}
        <div className="text-center space-y-3 max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            <Sparkles className="w-3.5 h-3.5 text-blue-400" />
            Live Voice Transformation Sandbox
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold text-white tracking-tight">
            See the magic happen in 3 seconds.
          </h2>
          <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed">
            Speak into your microphone or pick a preset thought below. Watch how Own Voice removes filler words, fixes hesitation, and crafts executive written work.
          </p>
        </div>

        {/* The Sandbox Container */}
        <div className="rounded-[32px] bg-[#0c101c]/95 border border-white/[0.12] p-6 sm:p-10 shadow-[0_25px_60px_rgba(0,0,0,0.8),0_0_30px_rgba(59,130,246,0.12)] backdrop-blur-xl space-y-8">
          {/* Top Controls: Preset Pills & Live Mic Button */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 border-b border-white/[0.08] pb-6">
            {/* Presets */}
            <div className="flex items-center gap-2 flex-wrap justify-center sm:justify-start">
              <span className="text-[11px] font-mono text-zinc-500 uppercase mr-1">
                Presets:
              </span>
              {PRESETS.map((preset) => {
                const Icon = preset.icon;
                const active = selectedPreset.id === preset.id;
                return (
                  <button
                    key={preset.id}
                    type="button"
                    onClick={() => selectPreset(preset)}
                    className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-medium transition-all ${
                      active
                        ? "bg-blue-600 text-white shadow-md shadow-blue-500/25 border border-blue-400/40"
                        : "bg-zinc-900/80 text-zinc-400 hover:text-white border border-white/5 hover:border-white/10"
                    }`}
                  >
                    <Icon className="w-3 h-3" />
                    <span>{preset.label}</span>
                  </button>
                );
              })}
            </div>

            {/* Live Microphone Button */}
            <div>
              {isRecording ? (
                <button
                  type="button"
                  onClick={stopVoiceRecording}
                  className="px-5 py-2.5 rounded-full bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-2 transition-all shadow-[0_0_20px_rgba(244,63,94,0.5)] animate-pulse"
                >
                  <Square className="w-3.5 h-3.5 fill-current" />
                  <span>Stop & Polish ({recordingSeconds}s)</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={startVoiceRecording}
                  className="px-5 py-2.5 rounded-full bg-zinc-900 hover:bg-zinc-800 text-zinc-200 hover:text-white border border-white/10 text-xs font-semibold flex items-center gap-2 transition-all shadow-sm active:scale-95"
                >
                  <Mic className="w-3.5 h-3.5 text-blue-400" />
                  <span>Click to Speak (Live Mic)</span>
                </button>
              )}
            </div>
          </div>

          {/* Soundwave Visualizer Bar (when recording) */}
          {isRecording && (
            <div className="p-4 rounded-2xl bg-rose-950/20 border border-rose-500/30 flex items-center justify-between gap-4 animate-in fade-in duration-200">
              <div className="flex items-center gap-3">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
                <span className="text-xs font-semibold text-rose-300">
                  Listening... Speak your thoughts naturally (filler words will be erased)
                </span>
              </div>
              <div className="flex items-center gap-1 h-6">
                {[14, 22, 32, 18, 28, 12, 30, 24, 16, 26].map((h, i) => (
                  <div
                    key={i}
                    className="w-1 bg-rose-400 rounded-full animate-bounce"
                    style={{ height: `${h}px`, animationDelay: `${i * 0.08}s` }}
                  />
                ))}
              </div>
            </div>
          )}

          {/* 2-Column Transformation Board */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
            {/* Left Column: What You Said (Messy Raw Thoughts) */}
            <div className="lg:col-span-5 p-5 sm:p-6 rounded-2xl bg-black/50 border border-white/[0.08] flex flex-col justify-between space-y-4">
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs text-zinc-400">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-amber-400" />
                    <span className="font-semibold text-zinc-300">
                      Raw Speech (Unfiltered)
                    </span>
                  </div>
                  <span className="font-mono text-[10px] text-zinc-500">
                    Input Audio / Text
                  </span>
                </div>

                <div className="p-4 rounded-xl bg-zinc-900/50 border border-white/5 min-h-[160px] text-xs sm:text-sm text-zinc-300 font-mono italic leading-relaxed">
                  "{rawText}"
                </div>
              </div>

              <div className="pt-3 border-t border-white/5 flex items-center justify-between text-[11px] text-zinc-500">
                <span>Includes hesitations & filler words</span>
                <span className="text-amber-400/80 font-mono">Raw Stream</span>
              </div>
            </div>

            {/* Middle: Magic Arrow */}
            <div className="lg:col-span-2 hidden lg:flex flex-col items-center justify-center space-y-2">
              <div className="w-10 h-10 rounded-full bg-blue-600/20 border border-blue-500/40 text-blue-400 flex items-center justify-center shadow-[0_0_15px_rgba(59,130,246,0.3)]">
                <Sparkles className="w-4 h-4" />
              </div>
              <span className="text-[10px] font-mono text-blue-300 uppercase tracking-wider">
                Instant AI
              </span>
            </div>

            {/* Right Column: Polished Output (Finished Work) */}
            <div className="lg:col-span-5 p-5 sm:p-6 rounded-2xl bg-[#101524]/90 border border-blue-500/30 shadow-[0_15px_35px_rgba(0,0,0,0.5)] flex flex-col justify-between space-y-4">
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2 text-emerald-400">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                    <span className="font-semibold">Finished Output (Ready to Send)</span>
                  </div>

                  <button
                    type="button"
                    onClick={handleCopy}
                    className="flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-200 transition-colors border border-white/5"
                  >
                    {copied ? (
                      <>
                        <Check className="w-3 h-3 text-emerald-400" />
                        <span className="text-emerald-400">Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3 h-3" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>

                <div className="p-4 rounded-xl bg-black/40 border border-white/5 min-h-[160px] text-xs sm:text-sm text-zinc-100 font-sans leading-relaxed whitespace-pre-line">
                  {isTransforming ? (
                    <div className="flex flex-col items-center justify-center h-full min-h-[140px] space-y-2 text-zinc-500">
                      <Sparkles className="w-5 h-5 text-blue-400 animate-spin" />
                      <span className="text-xs font-mono">Polishing & formatting...</span>
                    </div>
                  ) : (
                    outputText
                  )}
                </div>
              </div>

              <div className="pt-3 border-t border-white/5 flex items-center justify-between text-[11px] text-zinc-400">
                <span className="flex items-center gap-1 text-emerald-400 font-medium">
                  <Check className="w-3 h-3" /> Zero filler words
                </span>
                <span className="text-blue-400 font-mono">
                  Transformed in ~1.2s
                </span>
              </div>
            </div>
          </div>

          {/* Bottom Conversion Bar */}
          <div className="pt-4 border-t border-white/[0.08] flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="text-left space-y-0.5">
              <div className="text-xs font-bold text-white flex items-center gap-1.5">
                <Zap className="w-3.5 h-3.5 text-amber-400" />
                Want this everywhere on your computer?
              </div>
              <p className="text-[11px] text-zinc-400">
                Own Voice floats as an obsidian pill on Windows. Press <code>F8</code> inside WhatsApp, Slack, Word, or VS Code to dictate instantly.
              </p>
            </div>

            <a
              href="#pricing"
              className="w-full sm:w-auto px-6 py-2.5 rounded-full bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-md active:scale-95 whitespace-nowrap"
            >
              Get Lifetime License (₹1,499 / $49)
              <ArrowRight className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
