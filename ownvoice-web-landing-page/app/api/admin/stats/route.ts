import { NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";

export async function GET() {
  try {
    const user = await getCurrentUser();
    if (!user || user.role !== "ADMIN") {
      return NextResponse.json({ success: false, error: "Forbidden: Admin access required." }, { status: 403 });
    }

    const [
      totalUsers,
      activeLicenses,
      totalPurchases,
      purchasesAgg,
      totalGenerations,
      totalRecordings,
      recentUsers,
      providerStats,
    ] = await Promise.all([
      prisma.user.count(),
      prisma.license.count({ where: { status: "ACTIVE" } }),
      prisma.purchase.count({ where: { status: "COMPLETED" } }),
      prisma.purchase.aggregate({
        _sum: { amount: true },
        where: { status: "COMPLETED" },
      }),
      prisma.generation.count(),
      prisma.recording.count(),
      prisma.user.findMany({
        take: 5,
        orderBy: { createdAt: "desc" },
        select: {
          id: true,
          email: true,
          name: true,
          status: true,
          createdAt: true,
          licenses: { select: { licenseKey: true, status: true } },
        },
      }),
      prisma.generation.groupBy({
        by: ["providerUsed"],
        _count: { id: true },
      }),
    ]);

    return NextResponse.json({
      success: true,
      stats: {
        totalUsers,
        activeLicenses,
        totalPurchases,
        grossRevenue: purchasesAgg._sum.amount || 0,
        totalGenerations,
        totalRecordings,
        recentUsers,
        providerStats,
      },
    });
  } catch (error: any) {
    console.error("Admin stats error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to load admin stats." },
      { status: 500 }
    );
  }
}
