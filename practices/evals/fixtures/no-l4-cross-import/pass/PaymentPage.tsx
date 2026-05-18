/**
 * FIXTURE: no-l4-cross-import/pass
 * Demonstrates CORRECT pattern: L4 payment domain uses shared hooks/context
 * instead of importing directly from another L4 domain.
 */
"use client";

// CORRECT: import from shared hooks, not from another L4 domain
import { useCurrentUser } from "hooks/useCurrentUser";
import { useRequireAuth } from "hooks/useRequireAuth";

import { createPayment } from "../actions/paymentActions";

export default function PaymentPage() {
  // CORRECT: shared hook provides auth state without coupling to L4/auth internals
  const user = useCurrentUser();
  useRequireAuth(); // redirects to login if not authenticated

  return (
    <div>
      <h1>Payment for {user?.name}</h1>
      {/* payment-specific UI only — no auth domain components */}
    </div>
  );
}
