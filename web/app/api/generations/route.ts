import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { decryptApiKey } from "@/lib/crypto";
import { getAIProvider, IntentType, ToneType } from "@/lib/providers";
import { logEvent } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const body = await req.json();
    const {
      rawTranscript,
      intent = "CLEAN",
      tone = "NATURAL",
      context,
      recordingId,
    } = body;

    if (!rawTranscript || typeof rawTranscript !== "string" || !rawTranscript.trim()) {
      return NextResponse.json(
        { success: false, error: "Raw transcript text is required for AI generation." },
        { status: 400 }
      );
    }

    // Fetch user settings and custom instructions
    const settings = await prisma.userSettings.findUnique({
      where: { userId: user.id },
    });

    const customInstructions = await prisma.customInstruction.findMany({
      where: { userId: user.id, isActive: true },
    });

    const compiledInstructions = customInstructions
      .map((ci) => `- ${ci.title}: ${ci.instruction}`)
      .join("\n");

    // Fetch active provider
    const preferredProviderName = settings?.defaultProvider || "openai";
    let userProvider = await prisma.userProvider.findFirst({
      where: { userId: user.id, provider: preferredProviderName, isActive: true },
    });

    if (!userProvider) {
      // Fallback to any active provider
      userProvider = await prisma.userProvider.findFirst({
        where: { userId: user.id, isActive: true },
      });
    }

    if (!userProvider) {
      return NextResponse.json(
        {
          success: false,
          errorCode: "NO_PROVIDER",
          error: "No AI provider connected. Please connect an API key in Settings to process voice thoughts.",
        },
        { status: 400 }
      );
    }

    const plainApiKey = decryptApiKey(userProvider.encryptedKey);
    const ai = getAIProvider(userProvider.provider);

    const generated = await ai.generate(
      {
        intent: intent as IntentType,
        tone: tone as ToneType,
        rawTranscript: rawTranscript.trim(),
        userInstructions: compiledInstructions,
        context,
      },
      plainApiKey,
      userProvider.selectedModel
    );

    const wordCount = generated.text.trim().split(/\s+/).filter(Boolean).length;

    const generation = await prisma.generation.create({
      data: {
        userId: user.id,
        recordingId: recordingId || null,
        title: generated.title || "Voice Document",
        intent,
        targetTone: tone,
        rawInput: rawTranscript.trim(),
        outputText: generated.text,
        wordCount,
        providerUsed: userProvider.provider,
        modelUsed: userProvider.selectedModel,
        versions: {
          create: {
            actionTaken: "INITIAL",
            outputText: generated.text,
          },
        },
      },
      include: {
        versions: true,
      },
    });

    await logEvent("generation_completed", user.id, {
      generationId: generation.id,
      intent,
      tone,
      wordCount,
      provider: userProvider.provider,
    });

    return NextResponse.json({
      success: true,
      generation: {
        id: generation.id,
        title: generation.title,
        intent: generation.intent,
        targetTone: generation.targetTone,
        rawInput: generation.rawInput,
        outputText: generation.outputText,
        wordCount: generation.wordCount,
        providerUsed: generation.providerUsed,
        modelUsed: generation.modelUsed,
        createdAt: generation.createdAt,
        versions: generation.versions,
      },
    });
  } catch (error: any) {
    console.error("Generation error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to generate AI output." },
      { status: 500 }
    );
  }
}
