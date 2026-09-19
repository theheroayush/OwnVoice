export type PaymentGateway = "stripe" | "cashfree";

export interface CheckoutRequest {
  currency: "USD" | "INR";
  userId: string;
  userEmail: string;
  userName?: string;
  userPhone?: string;
  returnUrl: string;
}

export interface CheckoutResponse {
  success: boolean;
  gateway: PaymentGateway;
  sessionId?: string;
  orderId?: string;
  checkoutUrl?: string;
  paymentSessionId?: string; // For Cashfree SDK dropin
  error?: string;
}
