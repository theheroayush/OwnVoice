import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { decryptApiKey } from "@/lib/crypto";
import { getAIProvider, SupportedProvider } from "@/lib/providers";
import { logEvent } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const formData = await req.formData();
    const audioFile = formData.get("audio") as File | null;
    const durationSec = parseInt((formData.get("duration") as string) || "0", 10);
    const requestedProvider = (formData.get("provider") as string) || undefined;

    // Validation: Empty recording check (Edge Case 6)
    if (!audioFile || audioFile.size === 0) {
      return NextResponse.json(
        { success: false, error: "Empty recording. Please speak before submitting." },
        { status: 400 }
      );
    }

    // Validation: Max duration check (Edge Case 7 - max 10 minutes)
    if (durationSec > 600) {
      return NextResponse.json(
        { success: false, error: "Recording exceeds maximum duration of 10 minutes." },
        { status: 400 }
      );
    }

    // Determine transcription provider
    // Transcription is supported by: openai, groq, google
    const userProviders = await prisma.userProvider.findMany({
      where: {
        userId: user.id,
        isActive: true,
      },
    });

    if (userProviders.length === 0) {
      return NextResponse.json(
        {
          success: false,
          errorCode: "NO_PROVIDER",
          error: "No AI provider connected. Please connect your OpenAI, Groq, or Google API key in Settings.",
        },
        { status: 400 }
      );
    }

    // Find a provider capable of transcription
    let targetProvider = userProviders.find((p) =>
      requestedProvider ? p.provider === requestedProvider : ["groq", "openai", "google"].includes(p.provider)
    );

    if (!targetProvider) {
      // Fallback to any provider if requested was not found
      targetProvider = userProviders.find((p) => ["groq", "openai", "google"].includes(p.provider));
    }

    if (!targetProvider) {
      return NextResponse.json(
        {
          success: false,
          errorCode: "NO_TRANSCRIPTION_PROVIDER",
          error: "Your connected provider (Anthropic) does not natively support speech transcription. Please connect OpenAI, Groq, or Google Gemini.",
        },
        { status: 400 }
      );
    }

    const plainApiKey = decryptApiKey(targetProvider.encryptedKey);
    const aiInstance = getAIProvider(targetProvider.provider);

    if (!aiInstance.transcribe) {
      return NextResponse.json(
        { success: false, error: `Provider ${targetProvider.provider} does not support audio transcription.` },
        { status: 400 }
      );
    }

    const arrayBuffer = await audioFile.arrayBuffer();
    const audioBuffer = Buffer.from(arrayBuffer);

    // Call transcription provider
    const transcription = await aiInstance.transcribe(
      audioBuffer,
      audioFile.type || "audio/webm",
      plainApiKey
    );

    // Audio buffer is automatically garbage collected here (Ephemeral Audio Retention)
    // Persist recording metadata and transcript
    const recording = await prisma.recording.create({
      data: {
        userId: user.id,
        durationSec: durationSec || 0,
        mimeType: audioFile.type || "audio/webm",
        transcript: {
          create: {
            rawText: transcription.text,
            language: transcription.language || "en",
            confidence: transcription.confidence || 0.98,
            providerUsed: targetProvider.provider,
          },
        },
      },
      include: {
        transcript: true,
      },
    });

    await logEvent("transcription_completed", user.id, {
      recordingId: recording.id,
      provider: targetProvider.provider,
      length: transcription.text.length,
    });

    return NextResponse.json({
      success: true,
      recordingId: recording.id,
      transcript: transcription.text,
      language: transcription.language,
      providerUsed: targetProvider.provider,
    });
  } catch (error: any) {
    console.error("Transcription error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Speech transcription failed. Please try again." },
      { status: 500 }
    );
  }
}
