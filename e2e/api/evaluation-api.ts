import type { APIRequestContext, APIResponse } from '@playwright/test';
import { json } from './http';

export interface EvaluationContext {
  attributes?: Record<string, string>;
}

/** `enabled` is the Evaluation result, not the flag's Enabled switch. */
export interface Evaluation {
  key: string;
  enabled: boolean;
}

/** Returns raw responses: the 404 for an unknown Key is part of the contract under test. */
export class EvaluationApi {
  private readonly base = '/api/v1/flags';

  constructor(private readonly request: APIRequestContext) {}

  async evaluate(key: string, context?: EvaluationContext): Promise<APIResponse> {
    return this.request.post(`${this.base}/${encodeURIComponent(key)}/evaluate`, { data: context });
  }

  async isEnabled(key: string, attributes?: Record<string, string>): Promise<boolean> {
    const response = await this.evaluate(key, attributes && { attributes });
    return (await json<Evaluation>(response)).enabled;
  }

  async evaluateAll(context?: EvaluationContext): Promise<APIResponse> {
    return this.request.post(`${this.base}/evaluate`, { data: context });
  }
}
