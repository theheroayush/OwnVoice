import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { id } = await params;

    const generation = await prisma.generation.findFirst({
      where: {
        id,
        userId: user.id, // Ensure authorization
      },
      include: {
        versions: {
          orderBy: { createdAt: "desc" },
        },
        recording: {
          include: {
            transcript: true,
          },
        },
      },
    });

    if (!generation) {
      return NextResponse.json({ success: false, error: "Generation not found." }, { status: 404 });
    }

    return NextResponse.json({
      success: true,
      generation,
    });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to retrieve generation." },
      { status: 500 }
    );
  }
}

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

    const deleted = await prisma.generation.deleteMany({
      where: {
        id,
        userId: user.id,
      },
    });

    if (deleted.count === 0) {
      return NextResponse.json({ success: false, error: "Generation not found." }, { status: 404 });
    }

    return NextResponse.json({
      success: true,
      message: "Generation deleted successfully.",
    });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to delete generation." },
      { status: 500 }
    );
  }
}
