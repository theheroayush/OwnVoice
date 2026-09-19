import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";

export async function DELETE(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { id } = await params;

    await prisma.customInstruction.deleteMany({
      where: { id, userId: user.id },
    });

    return NextResponse.json({ success: true, message: "Instruction deleted." });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to delete instruction." },
      { status: 500 }
    );
  }
}
