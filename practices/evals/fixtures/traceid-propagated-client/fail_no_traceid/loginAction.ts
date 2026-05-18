/**
 * FIXTURE: traceid-propagated-client/fail_no_traceid
 * Demonstrates WRONG pattern: Server Action returns an error without traceId.
 * The client receives no correlation handle — cannot reference the failure
 * in a support ticket or relate it to server logs.
 * Guard must catch: Server Action error branch missing traceId in return value.
 */
"use server";

interface LoginResult {
  success: boolean;
  error?: string;
  // MISSING: traceId field — client cannot correlate with server logs
}

export async function loginAction(formData: FormData): Promise<LoginResult> {
  const email = formData.get("email") as string;
  const password = formData.get("password") as string;

  try {
    await authenticate(email, password);
    return { success: true };
  } catch (err) {
    // VIOLATION: error returned without traceId.
    // Support team has no handle to find the server log for this failure.
    return {
      success: false,
      error: "Authentication failed",
      // missing: traceId: headers().get("x-trace-id") ?? generateTraceId()
    };
  }
}

async function authenticate(email: string, password: string): Promise<void> {
  throw new Error("Not implemented");
}
