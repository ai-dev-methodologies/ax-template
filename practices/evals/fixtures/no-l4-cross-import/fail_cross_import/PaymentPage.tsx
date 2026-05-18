/**
 * FIXTURE: no-l4-cross-import/fail_cross_import
 * Demonstrates WRONG pattern: L4 payment domain importing from L4 auth domain.
 * L4 domains are independent vertical slices; cross-domain imports create
 * tight coupling and circular dependency risks.
 * Guard must catch: import from "templates/L4/auth" inside "templates/L4/payment".
 */
"use client";

// VIOLATION: L4/payment importing directly from L4/auth — breaks domain isolation.
// Auth state should be accessed through a shared hook (useCurrentUser) not by
// directly importing from the auth domain's implementation.
import { useAuthStore } from "templates/L4/auth/store/authStore";
import { AuthGuard } from "templates/L4/auth/components/AuthGuard";
import { getCurrentUser } from "templates/L4/auth/actions/userActions";

import { createPayment } from "../actions/paymentActions";

export default function PaymentPage() {
  // VIOLATION: reading auth domain's store directly from payment domain
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <div>Please log in</div>;
  }

  return (
    // VIOLATION: rendering auth domain component from payment domain
    <AuthGuard>
      <div>
        <h1>Payment for {user?.name}</h1>
      </div>
    </AuthGuard>
  );
}
