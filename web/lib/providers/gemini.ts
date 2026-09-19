import { AIProvider, GenerationOptions, ToneType, TranscriptionResult } from "./types";

export class GeminiProvider implements AIProvider {
  id = "google" as const;

  async testConnection(apiKey: string): Promise<{ success: boolean; message: string; availableModels?: string[] }> {
    try {
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`);
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        return {
          success: false,
          message: err?.error?.message || `HTTP ${res.status}: Invalid Google Gemini API key.`,
        };
      }

      const data = await res.json();
      const models = (data.models || [])
        .map((m: { name: string }) => m.name.replace("models/", ""))
        .filter((name: string) => name.includes("gemini") || name.includes("flash"));

      return {
        success: true,
        message: "Successfully verified Google Gemini API key connection.",
        availableModels: models.slice(0, 10),
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.message || "Network error communicating with Google Gemini.",
      };
    }
  }

  async transcribe(audioBuffer: Buffer, mimeType: string, apiKey: string): Promise<TranscriptionResult> {
    const base64Audio = audioBuffer.toString("base64");
    const prompt = "Please accurately transcribe the following audio speech. Return ONLY the spoken text, with all filler words removed, proper punctuation, and accurate capitalization. Do NOT add notes.";

    const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [
          {
            parts: [
              { text: prompt },
              {
                inlineData: {
                  mimeType: mimeType.split(";")[0] || "audio/webm",
                  data: base64Audio,
                },
              },
            ],
          },
        ],
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Gemini audio transcription failed: ${res.status}`);
    }

    const data = await res.json();
    const candidateText = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";
    return {
      text: candidateText,
      confidence: 0.99,
    };
  }

  async generate(options: GenerationOptions, apiKey: string, model: string = "gemini-2.5-flash"): Promise<{ text: string; title: string }> {
    const modelToUse = model.includes("gemini") ? model : "gemini-2.5-flash";
    const systemInstruction = `You are Own Voice — an executive voice AI workspace transforming spoken thoughts into finished work.
Return JSON with two keys:
{
  "title": "Short 2-4 word title",
  "text": "The final polished output"
}
Rules:
- Intent: ${options.intent}
- Tone: ${options.tone}
${options.userInstructions ? `- Writing Style: ${options.userInstructions}` : ""}
- OUTPUT ONLY JSON, NO CODE BLOCKS, NO PREAMBLE.`;

    const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${modelToUse}:generateContent?key=${apiKey}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: {
          parts: [{ text: systemInstruction }],
        },
        contents: [
          {
            parts: [{ text: `Spoken Thought:\n"${options.rawTranscript}"\n${options.context ? `Context: ${options.context}` : ""}` }],
          },
        ],
        generationConfig: {
          responseMimeType: "application/json",
          temperature: 0.2,
        },
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Gemini generation failed: ${res.status}`);
    }

    const data = await res.json();
    const raw = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "{}";
    try {
      const parsed = JSON.parse(raw);
      return {
        title: parsed.title || "Spoken Idea",
        text: parsed.text || raw,
      };
    } catch {
      return {
        title: "Voice Output",
        text: raw,
      };
    }
  }

  async rewrite(text: string, targetTone: ToneType, customPrompt: string | undefined, apiKey: string, model: string = "gemini-2.5-flash"): Promise<string> {
    const modelToUse = model.includes("gemini") ? model : "gemini-2.5-flash";
    const prompt = `Rewrite the following text to be ${targetTone} in tone.
${customPrompt ? `Instruction: ${customPrompt}\n` : ""}
Text to rewrite:
"${text}"
Output ONLY the rewritten text with zero explanation.`;

    const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${modelToUse}:generateContent?key=${apiKey}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.3 },
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Gemini rewrite failed: ${res.status}`);
    }

    const data = await res.json();
    return data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || text;
  }
}
