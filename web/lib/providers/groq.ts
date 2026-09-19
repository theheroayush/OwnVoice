import { AIProvider, GenerationOptions, ToneType, TranscriptionResult } from "./types";

export class GroqProvider implements AIProvider {
  id = "groq" as const;

  async testConnection(apiKey: string): Promise<{ success: boolean; message: string; availableModels?: string[] }> {
    try {
      const res = await fetch("https://api.groq.com/openai/v1/models", {
        headers: { Authorization: `Bearer ${apiKey}` },
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        return {
          success: false,
          message: err?.error?.message || `HTTP ${res.status}: Invalid Groq API key.`,
        };
      }

      const data = await res.json();
      const models = (data.data || []).map((m: { id: string }) => m.id);

      return {
        success: true,
        message: "Successfully verified Groq API key connection.",
        availableModels: models.slice(0, 10),
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.message || "Network error communicating with Groq.",
      };
    }
  }

  async transcribe(audioBuffer: Buffer, mimeType: string, apiKey: string): Promise<TranscriptionResult> {
    const formData = new FormData();
    const blob = new Blob([new Uint8Array(audioBuffer)], { type: mimeType });
    formData.append("file", blob, "audio.webm");
    formData.append("model", "whisper-large-v3");
    formData.append("response_format", "json");

    const res = await fetch("https://api.groq.com/openai/v1/audio/transcriptions", {
      method: "POST",
      headers: { Authorization: `Bearer ${apiKey}` },
      body: formData,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Groq transcription failed: ${res.status}`);
    }

    const data = await res.json();
    return {
      text: data.text || "",
      confidence: 0.99,
    };
  }

  async generate(options: GenerationOptions, apiKey: string, model: string = "llama-3.3-70b-versatile"): Promise<{ text: string; title: string }> {
    const systemPrompt = `You are Own Voice — an executive AI productivity engine converting speech to finished work.
Output pure JSON with NO backticks:
{
  "title": "2-4 word title",
  "text": "The finished production output"
}
Rules:
- Intent: ${options.intent}
- Tone: ${options.tone}
${options.userInstructions ? `- Writing Style:\n${options.userInstructions}` : ""}
No conversational meta-talk.`;

    const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: model || "llama-3.3-70b-versatile",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: options.rawTranscript },
        ],
        response_format: { type: "json_object" },
        temperature: 0.2,
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Groq generation failed: ${res.status}`);
    }

    const data = await res.json();
    const content = data.choices?.[0]?.message?.content || "{}";
    try {
      const parsed = JSON.parse(content);
      return {
        title: parsed.title || "Voice Note",
        text: parsed.text || content,
      };
    } catch {
      return {
        title: "Voice Output",
        text: content,
      };
    }
  }

  async rewrite(text: string, targetTone: ToneType, customPrompt: string | undefined, apiKey: string, model: string = "llama-3.3-70b-versatile"): Promise<string> {
    const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: model || "llama-3.3-70b-versatile",
        messages: [
          { role: "system", content: `Rewrite the text to be ${targetTone}. ${customPrompt || ""}. Output ONLY the rewritten text.` },
          { role: "user", content: text },
        ],
        temperature: 0.3,
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Groq rewrite failed: ${res.status}`);
    }

    const data = await res.json();
    return data.choices?.[0]?.message?.content?.trim() || text;
  }
}
