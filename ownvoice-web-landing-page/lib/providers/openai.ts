import { AIProvider, GenerationOptions, ToneType, TranscriptionResult } from "./types";

export class OpenAIProvider implements AIProvider {
  id = "openai" as const;

  async testConnection(apiKey: string): Promise<{ success: boolean; message: string; availableModels?: string[] }> {
    try {
      const res = await fetch("https://api.openai.com/v1/models", {
        headers: { Authorization: `Bearer ${apiKey}` },
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        return {
          success: false,
          message: err?.error?.message || `HTTP ${res.status}: Invalid OpenAI API key.`,
        };
      }

      const data = await res.json();
      const models = (data.data || [])
        .map((m: { id: string }) => m.id)
        .filter((id: string) => id.includes("gpt") || id.includes("o3") || id.includes("whisper"));

      return {
        success: true,
        message: "Successfully verified OpenAI API key connection.",
        availableModels: models.slice(0, 10),
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.message || "Network error communicating with OpenAI.",
      };
    }
  }

  async transcribe(audioBuffer: Buffer, mimeType: string, apiKey: string): Promise<TranscriptionResult> {
    const formData = new FormData();
    const blob = new Blob([new Uint8Array(audioBuffer)], { type: mimeType });
    formData.append("file", blob, "audio.webm");
    formData.append("model", "whisper-1");
    formData.append("response_format", "json");

    const res = await fetch("https://api.openai.com/v1/audio/transcriptions", {
      method: "POST",
      headers: { Authorization: `Bearer ${apiKey}` },
      body: formData,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `OpenAI transcription failed with status ${res.status}`);
    }

    const data = await res.json();
    return {
      text: data.text || "",
      language: data.language,
      confidence: 0.98,
    };
  }

  async generate(options: GenerationOptions, apiKey: string, model: string = "gpt-4o"): Promise<{ text: string; title: string }> {
    const systemPrompt = `You are Own Voice — a voice-first executive AI workspace that converts spoken thoughts into finished work.
Core Principles:
1. OUTPUT ONLY THE FINISHED PRODUCT. Never include conversational prefixes, preambles, apologies, or meta-commentary (e.g. Do NOT say "Here is your email:").
2. Follow the user's intent: ${options.intent}.
3. Apply Tone: ${options.tone}.
${options.userInstructions ? `4. User's Personal Writing Style & Rules:\n${options.userInstructions}` : ""}

Return a strictly valid JSON response with two keys:
{
  "title": "A concise 2-5 word title summarizing the output",
  "text": "The finished, production-ready output"
}`;

    const userMessage = `Spoken Thought / Transcript:\n"${options.rawTranscript}"\n\n${options.context ? `Context: ${options.context}` : ""}`;

    const res = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model,
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userMessage },
        ],
        response_format: { type: "json_object" },
        temperature: 0.3,
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `OpenAI generation failed with status ${res.status}`);
    }

    const data = await res.json();
    const content = data.choices?.[0]?.message?.content;
    try {
      const parsed = JSON.parse(content);
      return {
        title: parsed.title || "Voice Note",
        text: parsed.text || content,
      };
    } catch {
      return {
        title: "Voice Generation",
        text: content,
      };
    }
  }

  async rewrite(text: string, targetTone: ToneType, customPrompt: string | undefined, apiKey: string, model: string = "gpt-4o"): Promise<string> {
    const systemPrompt = `You are Own Voice's refinement engine. 
Rewrite the text to be ${targetTone} in tone.
${customPrompt ? `Additional Instruction: ${customPrompt}` : ""}
OUTPUT ONLY THE REWRITTEN TEXT WITH ZERO COMMENTARY.`;

    const res = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model,
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: text },
        ],
        temperature: 0.3,
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `OpenAI rewrite failed: ${res.status}`);
    }

    const data = await res.json();
    return data.choices?.[0]?.message?.content?.trim() || text;
  }
}
