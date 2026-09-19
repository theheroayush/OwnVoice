import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";

export async function GET() {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const [settings, instructions] = await Promise.all([
      prisma.userSettings.findUnique({ where: { userId: user.id } }),
      prisma.customInstruction.findMany({
        where: { userId: user.id },
        orderBy: { createdAt: "asc" },
      }),
    ]);

    return NextResponse.json({
      success: true,
      settings: settings || {
        defaultProvider: "openai",
        defaultModel: "gpt-4o",
        defaultTone: "NATURAL",
        autoPunctuation: true,
        deleteAudioImmediate: true,
      },
      instructions,
    });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to load settings." },
      { status: 500 }
    );
  }
}

export async function PUT(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const body = await req.json();
    const {
      defaultProvider,
      defaultModel,
      defaultTone,
      autoPunctuation,
      deleteAudioImmediate,
      microphoneDeviceId,
    } = body;

    const updated = await prisma.userSettings.upsert({
      where: { userId: user.id },
      update: {
        ...(defaultProvider && { defaultProvider }),
        ...(defaultModel && { defaultModel }),
        ...(defaultTone && { defaultTone }),
        ...(autoPunctuation !== undefined && { autoPunctuation }),
        ...(deleteAudioImmediate !== undefined && { deleteAudioImmediate }),
        ...(microphoneDeviceId !== undefined && { microphoneDeviceId }),
      },
      create: {
        userId: user.id,
        defaultProvider: defaultProvider || "openai",
        defaultModel: defaultModel || "gpt-4o",
        defaultTone: defaultTone || "NATURAL",
        autoPunctuation: autoPunctuation ?? true,
        deleteAudioImmediate: deleteAudioImmediate ?? true,
        microphoneDeviceId,
      },
    });

    return NextResponse.json({
      success: true,
      settings: updated,
      message: "Settings updated successfully.",
    });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to update settings." },
      { status: 500 }
    );
  }
}

// Support adding custom instruction
export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { title, instruction } = await req.json();
    if (!title || !instruction) {
      return NextResponse.json(
        { success: false, error: "Title and instruction are required." },
        { status: 400 }
      );
    }

    const item = await prisma.customInstruction.create({
      data: {
        userId: user.id,
        title: title.trim(),
        instruction: instruction.trim(),
        isActive: true,
      },
    });

    return NextResponse.json({
      success: true,
      instruction: item,
      message: "Custom instruction added.",
    });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to save instruction." },
      { status: 500 }
    );
  }
}
