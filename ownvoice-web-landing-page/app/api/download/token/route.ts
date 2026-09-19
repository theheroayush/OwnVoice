import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";
import prisma from "@/lib/db";
import { generateDownloadToken } from "@/lib/download-token";
import { generateLicenseKey } from "@/lib/license";
import { verifyCashfreeOrder } from "@/lib/payments/cashfree";
import { hashPassword } from "@/lib/auth";
import { logEvent, logAudit } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json().catch(() => ({}));
    const email = body.email?.trim().toLowerCase();
    const licenseKey = body.licenseKey?.trim().toUpperCase();
    const orderId = body.orderId?.trim();

    if (!email && !licenseKey && !orderId) {
      return NextResponse.json(
        { success: false, error: "Please provide an email, license key, or order ID." },
        { status: 400 }
      );
    }

    // 1. If orderId is provided, check if already recorded or verify live with gateway
    let license: any = null;

    if (orderId) {
      const existingPurchase = await prisma.purchase.findUnique({
        where: { transactionId: orderId },
        include: {
          user: {
            include: {
              licenses: {
                where: { status: "ACTIVE" },
                orderBy: { createdAt: "desc" },
              },
            },
          },
        },
      });

      if (existingPurchase && existingPurchase.user?.licenses?.length > 0) {
        license = {
          ...existingPurchase.user.licenses[0],
          user: existingPurchase.user,
        };
      } else if (orderId.startsWith("order_")) {
        // Verify with Cashfree live API
        const cfCheck = await verifyCashfreeOrder(orderId);
        if (cfCheck.paid && cfCheck.customerEmail) {
          const targetEmail = cfCheck.customerEmail.toLowerCase().trim();
          let user = await prisma.user.findUnique({
            where: { email: targetEmail },
            include: {
              licenses: {
                where: { status: "ACTIVE" },
                orderBy: { createdAt: "desc" },
              },
            },
          });

          if (!user) {
            const randomPassword = crypto.randomBytes(16).toString("hex");
            const passwordHash = await hashPassword(randomPassword);
            user = await prisma.user.create({
              data: {
                email: targetEmail,
                name: targetEmail.split("@")[0],
                passwordHash,
                role: "USER",
                status: "ACTIVE",
              },
              include: {
                licenses: {
                  where: { status: "ACTIVE" },
                  orderBy: { createdAt: "desc" },
                },
              },
            });
          }

          if (user.licenses.length > 0) {
            license = {
              ...user.licenses[0],
              user,
            };
          } else {
            const newKey = generateLicenseKey();
            const [createdPurchase, createdLicense] = await prisma.$transaction([
              prisma.purchase.create({
                data: {
                  userId: user.id,
                  provider: "cashfree",
                  transactionId: orderId,
                  amount: Math.round((cfCheck.orderAmount || 1499) * 100),
                  currency: "INR",
                  productId: "ownvoice-lifetime",
                  status: "COMPLETED",
                  metadata: JSON.stringify(cfCheck),
                },
              }),
              prisma.license.create({
                data: {
                  userId: user.id,
                  licenseKey: newKey,
                  productId: "ownvoice-lifetime",
                  status: "ACTIVE",
                  activatedAt: new Date(),
                },
              }),
            ]);

            license = {
              ...createdLicense,
              user,
            };

            await logEvent("purchase_completed", user.id, {
              gateway: "cashfree",
              transactionId: orderId,
            });
            await logAudit("LICENSE_PURCHASED_CASHFREE_VERIFIED", user.id);
          }
        }
      }
    }

    // 2. Lookup by direct licenseKey or email if not resolved via orderId
    if (!license && licenseKey) {
      license = await prisma.license.findUnique({
        where: { licenseKey },
        include: { user: true },
      });
    } else if (!license && email) {
      const user = await prisma.user.findUnique({
        where: { email },
        include: {
          licenses: {
            where: { status: "ACTIVE" },
            orderBy: { createdAt: "desc" },
          },
        },
      });

      if (user && user.licenses.length > 0) {
        license = {
          ...user.licenses[0],
          user,
        };
      }
    }

    if (!license || license.status !== "ACTIVE") {
      return NextResponse.json(
        {
          success: false,
          error: "No active lifetime license found for the provided details. Please check your purchase email or contact support.",
        },
        { status: 404 }
      );
    }

    // Generate 24-hour expiring download token
    const token = generateDownloadToken({
      licenseKey: license.licenseKey,
      email: license.user.email,
      validHours: 24,
    });

    const expiresAtDate = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();

    return NextResponse.json({
      success: true,
      licenseKey: license.licenseKey,
      email: license.user.email,
      token,
      expiresAt: expiresAtDate,
      downloadUrls: {
        windows: `/api/download/secure?token=${token}&os=windows`,
        mac: `/api/download/secure?token=${token}&os=mac`,
        android: `/api/download/secure?token=${token}&os=android`,
      },
    });
  } catch (err: any) {
    console.error("Token generation error:", err);
    return NextResponse.json(
      { success: false, error: err.message || "Failed to generate download token." },
      { status: 500 }
    );
  }
}
