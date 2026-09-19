import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { encryptApiKey, maskApiKey } from "@/lib/crypto";
import { getAIProvider, SupportedProvider, SUPPORTED_PROVIDERS_CONFIG } from "@/lib/providers";
import { logEvent, logAudit } from "@/lib/analytics";

export async function GET() {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const userProviders = await prisma.userProvider.findMany({
      where: { userId: user.id },
      select: {
        id: true,
        provider: true,
        keyHint: true,
        selectedModel: true,
        isActive: true,
        isValid: true,
        lastTestedAt: true,
        createdAt: true,
      },
    });

    return NextResponse.json({
      success: true,
      providers: userProviders,
      supported: SUPPORTED_PROVIDERS_CONFIG,
    });
  } catch (error: any) {
    console.error("Fetch providers error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to fetch providers." },
      { status: 500 }
    );
  }
}

export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { provider, apiKey, selectedModel } = await req.json();

    if (!provider || !apiKey) {
      return NextResponse.json(
        { success: false, error: "Provider and API key are required." },
        { status: 400 }
      );
    }

    const normalizedProvider = provider.toLowerCase().trim() as SupportedProvider;
    if (!SUPPORTED_PROVIDERS_CONFIG[normalizedProvider]) {
      return NextResponse.json(
        { success: false, error: `Invalid provider: ${provider}` },
        { status: 400 }
      );
    }

    const trimmedKey = apiKey.trim();
    // Test connection with provider before saving
    const aiProvider = getAIProvider(normalizedProvider);
    const testResult = await aiProvider.testConnection(trimmedKey);

    if (!testResult.success) {
      await logEvent("provider_connection_failed", user.id, { provider: normalizedProvider, reason: testResult.message });
      return NextResponse.json(
        { success: false, error: `Connection failed: ${testResult.message}` },
        { status: 400 }
      );
    }

    const encryptedKey = encryptApiKey(trimmedKey);
    const keyHint = maskApiKey(trimmedKey);
    const modelToSave = selectedModel || SUPPORTED_PROVIDERS_CONFIG[normalizedProvider].models[0].id;

    const saved = await prisma.userProvider.upsert({
      where: {
        userId_provider: {
          userId: user.id,
          provider: normalizedProvider,
        },
      },
      update: {
        encryptedKey,
        keyHint,
        selectedModel: modelToSave,
        isActive: true,
        isValid: true,
        lastTestedAt: new Date(),
      },
      create: {
        userId: user.id,
        provider: normalizedProvider,
        encryptedKey,
        keyHint,
        selectedModel: modelToSave,
        isActive: true,
        isValid: true,
        lastTestedAt: new Date(),
      },
    });

    // Update default provider in settings if this is their first connected provider
    await prisma.userSettings.upsert({
      where: { userId: user.id },
      update: {
        defaultProvider: normalizedProvider,
        defaultModel: modelToSave,
      },
      create: {
        userId: user.id,
        defaultProvider: normalizedProvider,
        defaultModel: modelToSave,
      },
    });

    await logEvent("provider_connected", user.id, { provider: normalizedProvider });
    await logAudit("KEY_CONNECTED", user.id);

    return NextResponse.json({
      success: true,
      provider: {
        id: saved.id,
        provider: saved.provider,
        keyHint: saved.keyHint,
        selectedModel: saved.selectedModel,
        isActive: saved.isActive,
        lastTestedAt: saved.lastTestedAt,
      },
      message: `Successfully connected ${SUPPORTED_PROVIDERS_CONFIG[normalizedProvider].name}!`,
    });
  } catch (error: any) {
    console.error("Save provider error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to save AI provider." },
      { status: 500 }
    );
  }
}
