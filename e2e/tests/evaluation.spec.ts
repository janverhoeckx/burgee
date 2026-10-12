import { expect, test } from '../fixtures';
import { uniqueKey } from '../support/unique-key';

// Without Conditions the Evaluation result is the flag's Enabled switch (ADR 0002); targeting.spec.ts covers Conditions.
test.describe('evaluation', () => {
  test('a flag created as enabled in the dashboard evaluates as enabled', async ({
    page,
    flagsListPage,
    flagFormPage,
    evaluationApi,
  }) => {
    const key = uniqueKey('evaluate-created');

    await flagsListPage.goto();
    await flagsListPage.newFlag.click();
    await flagFormPage.createFlag({ key, name: 'Evaluated flag', enabled: true });
    await expect(page).toHaveURL(/\/flags$/);

    const response = await evaluationApi.evaluate(key);
    expect(response.status()).toBe(200);
    expect(await response.json()).toEqual({ key, enabled: true });

    const all = await evaluationApi.evaluateAll();
    expect(all.status()).toBe(200);
    expect(await all.json()).toContainEqual({ key, enabled: true });
  });

  test('after a toggle in the dashboard, Evaluation returns the new state', async ({
    adminApi,
    flagsListPage,
    evaluationApi,
  }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('evaluate-toggled'), name: 'Toggled flag', enabled: true });
    await flagsListPage.goto();

    await test.step('toggle off', async () => {
      await flagsListPage.toggle(flag.key);

      const response = await evaluationApi.evaluate(flag.key);
      expect(response.status()).toBe(200);
      expect(await response.json()).toEqual({ key: flag.key, enabled: false });
    });

    await test.step('toggle back on', async () => {
      await flagsListPage.toggle(flag.key);

      const response = await evaluationApi.evaluate(flag.key);
      expect(response.status()).toBe(200);
      expect(await response.json()).toEqual({ key: flag.key, enabled: true });
    });
  });

  test('an unknown key returns 404', async ({ evaluationApi }) => {
    const response = await evaluationApi.evaluate(uniqueKey('never-created'));

    expect(response.status()).toBe(404);
  });

  test('a flag deleted in the dashboard returns 404', async ({ adminApi, flagsListPage, evaluationApi }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('evaluate-deleted'), name: 'Deleted flag', enabled: true });
    expect((await evaluationApi.evaluate(flag.key)).status()).toBe(200);

    await flagsListPage.goto();
    await flagsListPage.delete(flag.key);
    // The list drops the row only once the backend confirmed the delete.
    await expect(flagsListPage.row(flag.key)).toHaveCount(0);

    const response = await evaluationApi.evaluate(flag.key);
    expect(response.status()).toBe(404);
    const all = await evaluationApi.evaluateAll();
    expect(await all.json()).not.toContainEqual(expect.objectContaining({ key: flag.key }));
  });

  test('Evaluation needs no credentials', async ({ adminApi, anonymousRequest, evaluationApi }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('evaluate-anonymous'), name: 'Public flag', enabled: true });

    // The same request context is refused by the admin API, so it really sends no credentials.
    expect((await anonymousRequest.get('/api/admin/flags')).status()).toBe(401);

    const response = await evaluationApi.evaluate(flag.key);
    expect(response.status()).toBe(200);
    expect(await response.json()).toEqual({ key: flag.key, enabled: true });
  });
});
