import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  billingClient,
  type Plan,
  type Subscription,
  type SubscriptionList,
} from '@/lib/api/billingClient';

export const billingKeys = {
  plans: ['billing', 'plans'] as const,
  subscriptions: ['billing', 'subscriptions'] as const,
};

/** Available plans (admin-managed; demo account is ADMIN). */
export function usePlans() {
  return useQuery<Plan[]>({
    queryKey: billingKeys.plans,
    queryFn: () => billingClient.listPlans(),
  });
}

/** The current user's subscriptions. */
export function useSubscriptions() {
  return useQuery<SubscriptionList>({
    queryKey: billingKeys.subscriptions,
    queryFn: () => billingClient.listSubscriptions(),
  });
}

export function useSubscribe() {
  const queryClient = useQueryClient();
  return useMutation<Subscription, Error, { planId: string; provider: string }>({
    mutationFn: ({ planId, provider }) => billingClient.subscribe(planId, provider),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: billingKeys.subscriptions });
    },
  });
}

export function useCancelSubscription() {
  const queryClient = useQueryClient();
  return useMutation<Subscription, Error, { id: string }>({
    mutationFn: ({ id }) => billingClient.cancel(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: billingKeys.subscriptions });
    },
  });
}
