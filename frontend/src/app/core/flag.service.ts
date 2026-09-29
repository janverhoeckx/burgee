import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBaseUrl } from './api.config';

/** The only operator a Condition supports for now. */
export type ConditionOperator = 'IN';

/** One requirement in a flag's Targeting Rule: the Attribute must have one of the values. */
export interface Condition {
  attribute: string;
  operator: ConditionOperator;
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

export interface CreateFlagPayload {
  key: string;
  name: string;
  description?: string | null;
  enabled: boolean;
  conditions: Condition[];
}

/**
 * A PUT replaces the whole Targeting Rule, and a missing `conditions` clears it,
 * so `conditions` is required here: always send the flag's full list.
 */
export interface UpdateFlagPayload {
  name: string;
  description?: string | null;
  enabled: boolean;
  conditions: Condition[];
}

@Injectable({ providedIn: 'root' })
export class FlagService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBaseUrl()}/api/admin/flags`;

  list(): Observable<FeatureFlag[]> {
    return this.http.get<FeatureFlag[]>(this.base);
  }

  get(id: string): Observable<FeatureFlag> {
    return this.http.get<FeatureFlag>(`${this.base}/${id}`);
  }

  create(payload: CreateFlagPayload): Observable<FeatureFlag> {
    return this.http.post<FeatureFlag>(this.base, payload);
  }

  update(id: string, payload: UpdateFlagPayload): Observable<FeatureFlag> {
    return this.http.put<FeatureFlag>(`${this.base}/${id}`, payload);
  }

  toggle(id: string): Observable<FeatureFlag> {
    return this.http.post<FeatureFlag>(`${this.base}/${id}/toggle`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
