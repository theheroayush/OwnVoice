import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth";
import { activateLicenseForUser, verifyLicenseKey } from "@/lib/license";
import { logEvent, logAudit } from "@/lib/analytics";

export async function POST(req: NextRequest) {
  try {
    const user = await getCurrentUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "Unauthorized." }, { status: 401 });
    }

    const { licenseKey } = await req.json();
    if (!licenseKey) {
      return NextResponse.json(
        { success: false, error: "License key is required." },
        { status: 400 }
      );
    }

    const verification = await verifyLicenseKey(licenseKey);
    if (!verification.isValid) {
      return NextResponse.json(
        { success: false, error: verification.message },
        { status: 400 }
      );
    }

    // Bind license to this user
    const activated = await activateLicenseForUser(user.id, licenseKey);

    await logEvent("license_activated", user.id, { licenseKey: activated.licenseKey });
    await logAudit("LICENSE_ACTIVATED", user.id);

    return NextResponse.json({
      success: true,
      license: activated,
      message: "License activated successfully! Enjoy lifetime access.",
    });
  } catch (error: any) {
    console.error("License activation error:", error);
    return NextResponse.json(
      { success: false, error: error.message || "Failed to activate license." },
      { status: 500 }
    );
  }
}
