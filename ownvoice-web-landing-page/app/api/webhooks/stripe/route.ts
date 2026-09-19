import { NextRequest, NextResponse } from "next/server";
import prisma from "@/lib/db";
import { generateLicenseKey } from "@/lib/license";
import { logEvent, logAudit } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const rawBody = await req.text();
    const sig = req.headers.get("stripe-signature");

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
