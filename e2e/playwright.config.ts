import { defineConfig, devices } from '@playwright/test';
import { baseURL } from './env';

const ci = !!process.env['CI'];

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: ci,
  retries: ci ? 2 : 0,
  reporter: [['html', { open: 'never' }], ['list']],
  globalSetup: './global-setup.ts',
  globalTeardown: './global-teardown.ts',
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  projects: [
    // Signs in once through the login page; see support/session.ts for how later tests reuse it.
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['setup'],
    },
  ],
});
