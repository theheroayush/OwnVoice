import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import { logEvent, EventType } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    const body = await req.json();
    const { eventType, metadata } = body;

    if (!eventType) {
      return NextResponse.json({ success: false, error: "eventType is required." }, { status: 400 });
    }

    await logEvent(eventType as EventType, user?.id, metadata);

    return NextResponse.json({ success: true });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to log event." },
      { status: 500 }
    );
  }
}
