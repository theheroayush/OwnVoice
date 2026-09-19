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
        { licenseKey: { contains: query } },
        { user: { email: { contains: query } } },
      ];
    }

    const licenses = await prisma.license.findMany({
      where,
      orderBy: { createdAt: "desc" },
      take: 50,
      include: {
        user: {
          select: {
            id: true,
            email: true,
            name: true,
          },
        },
        purchase: {
          select: {
            transactionId: true,
            amount: true,
            currency: true,
            status: true,
          },
        },
      },
    });

    return NextResponse.json({ success: true, licenses });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to load licenses." },
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

    const { licenseId, status } = await req.json();

    if (!licenseId || !["ACTIVE", "REVOKED", "REFUNDED", "SUSPENDED"].includes(status)) {
      return NextResponse.json({ success: false, error: "Invalid license ID or status." }, { status: 400 });
    }

    const updated = await prisma.license.update({
      where: { id: licenseId },
      data: { status },
    });

    await logAudit(`LICENSE_${status}`, admin.id);

    return NextResponse.json({ success: true, license: updated });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to update license status." },
      { status: 500 }
    );
  }
}
