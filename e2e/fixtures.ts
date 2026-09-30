import { test as base } from '@playwright/test';
import { AdminApi } from './api/admin-api';
import { baseURL, e2eEnv } from './env';
import { FlagFormPage } from './pages/flag-form-page';
import { FlagsListPage } from './pages/flags-list-page';
import { LoginPage } from './pages/login-page';
import { restoreSession } from './support/session';

type Options = {
  /** Start signed in as the Bootstrap admin (the default); `test.use({ signedIn: false })` starts signed out. */
  signedIn: boolean;
};

type Fixtures = {
  loginPage: LoginPage;
  flagsListPage: FlagsListPage;
  flagFormPage: FlagFormPage;
  adminApi: AdminApi;
};

export const test = base.extend<Options & Fixtures>({
  signedIn: [true, { option: true }],

  context: async ({ context, signedIn }, use) => {
    if (signedIn) await restoreSession(context);
    await use(context);
  },

  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  flagsListPage: async ({ page }, use) => {
    await use(new FlagsListPage(page));
  },

  flagFormPage: async ({ page }, use) => {
    await use(new FlagFormPage(page));
  },

  adminApi: async ({ playwright }, use) => {
    const { username, password } = e2eEnv.admin;
    const request = await playwright.request.newContext({
      baseURL,
      httpCredentials: { username, password, send: 'always' },
    });
    await use(new AdminApi(request));
    await request.dispose();
  },
});

export { expect } from '@playwright/test';
