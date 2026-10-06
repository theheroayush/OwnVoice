import { NextRequest, NextResponse } from "next/server";
import prisma from "@/lib/db";
import { generateLicenseKey } from "@/lib/license";
import { logEvent, logAudit } from "@/lib/analytics";
import { verifyCashfreeSignature } from "@/lib/webhook-verify";

export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  try {
    // The signature must be computed over the *raw* body, so read text first.
    const rawBody = await req.text();

    const verification = verifyCashfreeSignature(
      rawBody,
      req.headers.get("x-webhook-signature"),
      req.headers.get("x-webhook-timestamp"),
      process.env.CASHFREE_SECRET_KEY
    );
    if (!verification.ok) {
      console.error("Rejected Cashfree webhook:", verification.reason);
      return NextResponse.json({ error: verification.reason }, { status: 401 });
    }

    let body: any;
    try {
      body = JSON.parse(rawBody);
    } catch {
      return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
    }
    const eventType = body?.type;

    if (eventType === "PAYMENT_SUCCESS_WEBHOOK") {
      const order = body.data?.order;
      const customer = body.data?.customer_details;
      const userId = customer?.customer_id;

      if (userId) {
        const transactionId = order?.order_id || `tx_cf_${Date.now()}`;

        // Idempotency: Cashfree retries webhooks until they get a 200.
        const existing = await prisma.purchase.findUnique({ where: { transactionId } });
        if (existing) {
          return NextResponse.json({ status: "OK", duplicate: true });
        }

        const newLicenseKey = generateLicenseKey();

        await prisma.$transaction([
          prisma.purchase.create({
            data: {
              userId,
              provider: "cashfree",
              transactionId,
              amount: Math.round(Number(order?.order_amount ?? 1499) * 100),
              currency: order?.order_currency || "INR",
              productId: "ownvoice-lifetime",
              status: "COMPLETED",
              metadata: JSON.stringify(body),
            },
          }),
          prisma.license.create({
            data: {
              userId,
              licenseKey: newLicenseKey,
              productId: "ownvoice-lifetime",
              status: "ACTIVE",
              activatedAt: new Date(),
            },
          }),
        ]);

        await logEvent("purchase_completed", userId, { gateway: "cashfree", transactionId });
        await logAudit("LICENSE_PURCHASED_CASHFREE", userId);
      }
    }

    return NextResponse.json({ status: "OK" });
  } catch (err: any) {
    console.error("Cashfree webhook error:", err);
    return NextResponse.json({ error: err.message }, { status: 500 });
  }
}

export async function GET() {
  return NextResponse.json({ error: "Method Not Allowed" }, { status: 405 });
}
