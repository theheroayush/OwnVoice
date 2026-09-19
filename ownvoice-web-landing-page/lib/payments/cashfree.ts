import { CheckoutRequest, CheckoutResponse } from "./types";

const CASHFREE_APP_ID = process.env.CASHFREE_APP_ID;
const CASHFREE_SECRET_KEY = process.env.CASHFREE_SECRET_KEY;
const CASHFREE_ENV = process.env.CASHFREE_ENVIRONMENT || "sandbox"; // sandbox | production

const CASHFREE_BASE_URL =
  CASHFREE_ENV === "production"
    ? "https://api.cashfree.com/pg"
    : "https://sandbox.cashfree.com/pg";

export async function createCashfreeOrder(req: CheckoutRequest): Promise<CheckoutResponse> {
  const orderId = `order_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;

  if (!CASHFREE_APP_ID || !CASHFREE_SECRET_KEY) {
    // Graceful fallback for local development / testing without live keys
    return {
      success: true,
      gateway: "cashfree",
      orderId,
      paymentSessionId: `session_cf_mock_${Date.now()}`,
      checkoutUrl: `${req.returnUrl}?order_id=${orderId}&gateway=cashfree&status=success`,
    };
  }

  try {
    // Cashfree production requires return_url to strictly use HTTPS
    let sanitizedReturnUrl = req.returnUrl;
    if (sanitizedReturnUrl.startsWith("http://")) {
      sanitizedReturnUrl = sanitizedReturnUrl.replace("http://", "https://");
    }

    const payload = {
      order_id: orderId,
      order_amount: 1499.0, // Rs 1,499 INR
      order_currency: "INR",
      customer_details: {
        customer_id: req.userId,
        customer_email: req.userEmail,
        customer_phone: req.userPhone || "9999999999",
        customer_name: req.userName || req.userEmail.split("@")[0],
      },
      order_meta: {
        return_url: `${sanitizedReturnUrl}?order_id={order_id}&gateway=cashfree&email=${encodeURIComponent(
          req.userEmail
        )}&status=success`,
        payment_methods: "cc,dc,upi,nb",
      },
      order_note: "Own Voice Lifetime Software License (BYOK)",
    };

    const res = await fetch(`${CASHFREE_BASE_URL}/orders`, {
      method: "POST",
      headers: {
        "x-client-id": CASHFREE_APP_ID,
        "x-client-secret": CASHFREE_SECRET_KEY,
        "x-api-version": "2023-08-01",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    const data = await res.json();
    if (!res.ok || !data.payment_session_id) {
      throw new Error(data.message || "Failed to create Cashfree payment order.");
    }

    const hostedCheckoutBase =
      CASHFREE_ENV === "production"
        ? "https://payments.cashfree.com/forms"
        : "https://sandbox.cashfree.com/forms";

    return {
      success: true,
      gateway: "cashfree",
      orderId: data.order_id,
      paymentSessionId: data.payment_session_id,
      checkoutUrl: `${hostedCheckoutBase}/${data.payment_session_id}`,
    };
  } catch (err: any) {
    console.error("Cashfree order creation error:", err);
    return {
      success: false,
      gateway: "cashfree",
      error: err.message || "Failed to initialize Cashfree payment.",
    };
  }
}

export async function verifyCashfreeOrder(orderId: string): Promise<{
  paid: boolean;
  orderStatus: string;
  orderAmount: number;
  customerEmail?: string;
  customerId?: string;
  cfOrderId?: string;
}> {
  if (!CASHFREE_APP_ID || !CASHFREE_SECRET_KEY) {
    return {
      paid: false,
      orderStatus: "MOCK_NOT_CONFIGURED",
      orderAmount: 0,
    };
  }

  try {
    const res = await fetch(`${CASHFREE_BASE_URL}/orders/${orderId}`, {
      method: "GET",
      headers: {
        "x-client-id": CASHFREE_APP_ID,
        "x-client-secret": CASHFREE_SECRET_KEY,
        "x-api-version": "2023-08-01",
      },
    });

    if (!res.ok) {
      return {
        paid: false,
        orderStatus: `HTTP_${res.status}`,
        orderAmount: 0,
      };
    }

    const data = await res.json();
    const isPaid = data.order_status === "PAID";

    return {
      paid: isPaid,
      orderStatus: data.order_status || "UNKNOWN",
      orderAmount: data.order_amount || 0,
      customerEmail: data.customer_details?.customer_email || undefined,
      customerId: data.customer_details?.customer_id || undefined,
      cfOrderId: data.cf_order_id ? String(data.cf_order_id) : undefined,
    };
  } catch (err: any) {
    console.error("Failed to verify Cashfree order status:", err);
    return {
      paid: false,
      orderStatus: "NETWORK_ERROR",
      orderAmount: 0,
    };
  }
}
