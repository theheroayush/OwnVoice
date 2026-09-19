import { NextRequest, NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import { verifyDownloadToken } from "@/lib/download-token";
import prisma from "@/lib/db";

export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const token = searchParams.get("token");
    const requestedOs = (searchParams.get("os") || "windows").toLowerCase();

    if (!token) {
      return NextResponse.json(
        {
          success: false,
          error: "Access Denied: Missing secure download token. Direct downloading is disabled to prevent unauthorized copying.",
          code: "MISSING_TOKEN",
          solution: "Please use the download link provided on your purchase confirmation page, or retrieve your link at /download/lookup.",
        },
        { status: 401 }
      );
    }

    // 1. Verify HMAC cryptographic signature and 24-hour expiration
    const verification = verifyDownloadToken(token);
    if (!verification.isValid || !verification.payload) {
      return NextResponse.json(
        {
          success: false,
          error: verification.error || "Access Denied: Invalid or expired download token.",
          code: "INVALID_OR_EXPIRED_TOKEN",
          solution: "Download links expire after 24 hours for security. You can generate a fresh download link anytime at /download/lookup with your purchase email.",
        },
        { status: 403 }
      );
    }

    const { licenseKey, email } = verification.payload;

    // 2. Anti-Piracy Check: Verify that the License exists and is ACTIVE
    const license = await prisma.license.findUnique({
      where: { licenseKey },
      include: { user: true },
    });

    // If license is revoked or suspended, immediately block the download
    if (license && license.status !== "ACTIVE") {
      return NextResponse.json(
        {
          success: false,
          error: `License ${licenseKey} is currently ${license.status.toLowerCase()}. Downloads are disabled.`,
          code: "LICENSE_INACTIVE",
        },
        { status: 403 }
      );
    }

    // 3. Resolve the secure binary file on disk (never in public/ directory)
    const possibleDirs = [
      path.join(process.cwd(), "private_storage", "installers"),
      path.join(process.cwd(), "..", "releases"),
      path.join(process.cwd(), "..", "dist"),
    ];

    let targetFilename = "";
    let downloadAsName = "";

    if (requestedOs === "android") {
      targetFilename = "OwnVoice.apk";
      downloadAsName = "OwnVoice-Mobile-v3.0.0.apk";
    } else if (requestedOs === "mac") {
      targetFilename = "OwnVoice-v2.0.0-Mac.zip";
      downloadAsName = "OwnVoice-v2.0.0-macOS.zip";
    } else {
      // Default: Windows
      targetFilename = "OwnVoice-v2.0.0-Windows.zip";
      downloadAsName = "OwnVoice-v2.0.0-Windows.zip";
    }

    let resolvedFilePath: string | null = null;
    for (const dir of possibleDirs) {
      const candidate = path.join(dir, targetFilename);
      if (fs.existsSync(candidate)) {
        resolvedFilePath = candidate;
        break;
      }
    }

    // Fallback search for android/windows alternatives if exact name differs
    if (!resolvedFilePath) {
      for (const dir of possibleDirs) {
        if (!fs.existsSync(dir)) continue;
        const files = fs.readdirSync(dir);
        if (requestedOs === "android") {
          const apk = files.find((f) => f.endsWith(".apk"));
          if (apk) {
            resolvedFilePath = path.join(dir, apk);
            downloadAsName = apk;
            break;
          }
        } else if (requestedOs === "windows") {
          const win = files.find((f) => f.includes("Windows") && f.endsWith(".zip"));
          if (win) {
            resolvedFilePath = path.join(dir, win);
            downloadAsName = win;
            break;
          }
        }
      }
    }

    if (!resolvedFilePath || !fs.existsSync(resolvedFilePath)) {
      return NextResponse.json(
        {
          success: false,
          error: `The requested package for ${requestedOs} is currently being prepared for production release.`,
          code: "FILE_BEING_PREPARED",
          advice: "Please use the Web Workspace at /app or try the Windows release while the macOS binary package finishes notarization.",
        },
        { status: 404 }
      );
    }

    const stat = fs.statSync(resolvedFilePath);
    const fileStream = fs.createReadStream(resolvedFilePath);

    // Stream directly with anti-caching & strict binary headers
    const stream = new ReadableStream({
      start(controller) {
        fileStream.on("data", (chunk) => controller.enqueue(chunk));
        fileStream.on("end", () => controller.close());
        fileStream.on("error", (err) => controller.error(err));
      },
      cancel() {
        fileStream.destroy();
      },
    });

    return new NextResponse(stream, {
      status: 200,
      headers: {
        "Content-Type": "application/octet-stream",
        "Content-Length": stat.size.toString(),
        "Content-Disposition": `attachment; filename="${downloadAsName}"`,
        "Cache-Control": "no-store, no-cache, must-revalidate, private",
        Pragma: "no-cache",
        Expires: "0",
      },
    });
  } catch (err: any) {
    console.error("Secure download streaming error:", err);
    return NextResponse.json(
      { success: false, error: err.message || "Failed to stream download securely." },
      { status: 500 }
    );
  }
}
