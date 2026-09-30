import type { APIRequestContext } from '@playwright/test';
import { FlagsApi } from './flags-api';
import { UsersApi } from './users-api';

export class AdminApi {
  readonly flags: FlagsApi;
  readonly users: UsersApi;

  constructor(request: APIRequestContext) {
    this.flags = new FlagsApi(request);
    this.users = new UsersApi(request);
  }
}
