import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import prisma from "./db";

const DEV_JWT_SECRET = "ownvoice-insecure-dev-jwt-secret-change-me";

/**
 * Resolves the JWT signing secret.
 * Production deployments MUST provide a unique 32+ character JWT_SECRET —
 * falling back to a repository-known default would let anyone forge session
 * cookies. Local development keeps a throwaway default so `npm run dev` works.
 */
function resolveJwtSecret(): string {
  const secret = process.env.JWT_SECRET;
  if (secret && secret.length >= 32) return secret;

  if (process.env.NODE_ENV === "production") {
    throw new Error(
      "JWT_SECRET is missing or too short (min 32 chars). Refusing to sign sessions with an insecure default."
    );
  }
  return DEV_JWT_SECRET;
}

const COOKIE_NAME = "ov_session_token";

export interface SessionUser {
  id: string;
  email: string;
  name: string | null;
  role: string;
  hasActiveLicense: boolean;
}

export async function hashPassword(password: string): Promise<string> {
  const salt = await bcrypt.genSalt(10);
  return bcrypt.hash(password, salt);
}

export async function verifyPassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

export function createSessionToken(user: { id: string; email: string; role: string }): string {
  return jwt.sign(
    {
      sub: user.id,
      email: user.email,
      role: user.role,
    },
    resolveJwtSecret(),
    { expiresIn: "30d" }
  );
}

export function verifySessionToken(token: string): { sub: string; email: string; role: string } | null {
  try {
    return jwt.verify(token, resolveJwtSecret()) as { sub: string; email: string; role: string };
  } catch {
    return null;
  }
}

export async function getCurrentUser(): Promise<SessionUser | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(COOKIE_NAME)?.value;

  if (!token) return null;

  const payload = verifySessionToken(token);
  if (!payload) return null;

  const user = await prisma.user.findUnique({
    where: { id: payload.sub },
    include: {
      licenses: {
        where: { status: "ACTIVE" },
      },
    },
  });

  if (!user || user.status === "SUSPENDED") return null;

  return {
    id: user.id,
    email: user.email,
    name: user.name,
    role: user.role,
    hasActiveLicense: user.licenses.length > 0,
  };
}

export function setSessionCookie(response: NextResponse, token: string) {
  response.cookies.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 30 * 24 * 60 * 60, // 30 days
  });
}

export function clearSessionCookie(response: NextResponse) {
  response.cookies.set(COOKIE_NAME, "", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 0,
  });
}
