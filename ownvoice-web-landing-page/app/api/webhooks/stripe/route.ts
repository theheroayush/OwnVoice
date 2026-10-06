import { NextRequest, NextResponse } from "next/server";
import prisma from "@/lib/db";
import { generateLicenseKey } from "@/lib/license";
import { logEvent, logAudit } from "@/lib/analytics";
import { verifyStripeSignature } from "@/lib/webhook-verify";

export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  try {
    const rawBody = await req.text();
    const sig = req.headers.get("stripe-signature");

    // Fail closed: without a verified signature anyone could POST a fake
    // "checkout.session.completed" event and mint a free lifetime license.
    const verification = verifyStripeSignature(
      rawBody,
      sig,
      process.env.STRIPE_WEBHOOK_SECRET
    );
    if (!verification.ok) {
      console.error("Rejected Stripe webhook:", verification.reason);
      return NextResponse.json({ error: verification.reason }, { status: 401 });
    }

    let event: any;
    try {
      event = JSON.parse(rawBody);
    } catch {
      return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
    }

    if (event.type === "checkout.session.completed") {
      const session = event.data?.object;
      const userId = session?.client_reference_id || session?.metadata?.userId;

      if (userId) {
        const transactionId = session.id || `tx_stripe_${Date.now()}`;

        // Idempotency: gateways retry deliveries, so never create a second
        // purchase/license for a transaction we already recorded.
        const existing = await prisma.purchase.findUnique({ where: { transactionId } });
        if (existing) {
          return NextResponse.json({ received: true, duplicate: true });
        }

        const newLicenseKey = generateLicenseKey();

        await prisma.$transaction([
          prisma.purchase.create({
            data: {
              userId,
              provider: "stripe",
              transactionId,
              amount: session.amount_total || 4900,
              currency: session.currency || "USD",
              productId: "ownvoice-lifetime",
              status: "COMPLETED",
              metadata: JSON.stringify(session),
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

        await logEvent("purchase_completed", userId, { gateway: "stripe", transactionId });
        await logAudit("LICENSE_PURCHASED_STRIPE", userId);
      }
    }

    return NextResponse.json({ received: true });
  } catch (err: any) {
    console.error("Stripe webhook error:", err);
    return NextResponse.json({ error: err.message }, { status: 500 });
  }
}

export async function GET() {
  // Webhooks are POST-only; answering GET with 405 avoids leaking whether the
  // endpoint is configured.
  return NextResponse.json({ error: "Method Not Allowed" }, { status: 405 });
}
