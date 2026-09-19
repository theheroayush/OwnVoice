import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { decryptApiKey } from "@/lib/crypto";
import { getAIProvider, SupportedProvider } from "@/lib/providers";

export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const body = await req.json();
    const { provider, apiKey } = body;

    if (!provider) {
      return NextResponse.json(
        { success: false, error: "Provider is required." },
        { status: 400 }
      );
    }

    const normalizedProvider = provider.toLowerCase().trim() as SupportedProvider;
    let keyToTest = apiKey ? apiKey.trim() : null;

    // If no apiKey provided in body, test currently saved key
    if (!keyToTest) {
      const saved = await prisma.userProvider.findUnique({
        where: {
          userId_provider: {
            userId: user.id,
            provider: normalizedProvider,
          },
        },
      });

      if (!saved) {
        return NextResponse.json(
          { success: false, error: "No API key found for this provider. Please provide a key." },
          { status: 404 }
        );
      }

      keyToTest = decryptApiKey(saved.encryptedKey);
    }

    const aiProvider = getAIProvider(normalizedProvider);
    const result = await aiProvider.testConnection(keyToTest);

    if (result.success) {
      // Update lastTestedAt
      await prisma.userProvider.updateMany({
        where: { userId: user.id, provider: normalizedProvider },
        data: { lastTestedAt: new Date(), isValid: true },
      });
    }

    return NextResponse.json(result);
  } catch (error: any) {
    console.error("Test provider connection error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to test provider connection." },
      { status: 500 }
    );
  }
}
