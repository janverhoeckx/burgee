import type { Response } from '@playwright/test';
import { expect, test } from '../fixtures';
import { uniqueKey } from '../support/unique-key';

// `page.goto` as the first navigation is a cold load, so the backend's SPA fallback must serve the dashboard.
test.describe('deep links', () => {
  test('opening the new-flag page cold shows an empty flag form', async ({ flagFormPage }) => {
    const response = await flagFormPage.gotoNew();

    expectDashboardDocument(response);
    await expect(flagFormPage.heading).toHaveText('New flag');
    await expect(flagFormPage.key).toBeEditable();
    await expect(flagFormPage.key).toHaveValue('');
    await expect(flagFormPage.save).toBeVisible();
  });

  test("opening a flag's edit page cold shows the form filled with that flag", async ({
    adminApi,
    flagFormPage,
  }) => {
    const flag = await adminApi.flags.create({
      key: uniqueKey('deeplink'),
      name: 'Deep-linked flag',
      description: 'Opened straight from its URL',
      enabled: true,
    });

    const response = await flagFormPage.gotoEdit(flag.id);

    expectDashboardDocument(response);
    await expect(flagFormPage.heading).toHaveText('Edit flag');
    await expect(flagFormPage.key).toHaveValue(flag.key);
    await expect(flagFormPage.name).toHaveValue('Deep-linked flag');
    await expect(flagFormPage.description).toHaveValue('Opened straight from its URL');
    await expect(flagFormPage.enabled).toBeChecked();
  });
});

function expectDashboardDocument(response: Response | null): void {
  expect(response?.status()).toBe(200);
  expect(response?.headers()['content-type']).toContain('text/html');
}
