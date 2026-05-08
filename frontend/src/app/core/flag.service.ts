import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBaseUrl } from './api.config';

export interface FeatureFlag {
  id: string;
  key: string;
  name: string;
  description: string | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFlagPayload {
  key: string;
  name: string;
  description?: string | null;
  enabled: boolean;
}

export interface UpdateFlagPayload {
  name: string;
  description?: string | null;
  enabled: boolean;
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
