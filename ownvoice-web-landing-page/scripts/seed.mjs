import crypto from "crypto";
import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

const prisma = new PrismaClient();

/** Generates a license key in the same format as lib/license.ts. */
function generateLicenseKey() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const segments = [];
  for (let s = 0; s < 4; s++) {
    let segment = "";
    while (segment.length < 4) {
      const [byte] = crypto.randomBytes(1);
      if (byte < 256 - (256 % chars.length)) segment += chars[byte % chars.length];
    }
    segments.push(segment);
  }
  return `OV-${segments.join("-")}`;
}

/**
 * Resolves the admin credentials used by the seed.
 *
 * A hardcoded `admin@ownvoice.ai` / `AdminPassword123!` account (and its
 * published license key) would be a public backdoor into any seeded database,
 * so the password is either taken from SEED_ADMIN_PASSWORD or generated once
 * and printed. Set SEED_SKIP_ADMIN=true to seed prompts only.
 */
function resolveAdminCredentials() {
  if (process.env.SEED_SKIP_ADMIN === "true") return null;

  const email = (process.env.SEED_ADMIN_EMAIL || "admin@ownvoice.ai").trim().toLowerCase();
  const provided = process.env.SEED_ADMIN_PASSWORD;
  const generated = !provided;

  if (provided && provided.length < 12) {
    throw new Error("SEED_ADMIN_PASSWORD must be at least 12 characters long.");
  }

  return {
    email,
    password: provided || crypto.randomBytes(18).toString("base64url"),
    generated,
  };
}

async function main() {
  console.log("Seeding database...");

  // Create the admin user (credentials from env, or freshly generated)
  const credentials = resolveAdminCredentials();

  if (credentials) {
    const adminEmail = credentials.email;
    const existingAdmin = await prisma.user.findUnique({
      where: { email: adminEmail },
    });

    if (!existingAdmin) {
      const passwordHash = await bcrypt.hash(credentials.password, 10);
      const admin = await prisma.user.create({
        data: {
          email: adminEmail,
          passwordHash,
          name: "Admin",
          role: "ADMIN",
          status: "ACTIVE",
          settings: {
            create: {
              defaultProvider: "openai",
              defaultModel: "gpt-4o",
              defaultTone: "NATURAL",
            },
          },
        },
      });

      // License key is random per install: a key published in source code
      // would let anyone activate the desktop app for free.
      await prisma.license.create({
        data: {
          userId: admin.id,
          licenseKey: generateLicenseKey(),
          productId: "ownvoice-lifetime",
          status: "ACTIVE",
        },
      });

      console.log(`Admin user created: ${adminEmail}`);
      if (credentials.generated) {
        console.log(`Generated admin password (store it now): ${credentials.password}`);
      } else {
        console.log("Admin password taken from SEED_ADMIN_PASSWORD.");
      }
    }
  }

  // Create default prompts
  const defaultPrompts = [
    {
      slug: "email",
      name: "Professional Email",
      description: "Converts voice thoughts into clean, structured email drafts.",
      systemPrompt: "Format as an executive business email with appropriate Subject line, greeting, succinct body paragraphs, and professional sign-off.",
    },
    {
      slug: "message",
      name: "Casual Message",
      description: "Formats speech into natural WhatsApp/Slack messages.",
      systemPrompt: "Format as a friendly, punchy, conversational chat message without corporate jargon.",
    },
    {
      slug: "summary",
      name: "Executive Summary",
      description: "Distills rambling speech into key takeaways and action items.",
      systemPrompt: "Extract 3-5 bulleted key points followed by explicit next action items.",
    },
    {
      slug: "code",
      name: "Code & Terminal",
      description: "Translates spoken instructions into commands or code snippets.",
      systemPrompt: "Output clean code or terminal commands with brief explanatory comments.",
    },
  ];

  for (const p of defaultPrompts) {
    await prisma.prompt.upsert({
      where: { slug: p.slug },
      update: {},
      create: p,
    });
  }

  console.log("Seed completed successfully.");
}

main()
  .catch((e) => {
    console.error("Seed error:", e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
