import { NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";

export async function GET() {
  try {
    const sessionUser = await getCurrentUser();
    if (!sessionUser) {
      return NextResponse.json({ success: false, user: null }, { status: 401 });
    }

    const user = await prisma.user.findUnique({
      where: { id: sessionUser.id },
      include: {
        licenses: {
          orderBy: { createdAt: "desc" },
        },
        providers: {
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
        },
        settings: true,
        instructions: {
          where: { isActive: true },
        },
      },
    });

    if (!user) {
      return NextResponse.json({ success: false, user: null }, { status: 404 });
    }

    return NextResponse.json({
      success: true,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        avatar: user.avatar,
        role: user.role,
        status: user.status,
        createdAt: user.createdAt,
        licenses: user.licenses,
        providers: user.providers,
        settings: user.settings,
        instructions: user.instructions,
      },
    });
  } catch (error: any) {
    console.error("Auth me error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to fetch user profile." },
      { status: 500 }
    );
  }
}
