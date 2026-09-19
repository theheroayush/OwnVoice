declare module "@cashfreepayments/cashfree-js" {
  export interface CashfreeInitOptions {
    mode: "sandbox" | "production";
  }

  export interface CashfreeCheckoutOptions {
    paymentSessionId: string;
    redirectTarget?: "_self" | "_blank" | "_modal" | HTMLElement;
    returnUrl?: string;
  }

  export interface CashfreeInstance {
    checkout(options: CashfreeCheckoutOptions): Promise<{
      error?: { message: string; code?: string };
      redirect?: boolean;
    }>;
  }

  export function load(options: CashfreeInitOptions): Promise<CashfreeInstance | null>;
}
