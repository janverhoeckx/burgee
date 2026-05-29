import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBaseUrl } from './api.config';

export type AuditAction = 'CREATE' | 'UPDATE' | 'TOGGLE' | 'DELETE';

export interface AuditEntry {
  id: string;
  flagId: string;
  flagKey: string;
  action: AuditAction;
  actor: string;
  detail: string | null;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBaseUrl()}/api/admin/audit`;

  list(flagId?: string): Observable<AuditEntry[]> {
    const params = flagId ? new HttpParams().set('flagId', flagId) : undefined;
    return this.http.get<AuditEntry[]>(this.base, { params });
  }
}
