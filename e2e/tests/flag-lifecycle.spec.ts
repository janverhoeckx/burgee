import { expect, test } from '../fixtures';
import { uniqueKey } from '../support/unique-key';

test.describe('flag lifecycle', () => {
  test('creates, edits, toggles and deletes a flag through the dashboard', async ({
    page,
    flagsListPage,
    flagFormPage,
  }) => {
    const key = uniqueKey('lifecycle');
    const row = flagsListPage.row(key);

    await test.step('create', async () => {
      await flagsListPage.goto();
      await flagsListPage.newFlag.click();
      await flagFormPage.createFlag({
        key,
        name: 'Lifecycle flag',
        description: 'Created by the e2e suite',
        enabled: true,
      });

      await expect(page).toHaveURL(/\/flags$/);
      await expect(row).toContainText('Lifecycle flag');
      await expect(row).toContainText('Created by the e2e suite');
      await expect(flagsListPage.toggleSwitch(key)).toBeChecked();
    });

    await test.step('edit', async () => {
      await flagsListPage.edit(key);
      await expect(flagFormPage.heading).toHaveText('Edit flag');
      await expect(flagFormPage.key).toHaveValue(key);

      await flagFormPage.update({ name: 'Renamed flag', description: 'Edited by the e2e suite' });

      await expect(page).toHaveURL(/\/flags$/);
      await expect(row).toContainText('Renamed flag');
      await expect(row).toContainText('Edited by the e2e suite');
      await expect(row).not.toContainText('Lifecycle flag');
    });

    await test.step('toggle, and the new state survives a reload', async () => {
      await flagsListPage.toggle(key);
      await expect(flagsListPage.toggleSwitch(key)).not.toBeChecked();

      await page.reload();
      await expect(flagsListPage.toggleSwitch(key)).not.toBeChecked();
    });

    await test.step('delete after accepting the confirmation', async () => {
      const message = await flagsListPage.delete(key);

      expect(message).toContain(key);
      // The list drops the row only once the backend confirmed the delete.
      await expect(row).toHaveCount(0);
    });
  });

  test('declining the delete confirmation keeps the flag', async ({ page, adminApi, flagsListPage }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('keep'), name: 'Kept flag' });

    await flagsListPage.goto();
    const message = await flagsListPage.deleteButDecline(flag.key);

    expect(message).toContain(flag.key);
    await expect(flagsListPage.row(flag.key)).toBeVisible();
    await page.reload();
    await expect(flagsListPage.row(flag.key)).toBeVisible();
  });
});
