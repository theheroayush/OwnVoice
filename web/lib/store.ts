import { create } from "zustand";
import { IntentType, ToneType } from "./providers/types";

export type RecordingStatus = "IDLE" | "RECORDING" | "PAUSED" | "PROCESSING";

interface GenerationData {
  id: string;
  title: string;
  intent: string;
  targetTone: string;
  rawInput: string;
  outputText: string;
  wordCount: number;
  providerUsed: string;
  modelUsed: string;
  createdAt: string;
  versions?: Array<{
    id: string;
    actionTaken: string;
    outputText: string;
    createdAt: string;
  }>;
}

interface AppStore {
  status: RecordingStatus;
  statusMessage: string;
  audioDurationSec: number;
  audioVolume: number;
  rawTranscript: string;
  currentGeneration: GenerationData | null;
  activeIntent: IntentType;
  activeTone: ToneType;
  selectedProvider: string;
  toast: { message: string; type: "success" | "error" | "info" } | null;

  setStatus: (status: RecordingStatus, message?: string) => void;
  setAudioDurationSec: (sec: number | ((prev: number) => number)) => void;
  setAudioVolume: (volume: number) => void;
  setRawTranscript: (text: string) => void;
  setCurrentGeneration: (gen: GenerationData | null) => void;
  setActiveIntent: (intent: IntentType) => void;
  setActiveTone: (tone: ToneType) => void;
  setSelectedProvider: (provider: string) => void;
  showToast: (message: string, type?: "success" | "error" | "info") => void;
  hideToast: () => void;
}

export const useAppStore = create<AppStore>((set) => ({
  status: "IDLE",
  statusMessage: "",
  audioDurationSec: 0,
  audioVolume: 0,
  rawTranscript: "",
  currentGeneration: null,
  activeIntent: "CLEAN",
  activeTone: "NATURAL",
  selectedProvider: "openai",
  toast: null,

  setStatus: (status, message = "") => set({ status, statusMessage: message }),
  setAudioDurationSec: (updater) =>
    set((state) => ({
      audioDurationSec: typeof updater === "function" ? updater(state.audioDurationSec) : updater,
    })),
  setAudioVolume: (audioVolume) => set({ audioVolume }),
  setRawTranscript: (rawTranscript) => set({ rawTranscript }),
  setCurrentGeneration: (currentGeneration) => set({ currentGeneration }),
  setActiveIntent: (activeIntent) => set({ activeIntent }),
  setActiveTone: (activeTone) => set({ activeTone }),
  setSelectedProvider: (selectedProvider) => set({ selectedProvider }),
  showToast: (message, type = "success") => set({ toast: { message, type } }),
  hideToast: () => set({ toast: null }),
}));
