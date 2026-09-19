"use client";

import React, { useState, useRef, useEffect } from "react";
import { useAppStore } from "@/lib/store";
import { Mic, Square, Pause, Play, X, RotateCcw, AlertCircle, RefreshCw } from "lucide-react";

export function VoiceRecorder() {
  const {
    status,
    setStatus,
    audioDurationSec,
    setAudioDurationSec,
    setRawTranscript,
    setCurrentGeneration,
    activeIntent,
    activeTone,
    showToast,
  } = useAppStore();

  const [permissionDenied, setPermissionDenied] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const timerIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const animationFrameRef = useRef<number | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Web Speech API fallback recognition reference
  const speechRecognitionRef = useRef<any>(null);
  const fallbackTranscriptRef = useRef<string>("");

  useEffect(() => {
    return () => {
      stopTimer();
      cancelAudioVisualization();
      if (speechRecognitionRef.current) {
        try {
          speechRecognitionRef.current.stop();
        } catch {}
      }
    };
  }, []);

  const startTimer = () => {
    setAudioDurationSec(0);
    timerIntervalRef.current = setInterval(() => {
      setAudioDurationSec((prev) => prev + 1);
    }, 1000);
  };

  const stopTimer = () => {
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
  };

  const initAudioVisualization = (stream: MediaStream) => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;

      const audioCtx = new AudioCtx();
      const analyser = audioCtx.createAnalyser();
      const source = audioCtx.createMediaStreamSource(stream);

      analyser.fftSize = 64;
      source.connect(analyser);

      audioContextRef.current = audioCtx;
      analyserRef.current = analyser;

      drawWaveform();
    } catch (err) {
      console.warn("Audio visualization unavailable:", err);
    }
  };

  const drawWaveform = () => {
    if (!canvasRef.current || !analyserRef.current) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const bufferLength = analyserRef.current.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    const render = () => {
      animationFrameRef.current = requestAnimationFrame(render);
      analyserRef.current?.getByteFrequencyData(dataArray);

      ctx.clearRect(0, 0, canvas.width, canvas.height);

      const barWidth = (canvas.width / bufferLength) * 1.6;
      let x = 0;

      for (let i = 0; i < bufferLength; i++) {
        const barHeight = (dataArray[i] / 255) * canvas.height * 0.9;

        // Gradient styling
        ctx.fillStyle = status === "PAUSED" ? "#71717a" : "#3b82f6";
        ctx.beginPath();
        ctx.roundRect(x, (canvas.height - barHeight) / 2, barWidth - 2, Math.max(3, barHeight), 2);
        ctx.fill();

        x += barWidth;
      }
    };

    render();
  };

  const cancelAudioVisualization = () => {
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
    if (audioContextRef.current && audioContextRef.current.state !== "closed") {
      audioContextRef.current.close().catch(() => {});
      audioContextRef.current = null;
    }
  };

  const startRecording = async () => {
    setErrorMessage(null);
    setPermissionDenied(false);
    audioChunksRef.current = [];
    fallbackTranscriptRef.current = "";

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      initAudioVisualization(stream);

      const mimeType = MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
        ? "audio/webm;codecs=opus"
        : "audio/webm";

      const mediaRecorder = new MediaRecorder(stream, { mimeType });
      mediaRecorderRef.current = mediaRecorder;

      mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.start(250); // Emit chunk every 250ms
      startTimer();
      setStatus("RECORDING");

      // Initialize Web Speech Recognition as parallel local transcription helper
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      if (SpeechRecognition) {
        try {
          const recognition = new SpeechRecognition();
          recognition.continuous = true;
          recognition.interimResults = true;
          recognition.lang = "en-US";
          recognition.onresult = (e: any) => {
            let interim = "";
            for (let i = e.resultIndex; i < e.results.length; ++i) {
              if (e.results[i].isFinal) {
                fallbackTranscriptRef.current += e.results[i][0].transcript + " ";
              } else {
                interim += e.results[i][0].transcript;
              }
            }
          };
          recognition.start();
          speechRecognitionRef.current = recognition;
        } catch (e) {
          console.warn("Local speech recognition unavailable, will rely on server BYOK:", e);
        }
      }
    } catch (err: any) {
      console.error("Microphone access error:", err);
      if (err.name === "NotAllowedError" || err.name === "PermissionDeniedError") {
        setPermissionDenied(true);
      } else {
        setErrorMessage("Microphone could not be initialized. Check your audio device.");
      }
      setStatus("IDLE");
    }
  };

  const pauseRecording = () => {
    if (mediaRecorderRef.current && status === "RECORDING") {
      mediaRecorderRef.current.pause();
      stopTimer();
      setStatus("PAUSED");
      if (speechRecognitionRef.current) {
        try { speechRecognitionRef.current.stop(); } catch {}
      }
    }
  };

  const resumeRecording = () => {
    if (mediaRecorderRef.current && status === "PAUSED") {
      mediaRecorderRef.current.resume();
      timerIntervalRef.current = setInterval(() => {
        setAudioDurationSec((prev) => prev + 1);
      }, 1000);
      setStatus("RECORDING");
      if (speechRecognitionRef.current) {
        try { speechRecognitionRef.current.start(); } catch {}
      }
    }
  };

  const cancelRecording = () => {
    stopTimer();
    cancelAudioVisualization();
    if (speechRecognitionRef.current) {
      try { speechRecognitionRef.current.stop(); } catch {}
    }
    if (mediaRecorderRef.current) {
      mediaRecorderRef.current.stream.getTracks().forEach((track) => track.stop());
      mediaRecorderRef.current = null;
    }
    audioChunksRef.current = [];
    setStatus("IDLE");
    showToast("Recording discarded.", "info");
  };

  const stopAndProcessRecording = async () => {
    stopTimer();
    cancelAudioVisualization();

    if (speechRecognitionRef.current) {
      try { speechRecognitionRef.current.stop(); } catch {}
    }

    if (!mediaRecorderRef.current) return;

    setStatus("PROCESSING", "Finalizing audio...");

    mediaRecorderRef.current.stream.getTracks().forEach((track) => track.stop());

    // Wait a brief moment to ensure all data chunks flushed
    await new Promise((resolve) => setTimeout(resolve, 300));

    const audioBlob = new Blob(audioChunksRef.current, {
      type: mediaRecorderRef.current.mimeType || "audio/webm",
    });

    if (audioBlob.size === 0 && !fallbackTranscriptRef.current.trim()) {
      setStatus("IDLE");
      setErrorMessage("No speech detected. Please speak into your microphone.");
      return;
    }

    try {
      setStatus("PROCESSING", "Transcribing speech...");

      let transcriptText = "";
      let recordingId: string | undefined = undefined;

      // Attempt server-side BYOK transcription first
      const formData = new FormData();
      formData.append("audio", audioBlob, "recording.webm");
      formData.append("duration", audioDurationSec.toString());

      const transcribeRes = await fetch("/api/recordings/transcribe", {
        method: "POST",
        body: formData,
      });

      const transcribeData = await transcribeRes.json();

      if (transcribeRes.ok && transcribeData.success) {
        transcriptText = transcribeData.transcript;
        recordingId = transcribeData.recordingId;
      } else {
        // If server transcription failed or no BYOK key is connected yet,
        // use the in-browser Web Speech transcript fallback so user can still test!
        if (fallbackTranscriptRef.current.trim()) {
          transcriptText = fallbackTranscriptRef.current.trim();
        } else {
          throw new Error(transcribeData.error || "Failed to transcribe audio.");
        }
      }

      setRawTranscript(transcriptText);

      // Next phase: AI Generation (Intent + Tone + Custom Instructions)
      setStatus("PROCESSING", "Understanding intent and generating finished work...");

      const genRes = await fetch("/api/generations", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          rawTranscript: transcriptText,
          intent: activeIntent,
          tone: activeTone,
          recordingId,
        }),
      });

      const genData = await genRes.json();

      if (!genRes.ok || !genData.success) {
        throw new Error(genData.error || "Failed to generate AI output.");
      }

      setCurrentGeneration(genData.generation);
      setStatus("IDLE");
      showToast("Finished work ready!", "success");
    } catch (err: any) {
      console.error("Pipeline error:", err);
      setStatus("IDLE");
      setErrorMessage(err.message || "An error occurred during voice processing.");
      showToast(err.message || "Voice processing failed", "error");
    }
  };

  const formatTime = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <div className="w-full flex flex-col items-center justify-center space-y-6">
      {/* Microphone Permission Denied State (PRD Section 39) */}
      {permissionDenied && (
        <div className="w-full max-w-md p-4 rounded-xl bg-rose-950/40 border border-rose-500/30 text-rose-300 text-xs flex items-start gap-3">
          <AlertCircle className="w-4 h-4 shrink-0 text-rose-400 mt-0.5" />
          <div className="space-y-1">
            <span className="font-semibold block text-rose-200">Microphone Access Denied</span>
            <p className="text-rose-300/80">
              Own Voice needs microphone permission to capture your voice. Please click the lock icon in your browser address bar and enable Microphone access.
            </p>
          </div>
        </div>
      )}

      {/* General Error Message */}
      {errorMessage && !permissionDenied && (
        <div className="w-full max-w-md p-3.5 rounded-xl bg-amber-950/40 border border-amber-500/30 text-amber-300 text-xs flex items-center justify-between gap-2">
          <span>{errorMessage}</span>
          <button
            onClick={() => setErrorMessage(null)}
            className="text-amber-400 hover:text-amber-200 text-xs font-semibold"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Primary Voice Capsule & Visualizer */}
      <div className="flex flex-col items-center space-y-4">
        {/* Real-time audio waveform canvas */}
        {(status === "RECORDING" || status === "PAUSED") && (
          <div className="w-64 h-12 flex items-center justify-center bg-zinc-950/60 rounded-xl border border-white/5 px-2">
            <canvas ref={canvasRef} width={240} height={44} className="w-full h-full" />
          </div>
        )}

        {/* Timer & Status Label */}
        {status !== "IDLE" && (
          <div className="flex items-center gap-2">
            <span
              className={`w-2 h-2 rounded-full ${
                status === "RECORDING"
                  ? "bg-rose-500 animate-ping"
                  : status === "PAUSED"
                  ? "bg-amber-500"
                  : "bg-blue-500 animate-spin"
              }`}
            />
            <span className="font-mono text-sm font-semibold tracking-wider text-zinc-300">
              {formatTime(audioDurationSec)}
            </span>
          </div>
        )}

        {/* The Giant Microphone Action Button */}
        {status === "IDLE" && (
          <button
            onClick={startRecording}
            className="group relative w-24 h-24 rounded-full bg-gradient-to-b from-blue-600 to-blue-700 hover:from-blue-500 hover:to-blue-600 text-white flex items-center justify-center shadow-xl hover:shadow-blue-500/30 transition-all transform hover:scale-105 active:scale-95 border border-blue-400/30"
            title="Click to speak"
          >
            <Mic className="w-10 h-10 group-hover:scale-110 transition-transform" />
          </button>
        )}

        {/* Active Recording Controls */}
        {(status === "RECORDING" || status === "PAUSED") && (
          <div className="flex items-center gap-4">
            {/* Cancel Button */}
            <button
              onClick={cancelRecording}
              className="p-3 rounded-full bg-zinc-900 hover:bg-zinc-800 text-zinc-400 hover:text-rose-400 border border-white/10 transition-all"
              title="Cancel recording"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Pause / Resume Button */}
            {status === "RECORDING" ? (
              <button
                onClick={pauseRecording}
                className="p-4 rounded-full bg-zinc-800 hover:bg-zinc-700 text-zinc-200 border border-white/10 transition-all"
                title="Pause recording"
              >
                <Pause className="w-6 h-6" />
              </button>
            ) : (
              <button
                onClick={resumeRecording}
                className="p-4 rounded-full bg-blue-600 hover:bg-blue-500 text-white shadow-lg transition-all"
                title="Resume recording"
              >
                <Play className="w-6 h-6 ml-0.5" />
              </button>
            )}

            {/* Stop & Finish Button */}
            <button
              onClick={stopAndProcessRecording}
              className="p-4 rounded-full bg-rose-600 hover:bg-rose-500 text-white shadow-lg glow-recording transition-all transform hover:scale-105 active:scale-95"
              title="Stop & Process"
            >
              <Square className="w-6 h-6 fill-white" />
            </button>
          </div>
        )}

        {/* Processing State (PRD Section 16) */}
        {status === "PROCESSING" && (
          <div className="flex flex-col items-center space-y-3 p-6 rounded-2xl bg-zinc-900/50 border border-white/5">
            <RefreshCw className="w-8 h-8 text-blue-400 animate-spin" />
            <div className="text-center space-y-1">
              <span className="text-sm font-semibold text-white block">Transforming your thought...</span>
              <span className="text-xs text-zinc-400 font-mono">Speech → Intent → Finished Work</span>
            </div>
          </div>
        )}

        {/* Primary Prompt Instruction */}
        {status === "IDLE" && (
          <div className="text-center space-y-1">
            <span className="text-base font-medium text-zinc-200 block">
              Click to speak your thoughts
            </span>
            <span className="text-xs text-zinc-500">
              Speak naturally. Own Voice polishes, formats, and finishes the work.
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
