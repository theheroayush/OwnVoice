import { NextRequest, NextResponse } from "next/server";
import prisma from "@/lib/db";
import { createSessionToken, setSessionCookie, verifyPassword } from "@/lib/auth";
import { clearFailures, clientIp, isLockedOut, recordFailure } from "@/lib/rate-limit";

const LOGIN_MAX_ATTEMPTS = 5;
const LOGIN_WINDOW_SECONDS = 300;

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { email, password } = body;

    if (!email || !password) {
      return NextResponse.json(
        { success: false, error: "Email and password are required." },
        { status: 400 }
      );
    }

    const normalizedEmail = email.trim().toLowerCase();
    const rateKey = `login:${clientIp(req)}:${normalizedEmail}`;

    // Password guessing must be expensive: after a handful of failures the
    // caller is locked out for the remainder of the window.
    const lock = isLockedOut(rateKey, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_SECONDS);
    if (!lock.allowed) {
      return NextResponse.json(
        {
          success: false,
          error: `Too many failed sign-in attempts. Please try again in ${lock.retryAfterSeconds} seconds.`,
        },
        { status: 429, headers: { "Retry-After": String(lock.retryAfterSeconds) } }
      );
    }
    const user = await prisma.user.findUnique({
      where: { email: normalizedEmail },
      include: {
        licenses: {
          where: { status: "ACTIVE" },
        },
      },
    });

    if (!user) {
      recordFailure(rateKey, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_SECONDS);
      return NextResponse.json(
        { success: false, error: "Invalid email or password." },
        { status: 401 }
      );
    }

    if (user.status === "SUSPENDED") {
      return NextResponse.json(
        { success: false, error: "This account has been suspended. Please contact support." },
        { status: 403 }
      );
    }

    const isValid = await verifyPassword(password, user.passwordHash);
    if (!isValid) {
      const result = recordFailure(rateKey, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_SECONDS);
      return NextResponse.json(
        {
          success: false,
          error: result.allowed
            ? "Invalid email or password."
            : "Too many failed sign-in attempts. Please try again later.",
        },
        { status: 401 }
      );
    }

    clearFailures(rateKey);

    await prisma.user.update({
      where: { id: user.id },
      data: { lastLoginAt: new Date() },
    });

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
        hasActiveLicense: user.licenses.length > 0,
      },
    });

    setSessionCookie(response, token);
    return response;
  } catch (error: any) {
    console.error("Login error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to log in." },
      { status: 500 }
    );
  }
}
