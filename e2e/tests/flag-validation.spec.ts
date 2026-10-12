import { expect, test } from '../fixtures';
import { uniqueKey } from '../support/unique-key';

test.describe('flag form validation', () => {
  const invalidKeys = [
    { reason: 'uppercase letters', key: () => uniqueKey('invalid').toUpperCase() },
    { reason: 'a leading dash', key: () => `-${uniqueKey('invalid')}` },
  ];

  for (const { reason, key: makeKey } of invalidKeys) {
    test(`a key with ${reason} shows an error and creates no flag`, async ({ page, adminApi, flagFormPage }) => {
      const key = makeKey();

      await flagFormPage.gotoNew();
      await flagFormPage.fillNew({ key, name: 'Invalid key flag' });

      // The form checks the key pattern itself, so the request never reaches the backend.
      await expect(flagFormPage.error('Lowercase letters, digits, and . _ - only.')).toBeVisible();
      await expect(flagFormPage.save).toBeDisabled();
      await expect(page).toHaveURL(/\/flags\/new$/);

      const flags = await adminApi.flags.list();
      expect(flags.filter((flag) => flag.key.toLowerCase() === key.toLowerCase())).toEqual([]);
    });
  }

  test('a duplicate key shows the backend error and leaves the existing flag unchanged', async ({
    page,
    adminApi,
    flagFormPage,
  }) => {
    const existing = await adminApi.flags.create({
      key: uniqueKey('duplicate'),
      name: 'Existing flag',
      description: 'Created by the e2e suite',
      enabled: true,
    });
    // Read it back rather than keep the create response: that one carries nanosecond timestamps
    // the database truncates to microseconds.
    const before = await adminApi.flags.get(existing.id);

    await flagFormPage.gotoNew();
    await flagFormPage.createFlag({
      key: existing.key,
      name: 'Duplicate flag',
      description: 'Should never be saved',
      enabled: false,
    });

    await expect(flagFormPage.error('A flag with this key already exists.')).toBeVisible();
    await expect(page).toHaveURL(/\/flags\/new$/);

    expect(await adminApi.flags.get(existing.id)).toEqual(before);
    const flags = await adminApi.flags.list();
    expect(flags.filter((flag) => flag.key === existing.key)).toHaveLength(1);
  });
});
