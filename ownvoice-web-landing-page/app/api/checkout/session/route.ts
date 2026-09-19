import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";
import prisma from "@/lib/db";
import { getCurrentUser, hashPassword } from "@/lib/auth";
import { createStripeCheckoutSession } from "@/lib/payments/stripe";
import { createCashfreeOrder } from "@/lib/payments/cashfree";
import { generateLicenseKey } from "@/lib/license";
import { logEvent } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json().catch(() => ({}));
    const currency = body.currency === "INR" ? "INR" : "USD";
    const appUrl = process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000";
    const returnUrl = `${appUrl}/checkout/success`;

    // 1. Identify or Create User (Support 1-Click Guest Checkout)
    let targetUser: { id: string; email: string; name?: string | null } | null = null;
    const sessionUser = await getCurrentUser();

    if (sessionUser) {
      targetUser = sessionUser;
    } else {
      const emailInput = body.email || body.customerEmail;
      if (!emailInput || typeof emailInput !== "string") {
        return NextResponse.json(
          {
            success: false,
            error: "Please enter a valid email address to receive your license key and download link.",
          },
          { status: 400 }
        );
      }

      const normalizedEmail = emailInput.trim().toLowerCase();
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(normalizedEmail)) {
        return NextResponse.json(
          {
            success: false,
            error: "Please enter a valid email address format (e.g. name@example.com).",
          },
          { status: 400 }
        );
      }

      // Find existing user or register new customer
      let dbUser = await prisma.user.findUnique({
        where: { email: normalizedEmail },
      });

      if (!dbUser) {
        const randomPassword = crypto.randomBytes(16).toString("hex");
        const passwordHash = await hashPassword(randomPassword);
        const nameFallback = body.name || normalizedEmail.split("@")[0];

        dbUser = await prisma.user.create({
          data: {
            email: normalizedEmail,
            name: nameFallback,
            passwordHash,
            role: "USER",
            status: "ACTIVE",
          },
        });
      }

      targetUser = {
        id: dbUser.id,
        email: dbUser.email,
        name: dbUser.name,
      };
    }

    await logEvent("purchase_started", targetUser.id, { currency, email: targetUser.email });

    // 2. Gateway Dispatch: Cashfree (India / INR) vs Stripe (Global / USD)
    if (currency === "INR") {
      const isTestMode = !process.env.CASHFREE_APP_ID || !process.env.CASHFREE_SECRET_KEY;
      const orderId = `order_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;

      if (isTestMode) {
        // Provision test purchase & license for seamless local preview
        const licenseKey = generateLicenseKey();
        await prisma.$transaction([
          prisma.purchase.create({
            data: {
              userId: targetUser.id,
              provider: "cashfree",
              transactionId: orderId,
              amount: 149900, // 1499.00 INR in paise
              currency: "INR",
              productId: "ownvoice-lifetime",
              status: "COMPLETED",
              metadata: JSON.stringify({ isTestMode: true, email: targetUser.email }),
            },
          }),
          prisma.license.create({
            data: {
              userId: targetUser.id,
              licenseKey,
              productId: "ownvoice-lifetime",
              status: "ACTIVE",
              activatedAt: new Date(),
            },
          }),
        ]);

        return NextResponse.json({
          success: true,
          gateway: "cashfree",
          orderId,
          checkoutUrl: `${returnUrl}?order_id=${orderId}&gateway=cashfree&email=${encodeURIComponent(
            targetUser.email
          )}&license_key=${licenseKey}&status=success`,
        });
      }

      // Live Cashfree Order
      const cashfreeResult = await createCashfreeOrder({
        currency: "INR",
        userId: targetUser.id,
        userEmail: targetUser.email,
        userName: targetUser.name || undefined,
        userPhone: body.phone || undefined,
        returnUrl,
      });

      return NextResponse.json(cashfreeResult);
    } else {
      // Global: Stripe Checkout
      const isTestMode = !process.env.STRIPE_SECRET_KEY;
      const mockSessionId = `cs_test_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;

      if (isTestMode) {
        // Provision test purchase & license for seamless local preview
        const licenseKey = generateLicenseKey();
        await prisma.$transaction([
          prisma.purchase.create({
            data: {
              userId: targetUser.id,
              provider: "stripe",
              transactionId: mockSessionId,
              amount: 4900, // $49.00 USD
              currency: "USD",
              productId: "ownvoice-lifetime",
              status: "COMPLETED",
              metadata: JSON.stringify({ isTestMode: true, email: targetUser.email }),
            },
          }),
          prisma.license.create({
            data: {
              userId: targetUser.id,
              licenseKey,
              productId: "ownvoice-lifetime",
              status: "ACTIVE",
              activatedAt: new Date(),
            },
          }),
        ]);

        return NextResponse.json({
          success: true,
          gateway: "stripe",
          sessionId: mockSessionId,
          checkoutUrl: `${returnUrl}?session_id=${mockSessionId}&gateway=stripe&email=${encodeURIComponent(
            targetUser.email
          )}&license_key=${licenseKey}&status=success`,
        });
      }

      // Live Stripe Checkout
      const stripeResult = await createStripeCheckoutSession({
        currency: "USD",
        userId: targetUser.id,
        userEmail: targetUser.email,
        userName: targetUser.name || undefined,
        returnUrl,
      });

      return NextResponse.json(stripeResult);
    }
  } catch (err: any) {
    console.error("Unified checkout session error:", err);
    return NextResponse.json(
      { success: false, error: err.message || "Failed to initialize checkout session." },
      { status: 500 }
    );
  }
}
