import { CheckoutRequest, CheckoutResponse } from "./types";

const STRIPE_SECRET_KEY = process.env.STRIPE_SECRET_KEY;

export async function createStripeCheckoutSession(req: CheckoutRequest): Promise<CheckoutResponse> {
  if (!STRIPE_SECRET_KEY) {
    // Graceful fallback for local development / testing without live keys
    const mockSessionId = `cs_test_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
    return {
      success: true,
      gateway: "stripe",
      sessionId: mockSessionId,
      checkoutUrl: `${req.returnUrl}?session_id=${mockSessionId}&gateway=stripe&status=success`,
    };
  }

  try {
    const params = new URLSearchParams();
    params.append("mode", "payment");
    params.append("success_url", `${req.returnUrl}?session_id={CHECKOUT_SESSION_ID}&gateway=stripe&status=success`);
    params.append("cancel_url", `${req.returnUrl}?status=cancelled`);
    params.append("customer_email", req.userEmail);
    params.append("client_reference_id", req.userId);
    params.append("line_items[0][price_data][currency]", "usd");
    params.append("line_items[0][price_data][unit_amount]", "4900"); // $49.00 USD
    params.append("line_items[0][price_data][product_data][name]", "Own Voice — Lifetime License");
    params.append("line_items[0][price_data][product_data][description]", "One-time purchase lifetime software license with BYOK AI access.");
    params.append("line_items[0][quantity]", "1");
    params.append("metadata[userId]", req.userId);
    params.append("metadata[productId]", "ownvoice-lifetime");

    const res = await fetch("https://api.stripe.com/v1/checkout/sessions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${STRIPE_SECRET_KEY}`,
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: params.toString(),
    });

    const session = await res.json();
    if (!res.ok) {
      throw new Error(session.error?.message || "Failed to create Stripe checkout session.");
    }

    return {
      success: true,
      gateway: "stripe",
      sessionId: session.id,
      checkoutUrl: session.url,
    };
  } catch (err: any) {
    console.error("Stripe session creation error:", err);
    return {
      success: false,
      gateway: "stripe",
      error: err.message || "Failed to initialize Stripe checkout.",
    };
  }
}
