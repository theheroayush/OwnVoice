import { NextRequest, NextResponse } from "next/server";
import fs from "fs";
import path from "path";

export const runtime = "nodejs";

/**
 * Serves installer artifacts from the repository-level `releases/` directory.
 *
 * The marketing download page links to `/releases/<file>`, but Next.js only
 * serves files that live inside `public/` — and copying ~250 MB of installers
 * into the app bundle is not an option. This route streams them from disk
 * instead, with an allow-list that prevents path traversal.
 */

const ALLOWED_FILENAME = /^[A-Za-z0-9][A-Za-z0-9._-]*\.(zip|apk|exe|msi|dmg|tar\.gz)$/i;

const CONTENT_TYPES: Record<string, string> = {
  ".zip": "application/zip",
  ".apk": "application/vnd.android.package-archive",
  ".exe": "application/vnd.microsoft.portable-executable",
  ".msi": "application/x-msi",
  ".dmg": "application/x-apple-diskimage",
  ".gz": "application/gzip",
};

function candidateDirs(): string[] {
  const cwd = process.cwd(); // <repo>/web
  return [
    path.join(cwd, "public", "releases"),
    path.join(cwd, "..", "releases"),
    path.join(cwd, "private_storage", "releases"),
  ];
}

export async function GET(
  _req: NextRequest,
  ctx: { params: Promise<{ file: string }> }
) {
  const { file } = await ctx.params;

  if (!file || !ALLOWED_FILENAME.test(file)) {
    return NextResponse.json(
      { success: false, error: "Invalid release filename." },
      { status: 400 }
    );
  }

  for (const dir of candidateDirs()) {
    const resolved = path.resolve(dir, file);
    // Defence in depth: the resolved path must stay inside the releases dir.
    if (!resolved.startsWith(path.resolve(dir) + path.sep)) continue;
    if (!fs.existsSync(resolved)) continue;

    const stat = fs.statSync(resolved);
    if (!stat.isFile()) continue;

    const stream = fs.createReadStream(resolved);
    const ext = path.extname(file).toLowerCase();

    return new NextResponse(stream as unknown as ReadableStream, {
      status: 200,
      headers: {
        "Content-Type": CONTENT_TYPES[ext] ?? "application/octet-stream",
        "Content-Length": String(stat.size),
        "Content-Disposition": `attachment; filename="${file}"`,
        "Cache-Control": "public, max-age=3600",
      },
    });
  }

  return NextResponse.json(
    {
      success: false,
      error: `Release artifact '${file}' was not found on the server.`,
      hint: "Installers are published to the repository's releases/ directory.",
    },
    { status: 404 }
  );
}
