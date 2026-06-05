/**
 * Feature-flag admin client. Backend: FeatureFlagAdminController
 * (/api/v1/admin/feature-flags). Auth: ROLE_ADMIN. Toggle = PATCH {enabled}.
 * Domain client — composes the shared @ax/core authed fetch.
 */
import { apiFetch } from '@ax/core';

export interface FeatureFlag {
  name: string;
  enabled: boolean;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FeatureFlagPage {
  content: FeatureFlag[];
  page: number;
  size: number;
  totalElements: number;
}

export interface CreateFeatureFlagInput {
  name: string;
  enabled: boolean;
  description?: string;
}

const BASE = '/v1/admin/feature-flags';

export const featureFlagClient = {
  list: (page = 0, size = 50): Promise<FeatureFlagPage> =>
    apiFetch<FeatureFlagPage>(BASE, { query: { page, size } }),

  create: (input: CreateFeatureFlagInput): Promise<FeatureFlag> =>
    apiFetch<FeatureFlag>(BASE, {
      method: 'POST',
      body: { name: input.name, enabled: input.enabled, description: input.description ?? null },
    }),

  setEnabled: (name: string, enabled: boolean): Promise<FeatureFlag> =>
    apiFetch<FeatureFlag>(`${BASE}/${encodeURIComponent(name)}`, {
      method: 'PATCH',
      body: { enabled },
    }),
};
