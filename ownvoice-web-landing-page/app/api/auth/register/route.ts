import { NextRequest, NextResponse } from "next/server";
import prisma from "@/lib/db";
import { createSessionToken, hashPassword, setSessionCookie } from "@/lib/auth";
import { activateLicenseForUser } from "@/lib/license";
import { logEvent } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { email, password, name, licenseKey } = body;

    if (!email || !password || typeof email !== "string" || typeof password !== "string") {
      return NextResponse.json(
        { success: false, error: "Email and password are required." },
        { status: 400 }
      );
    }

    const normalizedEmail = email.trim().toLowerCase();
    if (password.length < 8) {
      return NextResponse.json(
        { success: false, error: "Password must be at least 8 characters long." },
        { status: 400 }
      );
    }

    const existingUser = await prisma.user.findUnique({
      where: { email: normalizedEmail },
    });

    if (existingUser) {
      return NextResponse.json(
        { success: false, error: "An account with this email already exists." },
        { status: 409 }
      );
    }

    const passwordHash = await hashPassword(password);

    const user = await prisma.user.create({
      data: {
        email: normalizedEmail,
        passwordHash,
        name: name ? name.trim() : null,
        settings: {
          create: {
            defaultProvider: "openai",
            defaultModel: "gpt-4o",
            defaultTone: "NATURAL",
          },
        },
      },
    });

    // If a license key was provided or default starter license
    if (licenseKey) {
      await activateLicenseForUser(user.id, licenseKey);
    }

    const token = createSessionToken({
      id: user.id,
      email: user.email,
      role: user.role,
    });

    const response = NextResponse.json({
      success: true,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
      },
    });

    setSessionCookie(response, token);
    await logEvent("signup_completed", user.id, { email: user.email });

    return response;
  } catch (error: any) {
    console.error("Registration error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to create account." },
      { status: 500 }
    );
  }
}
