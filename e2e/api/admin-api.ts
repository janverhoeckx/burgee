import type { APIRequestContext } from '@playwright/test';
import { FlagsApi } from './flags-api';
import { UsersApi } from './users-api';

/**
 * The admin API, called as the Bootstrap admin, for setting up preconditions without the UI.
 * One property per resource: add e.g. `users` next to `flags` when a journey needs it.
 */
export class AdminApi {
  readonly flags: FlagsApi;
  readonly users: UsersApi;

  constructor(request: APIRequestContext) {
    this.flags = new FlagsApi(request);
    this.users = new UsersApi(request);
  }
}
