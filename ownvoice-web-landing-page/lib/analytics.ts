import prisma from "./db";

export type EventType =
  | "landing_view"
  | "pricing_view"
  | "demo_started"
  | "signup_started"
  | "signup_completed"
  | "purchase_started"
  | "purchase_completed"
  | "onboarding_started"
  | "provider_connected"
  | "provider_connection_failed"
  | "recording_started"
  | "recording_completed"
  | "transcription_completed"
  | "generation_completed"
  | "generation_failed"
  | "copy_clicked"
  | "rewrite_clicked"
  | "history_opened"
  | "license_activated"
  | "license_revoked";

export async function logEvent(
  eventType: EventType,
  userId?: string | null,
  metadata?: Record<string, any>
) {
  try {
    await prisma.usageEvent.create({
      data: {
        eventType,
        userId: userId || null,
        metadata: metadata ? JSON.stringify(metadata) : null,
      },
    });
  } catch (error) {
    // Non-blocking telemetry
    console.error("Telemetry error:", error);
  }
}

export async function logAudit(
  action: string,
  userId?: string | null,
  ipAddress?: string | null,
  userAgent?: string | null
) {
  try {
    await prisma.auditLog.create({
      data: {
        action,
        userId: userId || null,
        ipAddress: ipAddress || null,
        userAgent: userAgent || null,
      },
    });
  } catch (error) {
    console.error("Audit log error:", error);
  }
}
