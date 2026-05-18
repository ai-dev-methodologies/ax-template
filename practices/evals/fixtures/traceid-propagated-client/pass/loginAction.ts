/**
 * FIXTURE: traceid-propagated-client/pass
 * Demonstrates CORRECT pattern: Server Action propagates traceId in error path.
 * Client receives the traceId so the user/support team can correlate the failure
 * with the server's structured log entry.
 */
"use server";

import { headers } from "next/headers";

interface LoginResult {
  success: boolean;
  error?: string;
  // CORRECT: traceId always present so client can display "Error ref: <traceId>"
  traceId?: string;
}

export async function loginAction(formData: FormData): Promise<LoginResult> {
  const email = formData.get("email") as string;
  const password = formData.get("password") as string;
  const traceId = (await headers()).get("x-trace-id") ?? crypto.randomUUID();

  try {
    await authenticate(email, password);
    return { success: true, traceId };
  } catch (err) {
    // CORRECT: traceId propagated to client — links client error UI to server logs
    return {
      success: false,
      error: "Authentication failed",
      traceId,
    };
  }
}

async function authenticate(email: string, password: string): Promise<void> {
  throw new Error("Not implemented");
}
