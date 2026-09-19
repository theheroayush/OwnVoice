import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { logEvent } from "@/lib/analytics";

export async function GET(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { searchParams } = new URL(req.url);
    const query = searchParams.get("q") || "";
    const intent = searchParams.get("intent") || "";
    const limit = Math.min(parseInt(searchParams.get("limit") || "50", 10), 100);
    const offset = parseInt(searchParams.get("offset") || "0", 10);

    const whereClause: any = {
      userId: user.id,
    };

    if (intent) {
      whereClause.intent = intent;
    }

    if (query) {
      whereClause.OR = [
        { title: { contains: query } },
        { rawInput: { contains: query } },
        { outputText: { contains: query } },
        { tags: { contains: query } },
      ];
    }

    const [total, items] = await Promise.all([
      prisma.generation.count({ where: whereClause }),
      prisma.generation.findMany({
        where: whereClause,
        orderBy: { createdAt: "desc" },
        take: limit,
        skip: offset,
        include: {
          versions: {
            orderBy: { createdAt: "desc" },
          },
          recording: {
            select: {
              durationSec: true,
              mimeType: true,
            },
          },
        },
      }),
    ]);

    await logEvent("history_opened", user.id, { query, total });

    return NextResponse.json({
      success: true,
      total,
      limit,
      offset,
      generations: items,
    });
  } catch (error: any) {
    console.error("History fetch error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to load history." },
      { status: 500 }
    );
  }
}
