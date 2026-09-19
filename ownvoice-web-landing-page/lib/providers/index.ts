import { AIProvider, SupportedProvider } from "./types";
import { OpenAIProvider } from "./openai";
import { GeminiProvider } from "./gemini";
import { AnthropicProvider } from "./anthropic";
import { GroqProvider } from "./groq";

const providers: Record<SupportedProvider, AIProvider> = {
  openai: new OpenAIProvider(),
  google: new GeminiProvider(),
  anthropic: new AnthropicProvider(),
  groq: new GroqProvider(),
};

export function getAIProvider(providerName: string): AIProvider {
  const normalized = providerName.toLowerCase().trim() as SupportedProvider;
  const provider = providers[normalized];
  if (!provider) {
    throw new Error(`Unsupported AI provider: "${providerName}". Supported providers are openai, google, anthropic, groq.`);
  }
  return provider;
}

export * from "./types";
