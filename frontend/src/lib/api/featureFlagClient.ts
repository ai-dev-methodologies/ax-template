/**
 * Feature-flag admin client. Backend: FeatureFlagAdminController
 * (/api/v1/admin/feature-flags). Auth: ROLE_ADMIN. Toggle = PATCH {enabled}.
 */
import { apiFetch } from './enterpriseHttp';

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
  list: (page = 0, size = 50) =>
    apiFetch<FeatureFlagPage>(BASE, { query: { page, size } }),

  create: (input: CreateFeatureFlagInput) =>
    apiFetch<FeatureFlag>(BASE, {
      method: 'POST',
      body: { name: input.name, enabled: input.enabled, description: input.description ?? null },
    }),

  setEnabled: (name: string, enabled: boolean) =>
    apiFetch<FeatureFlag>(`${BASE}/${encodeURIComponent(name)}`, {
      method: 'PATCH',
      body: { enabled },
    }),
};
