import { AIProvider, GenerationOptions, ToneType } from "./types";

export class AnthropicProvider implements AIProvider {
  id = "anthropic" as const;

  async testConnection(apiKey: string): Promise<{ success: boolean; message: string; availableModels?: string[] }> {
    try {
      const res = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "x-api-key": apiKey,
          "anthropic-version": "2023-06-01",
          "content-type": "application/json",
        },
        body: JSON.stringify({
          model: "claude-3-5-haiku-20241022",
          max_tokens: 1,
          messages: [{ role: "user", content: "hi" }],
        }),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        return {
          success: false,
          message: err?.error?.message || `HTTP ${res.status}: Invalid Anthropic API key.`,
        };
      }

      return {
        success: true,
        message: "Successfully verified Anthropic API key connection.",
        availableModels: ["claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022"],
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.message || "Network error communicating with Anthropic.",
      };
    }
  }

  async generate(options: GenerationOptions, apiKey: string, model: string = "claude-3-5-sonnet-20241022"): Promise<{ text: string; title: string }> {
    const systemPrompt = `You are Own Voice — an elite executive voice workspace.
Convert the spoken input into finished work.
Output ONLY a JSON object with:
{
  "title": "Concise 2-4 word title",
  "text": "The finished formatted work"
}
Rules:
- Intent: ${options.intent}
- Tone: ${options.tone}
${options.userInstructions ? `- Writing Rules:\n${options.userInstructions}` : ""}
Never add conversational greetings or meta commentary. Return pure valid JSON.`;

    const res = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
      },
      body: JSON.stringify({
        model,
        max_tokens: 2048,
        system: systemPrompt,
        messages: [
          { role: "user", content: `Raw Transcript:\n"${options.rawTranscript}"\n${options.context ? `Context: ${options.context}` : ""}` },
        ],
        temperature: 0.2,
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Anthropic generation failed: ${res.status}`);
    }

    const data = await res.json();
    const content = data.content?.[0]?.text || "";
    try {
      const cleanJson = content.replace(/```json/g, "").replace(/```/g, "").trim();
      const parsed = JSON.parse(cleanJson);
      return {
        title: parsed.title || "Voice Document",
        text: parsed.text || content,
      };
    } catch {
      return {
        title: "Finished Work",
        text: content,
      };
    }
  }

  async rewrite(text: string, targetTone: ToneType, customPrompt: string | undefined, apiKey: string, model: string = "claude-3-5-sonnet-20241022"): Promise<string> {
    const res = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
      },
      body: JSON.stringify({
        model,
        max_tokens: 2048,
        system: `Rewrite the following text to be ${targetTone}. ${customPrompt || ""}. Output ONLY the rewritten text with zero explanation.`,
        messages: [{ role: "user", content: text }],
        temperature: 0.3,
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err?.error?.message || `Anthropic rewrite failed: ${res.status}`);
    }

    const data = await res.json();
    return data.content?.[0]?.text?.trim() || text;
  }
}
