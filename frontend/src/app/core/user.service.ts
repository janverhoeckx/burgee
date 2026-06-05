import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBaseUrl } from './api.config';

export type Role = 'ADMIN' | 'USER' | 'NEW';
export type IdentityProvider = 'BASIC' | 'JWT';

export interface User {
  id: string;
  subject: string;
  email: string | null;
  displayName: string | null;
  role: Role;
  provider: IdentityProvider;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserPayload {
  subject: string;
  email?: string | null;
  displayName?: string | null;
  role: Role;
  provider?: IdentityProvider | null;
  password?: string | null;
}

export interface UpdateUserPayload {
  email?: string | null;
  displayName?: string | null;
  role: Role;
  password?: string | null;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBaseUrl()}/api/admin/users`;

  list(): Observable<User[]> {
    return this.http.get<User[]>(this.base);
  }

  get(id: string): Observable<User> {
    return this.http.get<User>(`${this.base}/${id}`);
  }

  create(payload: CreateUserPayload): Observable<User> {
    return this.http.post<User>(this.base, payload);
  }

  update(id: string, payload: UpdateUserPayload): Observable<User> {
    return this.http.put<User>(`${this.base}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
