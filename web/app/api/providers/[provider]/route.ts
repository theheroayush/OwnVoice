import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { logAudit } from "@/lib/analytics";

export async function DELETE(
  _req: NextRequest,
  { params }: { params: Promise<{ provider: string }> }
) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { provider } = await params;
    const normalized = provider.toLowerCase().trim();

    await prisma.userProvider.deleteMany({
      where: {
        userId: user.id,
        provider: normalized,
      },
    });

    await logAudit("KEY_REVOKED", user.id);

    return NextResponse.json({
      success: true,
      message: `Disconnected ${normalized} API key. Existing generation history is preserved.`,
    });
  } catch (error: any) {
    console.error("Disconnect provider error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to disconnect provider." },
      { status: 500 }
    );
  }
}
