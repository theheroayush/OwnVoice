import { NextRequest, NextResponse } from "next/server";
import { verifyLicenseKey } from "@/lib/license";

export async function POST(req: NextRequest) {
  try {
    const { licenseKey } = await req.json();

    if (!licenseKey) {
      return NextResponse.json(
        { success: false, error: "License key is required." },
        { status: 400 }
      );
    }

    const result = await verifyLicenseKey(licenseKey);

    return NextResponse.json({
      success: result.isValid,
      ...result,
    });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || "Failed to verify license." },
      { status: 500 }
    );
  }
}
