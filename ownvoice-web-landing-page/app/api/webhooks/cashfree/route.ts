import { NextRequest, NextResponse } from "next/server";
import prisma from "@/lib/db";
import { generateLicenseKey } from "@/lib/license";
import { logEvent, logAudit } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const eventType = body?.type;

    if (eventType === "PAYMENT_SUCCESS_WEBHOOK") {
      const order = body.data?.order;
      const customer = body.data?.customer_details;
      const userId = customer?.customer_id;

      if (userId) {
        const transactionId = order.order_id || `tx_cf_${Date.now()}`;
        const newLicenseKey = generateLicenseKey();

        await prisma.$transaction([
          prisma.purchase.create({
            data: {
              userId,
              provider: "cashfree",
              transactionId,
              amount: Math.round((order.order_amount || 1499) * 100),
              currency: order.order_currency || "INR",
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
