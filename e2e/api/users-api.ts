import type { APIRequestContext } from '@playwright/test';
import { json, ok } from './http';

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

/** The backend defaults `role` to `USER`. Without a `password` a basic-auth User can't sign in. */
export interface CreateUser {
  subject: string;
  email?: string | null;
  displayName?: string | null;
  role?: Role;
  provider?: IdentityProvider;
  password?: string | null;
}

/** A PUT clears an omitted `email` or `displayName`; an omitted `password` keeps the current one. */
export interface UpdateUser {
  email?: string | null;
  displayName?: string | null;
  role: Role;
  password?: string | null;
}

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
