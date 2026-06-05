import type { ApiError } from '@ax/core';

/**
 * Narrow an unknown error (TanStack Query `error`, a thrown apiFetch ApiError,
 * etc.) to a user-facing message. apiFetch already folds RFC 9457 problem+json
 * `detail`/`title` into Error.message, so this just safely reads it.
 */
export function errorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error) {
    const message = (error as { message: unknown }).message;
    if (typeof message === 'string' && message.length > 0) return message;
  }
  return '문제가 발생했어요. 잠시 후 다시 시도해 주세요.';
}

/** Read the stable backend `code` (RFC 9457 extension) when present. */
export function errorCode(error: unknown): string | undefined {
  const code = (error as ApiError | undefined)?.code;
  return typeof code === 'string' ? code : undefined;
}
