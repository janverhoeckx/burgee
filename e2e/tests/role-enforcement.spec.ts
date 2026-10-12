import { expect, test } from '../fixtures';
import { uniqueKey } from '../support/unique-key';

// Signs in as a freshly provisioned New user instead of the Bootstrap admin.
test.use({ signedIn: false });

test.describe('role enforcement', () => {
  test('a New user can sign in but is kept out of user management', async ({
    page,
    adminApi,
    loginPage,
    flagsListPage,
    usersListPage,
  }) => {
    const username = uniqueKey('new');
    const password = `pw-${username}`;
    await adminApi.users.create({ subject: username, role: 'NEW', password });

    await test.step('sign in lands on the flags page without permissions', async () => {
      await loginPage.goto();
      await loginPage.signIn(username, password);

      await expect(page).toHaveURL(/\/flags$/);
      await expect(page.getByText("Your account doesn't have permission yet.")).toBeVisible();
      await expect(page.getByRole('button', { name: 'Users' })).toHaveCount(0);
    });

    await test.step('opening the users page directly sends them back to the flags page', async () => {
      await usersListPage.goto();

      await expect(page).toHaveURL(/\/flags$/);
      await expect(flagsListPage.heading).toBeVisible();
      await expect(usersListPage.heading).toHaveCount(0);
    });
  });
});
