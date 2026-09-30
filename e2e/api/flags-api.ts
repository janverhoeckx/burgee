import type { APIRequestContext, APIResponse } from '@playwright/test';

/** Mirrors `FeatureFlagResponse` / `ConditionDto` in the backend's `FlagDtos.kt`. */
export interface Condition {
  attribute: string;
  operator: 'IN';
  values: string[];
}

export interface FeatureFlag {
  id: string;
  key: string;
  name: string;
  description: string | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  conditions: Condition[];
}

/** Mirrors `CreateFeatureFlagRequest`; the backend defaults `enabled` to false and `conditions` to none. */
export interface CreateFlag {
  key: string;
  name: string;
  description?: string | null;
  enabled?: boolean;
  conditions?: Condition[];
}

/** Mirrors `UpdateFeatureFlagRequest`. A PUT replaces the Targeting Rule, so omitted `conditions` clears it. */
export interface UpdateFlag {
  name: string;
  description?: string | null;
  enabled: boolean;
  conditions?: Condition[];
}

/** `/api/admin/flags`. Every call throws on a non-2xx response, so a failed precondition fails loudly. */
export class FlagsApi {
  private readonly base = '/api/admin/flags';

  constructor(private readonly request: APIRequestContext) {}

  async create(flag: CreateFlag): Promise<FeatureFlag> {
    return json(await this.request.post(this.base, { data: flag }));
  }

  async get(id: string): Promise<FeatureFlag> {
    return json(await this.request.get(`${this.base}/${id}`));
  }

  async list(): Promise<FeatureFlag[]> {
    return json(await this.request.get(this.base));
  }

  async update(id: string, flag: UpdateFlag): Promise<FeatureFlag> {
    return json(await this.request.put(`${this.base}/${id}`, { data: flag }));
  }

  async toggle(id: string): Promise<FeatureFlag> {
    return json(await this.request.post(`${this.base}/${id}/toggle`));
  }

  async delete(id: string): Promise<void> {
    await ok(await this.request.delete(`${this.base}/${id}`));
  }
}

export async function ok(response: APIResponse): Promise<APIResponse> {
  if (!response.ok()) {
    throw new Error(`${response.status()} from ${response.url()}: ${await response.text()}`);
  }
  return response;
}

export async function json<T>(response: APIResponse): Promise<T> {
  return (await ok(response)).json() as Promise<T>;
}
