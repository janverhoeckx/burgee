import type { APIRequestContext } from '@playwright/test';
import { json, ok } from './http';

/** Mirrors `Role` in the backend's `user/domain/Role.kt`. */
export type Role = 'ADMIN' | 'USER' | 'NEW';

/** Mirrors `IdentityProvider`; in the e2e stack's basic auth mode the backend defaults to `BASIC`. */
export type IdentityProvider = 'BASIC' | 'JWT';

/** Mirrors `UserResponse` in the backend's `UserDtos.kt`. The password hash is never returned. */
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

/**
 * Mirrors `CreateUserRequest`. `subject` is the username in basic auth; the backend defaults `role` to
 * `USER` and `provider` to the running auth method. Without a `password` a basic-auth User can't sign in.
 */
export interface CreateUser {
  subject: string;
  email?: string | null;
  displayName?: string | null;
  role?: Role;
  provider?: IdentityProvider;
  password?: string | null;
}

/**
 * Mirrors `UpdateUserRequest`. A PUT replaces `email` and `displayName`, so omitting them clears them;
 * an omitted or blank `password` keeps the current one. The subject can't change.
 */
export interface UpdateUser {
  email?: string | null;
  displayName?: string | null;
  role: Role;
  password?: string | null;
}

/** `/api/admin/users`. Every call throws on a non-2xx response, so a failed precondition fails loudly. */
export class UsersApi {
  private readonly base = '/api/admin/users';

  constructor(private readonly request: APIRequestContext) {}

  async create(user: CreateUser): Promise<User> {
    return json(await this.request.post(this.base, { data: user }));
  }

  async get(id: string): Promise<User> {
    return json(await this.request.get(`${this.base}/${id}`));
  }

  async list(): Promise<User[]> {
    return json(await this.request.get(this.base));
  }

  async update(id: string, user: UpdateUser): Promise<User> {
    return json(await this.request.put(`${this.base}/${id}`, { data: user }));
  }

  async delete(id: string): Promise<void> {
    await ok(await this.request.delete(`${this.base}/${id}`));
  }
}
