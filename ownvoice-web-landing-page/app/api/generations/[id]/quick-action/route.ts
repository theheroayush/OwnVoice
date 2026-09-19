import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { decryptApiKey } from "@/lib/crypto";
import { getAIProvider, ToneType } from "@/lib/providers";
import { logEvent } from "@/lib/analytics";

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { id } = await params;
    const body = await req.json();
    const { action } = body;

    if (!action) {
      return NextResponse.json(
        { success: false, error: "Action is required." },
        { status: 400 }
      );
    }

    const generation = await prisma.generation.findFirst({
      where: { id, userId: user.id },
    });

    if (!generation) {
      return NextResponse.json({ success: false, error: "Generation not found." }, { status: 404 });
    }

    // Determine action parameters
    let targetTone: ToneType = "NATURAL";
    let instruction: string | undefined = undefined;

    switch (action.toUpperCase()) {
      case "REWRITE_PROFESSIONAL":
      case "PROFESSIONAL":
        targetTone = "PROFESSIONAL";
        instruction = "Make the language polished, executive, and suitable for business communications.";
        break;
      case "REWRITE_CASUAL":
      case "CASUAL":
        targetTone = "CASUAL";
        instruction = "Make the language friendly, warm, conversational, and direct.";
        break;
      case "SHORTEN":
        targetTone = "CONCISE";
        instruction = "Significantly shorten this text to its essential core points without losing the key message.";
        break;
      case "EXPAND":
        targetTone = "EXPANDED";
        instruction = "Elaborate with greater detail, complete explanations, and thorough reasoning.";
        break;
      case "SUMMARIZE":
        targetTone = "CONCISE";
        instruction = "Summarize into concise bullet points with key takeaways.";
        break;
      case "RETRY":
      case "TRY_AGAIN":
        targetTone = "NATURAL";
        instruction = "Regenerate this output with fresh phrasing and optimal structure.";
        break;
      default:
        targetTone = "NATURAL";
        instruction = `Custom adjustment: ${action}`;
        break;
    }

    // Fetch user's active provider
    const userProvider = await prisma.userProvider.findFirst({
      where: {
        userId: user.id,
        provider: generation.providerUsed,
        isActive: true,
      },
    }) || await prisma.userProvider.findFirst({
      where: { userId: user.id, isActive: true },
    });

    if (!userProvider) {
      return NextResponse.json(
        { success: false, error: "No active AI provider connected to perform this quick action." },
        { status: 400 }
      );
    }

    const plainApiKey = decryptApiKey(userProvider.encryptedKey);
    const ai = getAIProvider(userProvider.provider);

    const rewritten = await ai.rewrite(
      generation.outputText,
      targetTone,
      instruction,
      plainApiKey,
      userProvider.selectedModel
    );

    const newWordCount = rewritten.trim().split(/\s+/).filter(Boolean).length;

    // Create version and update generation
    const [updatedGeneration, newVersion] = await prisma.$transaction([
      prisma.generation.update({
        where: { id: generation.id },
        data: {
          outputText: rewritten,
          wordCount: newWordCount,
          targetTone,
        },
      }),
      prisma.generationVersion.create({
        data: {
          generationId: generation.id,
          actionTaken: action.toUpperCase(),
          outputText: rewritten,
        },
      }),
    ]);

    await logEvent("rewrite_clicked", user.id, {
      generationId: generation.id,
      action: action.toUpperCase(),
    });

    return NextResponse.json({
      success: true,
      generation: {
        ...updatedGeneration,
        latestVersion: newVersion,
      },
    });
  } catch (error: any) {
    console.error("Quick action error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to perform quick action." },
      { status: 500 }
    );
  }
}
