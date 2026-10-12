import { expect, test } from '../fixtures';
import { e2eEnv } from '../env';
import type { AuditAction } from '../pages/audit-page';
import { uniqueKey } from '../support/unique-key';

const { admin } = e2eEnv;

// The audit trail is shared by every test running in parallel, so each assertion is scoped to this test's own Key.
test.describe('audit', () => {
  test('every change made through the dashboard records an Audit entry by the Bootstrap admin', async ({
    page,
    flagsListPage,
    flagFormPage,
    auditPage,
  }) => {
    const key = uniqueKey('audited');

    await test.step('create, edit, toggle and delete the flag through the dashboard', async () => {
      await flagsListPage.goto();
      await flagsListPage.newFlag.click();
      await flagFormPage.createFlag({ key, name: 'Audited flag', enabled: true });
      await expect(page).toHaveURL(/\/flags$/);

      await flagsListPage.edit(key);
      await expect(flagFormPage.key).toHaveValue(key);
      await flagFormPage.update({ name: 'Renamed audited flag' });
      await expect(page).toHaveURL(/\/flags$/);

      await flagsListPage.toggle(key);
      await expect(flagsListPage.toggleSwitch(key)).not.toBeChecked();

      await flagsListPage.delete(key);
      await expect(flagsListPage.row(key)).toHaveCount(0);
    });

    await test.step('the audit trail shows one entry per change, each by the Bootstrap admin', async () => {
      await auditPage.goto();
      await auditPage.search(key);

      const actions: AuditAction[] = ['CREATE', 'UPDATE', 'TOGGLE', 'DELETE'];
      for (const action of actions) {
        const entry = auditPage.entry(key, action);
        await expect(entry, `${action} entry`).toHaveCount(1);
        await expect(entry.getByRole('cell', { name: admin.username, exact: true })).toBeVisible();
      }
      await expect(auditPage.entries(key)).toHaveCount(actions.length);
    });
  });

  test("a flag's History shows only that flag's Audit entries", async ({ adminApi, flagsListPage, auditPage }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('history'), name: 'Flag with history' });
    await adminApi.flags.toggle(flag.id);
    const other = await adminApi.flags.create({ key: uniqueKey('other'), name: 'Other flag' });

    // The other flag's entry is in the full trail, so its absence from the History below means something.
    await auditPage.goto();
    await expect(auditPage.entry(other.key, 'CREATE')).toHaveCount(1);

    await flagsListPage.goto();
    await flagsListPage.history(flag.key);

    await expect(auditPage.historySummary).toHaveText('History for one flag · 2 events');
    await expect(auditPage.entry(flag.key, 'CREATE')).toHaveCount(1);
    await expect(auditPage.entry(flag.key, 'TOGGLE')).toHaveCount(1);
    await expect(auditPage.entries(other.key)).toHaveCount(0);
  });
});
