import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

const prisma = new PrismaClient();

async function main() {
  console.log("Seeding database...");

  // Create default admin user
  const adminEmail = "admin@ownvoice.ai";
  const existingAdmin = await prisma.user.findUnique({
    where: { email: adminEmail },
  });

  if (!existingAdmin) {
    const passwordHash = await bcrypt.hash("AdminPassword123!", 10);
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

    // Create admin license
    await prisma.license.create({
      data: {
        userId: admin.id,
        licenseKey: "OV-ADM1-NIST-RATO-R001",
        productId: "ownvoice-lifetime",
        status: "ACTIVE",
      },
    });

    console.log("Admin user created: admin@ownvoice.ai (Pass: AdminPassword123!)");
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
