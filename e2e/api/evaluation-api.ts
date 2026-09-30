import type { APIRequestContext, APIResponse } from '@playwright/test';

/** Mirrors `EvaluateRequest` in the backend's `EvaluationDtos.kt`: string Attributes only. */
export interface EvaluationContext {
  attributes?: Record<string, string>;
}

/** Mirrors `EvaluationResponse`: `enabled` is the Evaluation result, not the flag's master switch. */
export interface Evaluation {
  key: string;
  enabled: boolean;
}

/**
 * The public Evaluation API (`/api/v1/flags`), called the way an application calls it.
 * Build it on a request context without credentials. Calls return the raw response,
 * because a 404 for an unknown Key is part of the contract under test.
 */
export class EvaluationApi {
  private readonly base = '/api/v1/flags';

  constructor(private readonly request: APIRequestContext) {}

  /** Evaluates one flag. Without a context the request has no body, which counts as an empty Evaluation Context. */
  async evaluate(key: string, context?: EvaluationContext): Promise<APIResponse> {
    return this.request.post(`${this.base}/${encodeURIComponent(key)}/evaluate`, { data: context });
  }

  /** Evaluates every flag. */
  async evaluateAll(context?: EvaluationContext): Promise<APIResponse> {
    return this.request.post(`${this.base}/evaluate`, { data: context });
  }
}
