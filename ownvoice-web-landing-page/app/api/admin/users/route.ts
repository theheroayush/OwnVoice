import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { logAudit } from "@/lib/analytics";

export async function GET(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user || user.role !== "ADMIN") {
      return NextResponse.json({ success: false, error: "Forbidden." }, { status: 403 });
    }

    const { searchParams } = new URL(req.url);
    const query = searchParams.get("q") || "";

    const where: any = {};
    if (query) {
      where.OR = [
        { email: { contains: query } },
        { name: { contains: query } },
      ];
    }

    const users = await prisma.user.findMany({
      where,
      orderBy: { createdAt: "desc" },
      take: 50,
      select: {
        id: true,
        email: true,
        name: true,
        role: true,
        status: true,
        createdAt: true,
        lastLoginAt: true,
        licenses: {
          select: {
            id: true,
            licenseKey: true,
            status: true,
            activatedAt: true,
          },
        },
        _count: {
          select: {
            generations: true,
            recordings: true,
          },
        },
      },
    });

    return NextResponse.json({ success: true, users });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to load users." },
      { status: 500 }
    );
  }
}

export async function PUT(req: NextRequest) {
  try {
    const admin = await getCurrentUser();
    if (!admin || admin.role !== "ADMIN") {
      return NextResponse.json({ success: false, error: "Forbidden." }, { status: 403 });
    }

    const { userId, status } = await req.json();

    if (!userId || !["ACTIVE", "SUSPENDED"].includes(status)) {
      return NextResponse.json({ success: false, error: "Invalid user or status." }, { status: 400 });
    }

    const updated = await prisma.user.update({
      where: { id: userId },
      data: { status },
    });

    await logAudit(`USER_${status}`, admin.id);

    return NextResponse.json({ success: true, user: updated });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to update user status." },
      { status: 500 }
    );
  }
}
