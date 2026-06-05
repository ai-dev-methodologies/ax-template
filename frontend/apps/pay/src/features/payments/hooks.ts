import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  paymentClient,
  type CreatePaymentInput,
  type Payment,
  type PaymentPage,
  type Refund,
  type RefundInput,
} from '@/lib/api/paymentClient';

export const paymentKeys = {
  all: ['payments'] as const,
  list: (page: number, size: number) => [...paymentKeys.all, 'list', page, size] as const,
};

/** Paged payment ledger. */
export function usePayments(page = 0, size = 20) {
  return useQuery<PaymentPage>({
    queryKey: paymentKeys.list(page, size),
    queryFn: () => paymentClient.list(page, size),
  });
}

/**
 * Create a payment with a CALLER-OWNED idempotency key. The checkout form holds
 * one key per attempt, so re-submitting the same attempt REPLAYS (no double
 * charge). A "new payment" action rotates the key.
 */
export function useCreatePayment() {
  const queryClient = useQueryClient();
  return useMutation<Payment, Error, { input: CreatePaymentInput; idempotencyKey: string }>({
    mutationFn: ({ input, idempotencyKey }) => paymentClient.create(input, idempotencyKey),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: paymentKeys.all });
    },
  });
}

/** Refund a captured payment (full when amount omitted). */
export function useRefundPayment() {
  const queryClient = useQueryClient();
  return useMutation<Refund, Error, { id: string; input: RefundInput }>({
    mutationFn: ({ id, input }) => paymentClient.refund(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: paymentKeys.all });
    },
  });
}
