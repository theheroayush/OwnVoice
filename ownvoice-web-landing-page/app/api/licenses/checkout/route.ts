import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { generateLicenseKey } from "@/lib/license";
import { logEvent, logAudit } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json(
        { success: false, error: "Please log in or sign up before completing purchase." },
        { status: 401 }
      );
    }

    const body = await req.json().catch(() => ({}));
    const currency = body.currency || "USD";
    const amount = currency === "INR" ? 399900 : 4900; // Rs 3,999 or $49.00
    const provider = body.provider || "stripe";

    const transactionId = `tx_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    const newLicenseKey = generateLicenseKey();

    // Perform transaction to record purchase and license in one atomic batch
    const [purchase, license] = await prisma.$transaction([
      prisma.purchase.create({
        data: {
          userId: user.id,
          provider,
          transactionId,
          amount,
          currency,
          productId: "ownvoice-lifetime",
          status: "COMPLETED",
          metadata: JSON.stringify({ source: "web_checkout" }),
        },
      }),
      prisma.license.create({
        data: {
          userId: user.id,
          licenseKey: newLicenseKey,
          productId: "ownvoice-lifetime",
          status: "ACTIVE",
          activatedAt: new Date(),
        },
      }),
    ]);

    await prisma.purchase.update({
      where: { id: purchase.id },
      data: { license: { connect: { id: license.id } } },
    });

    await logEvent("purchase_completed", user.id, { transactionId, amount, currency });
    await logAudit("PURCHASE_COMPLETED", user.id);

    return NextResponse.json({
      success: true,
      licenseKey: newLicenseKey,
      transactionId,
      message: "Congratulations! You now own Own Voice for life.",
    });
  } catch (error: any) {
    console.error("Checkout error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to process checkout." },
      { status: 500 }
    );
  }
}
