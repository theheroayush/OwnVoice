export type SupportedProvider = "openai" | "google" | "anthropic" | "groq";

export type IntentType =
  | "TRANSCRIBE"
  | "CLEAN"
  | "REWRITE"
  | "EMAIL"
  | "MESSAGE"
  | "SUMMARY"
  | "NOTE"
  | "PROMPT"
  | "SOCIAL_POST"
  | "CODE"
  | "CUSTOM";

export type ToneType = "NATURAL" | "PROFESSIONAL" | "CASUAL" | "CONCISE" | "EXPANDED";

export interface ModelOption {
  id: string;
  name: string;
  description: string;
  supportsAudio: boolean;
  contextWindow: string;
  recommended?: boolean;
}

export interface ProviderMetadata {
  id: SupportedProvider;
  name: string;
  description: string;
  websiteUrl: string;
  keyHelpUrl: string;
  keyPlaceholder: string;
  models: ModelOption[];
}

export interface TranscriptionResult {
  text: string;
  language?: string;
  confidence?: number;
  durationSec?: number;
}

export interface GenerationOptions {
  intent: IntentType;
  tone: ToneType;
  rawTranscript: string;
  userInstructions?: string;
  context?: string;
}

export interface AIProvider {
  id: SupportedProvider;
  testConnection(apiKey: string): Promise<{ success: boolean; message: string; availableModels?: string[] }>;
  transcribe?(audioBuffer: Buffer, mimeType: string, apiKey: string): Promise<TranscriptionResult>;
  generate(options: GenerationOptions, apiKey: string, model?: string): Promise<{ text: string; title: string }>;
  rewrite(text: string, targetTone: ToneType, customPrompt: string | undefined, apiKey: string, model?: string): Promise<string>;
}

export const SUPPORTED_PROVIDERS_CONFIG: Record<SupportedProvider, ProviderMetadata> = {
  openai: {
    id: "openai",
    name: "OpenAI",
    description: "Industry-standard Whisper speech transcription and GPT-4o reasoning models.",
    websiteUrl: "https://openai.com",
    keyHelpUrl: "https://platform.openai.com/api-keys",
    keyPlaceholder: "sk-proj-...",
    models: [
      { id: "gpt-4o", name: "GPT-4o", description: "Flagship omni model with high intelligence", supportsAudio: true, contextWindow: "128k", recommended: true },
      { id: "gpt-4o-mini", name: "GPT-4o Mini", description: "Fast, lightweight model for everyday voice cleanup", supportsAudio: true, contextWindow: "128k" },
      { id: "o3-mini", name: "o3-mini", description: "High-reasoning model for complex structured drafting", supportsAudio: false, contextWindow: "200k" },
    ],
  },
  google: {
    id: "google",
    name: "Google Gemini",
    description: "Multimodal Gemini 2.5 and 1.5 models with massive context and native audio processing.",
    websiteUrl: "https://ai.google.dev",
    keyHelpUrl: "https://aistudio.google.com/app/apikey",
    keyPlaceholder: "AIzaSy...",
    models: [
      { id: "gemini-2.5-flash", name: "Gemini 2.5 Flash", description: "Ultra-fast multimodal reasoning with audio support", supportsAudio: true, contextWindow: "1M", recommended: true },
      { id: "gemini-1.5-flash", name: "Gemini 1.5 Flash", description: "High speed, low latency audio & text model", supportsAudio: true, contextWindow: "1M" },
      { id: "gemini-1.5-pro", name: "Gemini 1.5 Pro", description: "Deep reasoning and complex document structuring", supportsAudio: true, contextWindow: "2M" },
    ],
  },
  anthropic: {
    id: "anthropic",
    name: "Anthropic Claude",
    description: "State-of-the-art nuanced prose, structured writing, and coding reasoning.",
    websiteUrl: "https://anthropic.com",
    keyHelpUrl: "https://console.anthropic.com/settings/keys",
    keyPlaceholder: "sk-ant-api03-...",
    models: [
      { id: "claude-3-5-sonnet-20241022", name: "Claude 3.5 Sonnet", description: "Most capable model for flawless human tone and writing", supportsAudio: false, contextWindow: "200k", recommended: true },
      { id: "claude-3-5-haiku-20241022", name: "Claude 3.5 Haiku", description: "Blazing fast text generation and message formatting", supportsAudio: false, contextWindow: "200k" },
    ],
  },
  groq: {
    id: "groq",
    name: "Groq",
    description: "LPU inference engine delivering instant Whisper transcription and Llama-3.3 generations.",
    websiteUrl: "https://groq.com",
    keyHelpUrl: "https://console.groq.com/keys",
    keyPlaceholder: "gsk_...",
    models: [
      { id: "llama-3.3-70b-versatile", name: "Llama 3.3 70B", description: "Ultra-fast 70B parameter model at 300+ tokens/sec", supportsAudio: false, contextWindow: "128k", recommended: true },
      { id: "whisper-large-v3", name: "Whisper Large V3 (Audio)", description: "Real-time speech-to-text on Groq LPUs", supportsAudio: true, contextWindow: "N/A" },
      { id: "mixtral-8x7b-32768", name: "Mixtral 8x7B", description: "Fast, efficient MoE model for quick formatting", supportsAudio: false, contextWindow: "32k" },
    ],
  },
};
