import type { APIRequestContext } from '@playwright/test';
import { FlagsApi } from './flags-api';

/**
 * The admin API, called as the Bootstrap admin, for setting up preconditions without the UI.
 * One property per resource: add e.g. `users` next to `flags` when a journey needs it.
 */
export class AdminApi {
  readonly flags: FlagsApi;

  constructor(request: APIRequestContext) {
    this.flags = new FlagsApi(request);
  }
}
