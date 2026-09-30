import { expect, test } from '../fixtures';
import { baseURL } from '../env';
import { FlagsListPage } from '../pages/flags-list-page';
import { LoginPage } from '../pages/login-page';
import { uniqueKey } from '../support/unique-key';

test.describe('user management', () => {
  test('an admin creates a user who can then sign in with that password', async ({
    browser,
    page,
    usersListPage,
    userFormPage,
  }) => {
    const username = uniqueKey('created');
    const password = `pw-${username}`;
    const row = usersListPage.row(username);

    await test.step('create', async () => {
      await usersListPage.goto();
      await usersListPage.newUser.click();
      await expect(userFormPage.heading).toHaveText('New user');
      await userFormPage.createUser({ username, displayName: 'Created by e2e', role: 'ADMIN', password });

      await expect(page).toHaveURL(/\/users$/);
      await expect(row).toContainText('Created by e2e');
      await expect(usersListPage.role(username, 'ADMIN')).toBeVisible();
    });

    await test.step('sign in as that user in a fresh browser context', async () => {
      const context = await browser.newContext({ baseURL });
      try {
        const freshPage = await context.newPage();
        const loginPage = new LoginPage(freshPage);

        await loginPage.goto();
        await loginPage.signIn(username, password);

        await expect(freshPage).toHaveURL(/\/flags$/);
        await expect(new FlagsListPage(freshPage).heading).toBeVisible();
        await expect(freshPage.getByText(username, { exact: true })).toBeVisible();
      } finally {
        await context.close();
      }
    });
  });

  test("changing a user's role survives a reload", async ({ page, adminApi, usersListPage, userFormPage }) => {
    const user = await adminApi.users.create({
      subject: uniqueKey('role'),
      displayName: 'Role change',
      role: 'NEW',
      password: 'secret',
    });

    await usersListPage.goto();
    await expect(usersListPage.role(user.subject, 'NEW')).toBeVisible();

    await usersListPage.edit(user.subject);
    await expect(userFormPage.heading).toHaveText('Edit user');
    await expect(userFormPage.username).toHaveValue(user.subject);
    await expect(userFormPage.role).toHaveValue('NEW');

    await userFormPage.update({ role: 'ADMIN' });

    await expect(page).toHaveURL(/\/users$/);
    await page.reload();
    await expect(usersListPage.role(user.subject, 'ADMIN')).toBeVisible();
    await expect(usersListPage.role(user.subject, 'NEW')).toHaveCount(0);
    await expect(usersListPage.row(user.subject)).toContainText('Role change');
  });

  test('deleting a user after accepting the confirmation removes them', async ({
    page,
    adminApi,
    usersListPage,
  }) => {
    const user = await adminApi.users.create({ subject: uniqueKey('delete'), role: 'NEW' });

    await usersListPage.goto();
    const message = await usersListPage.delete(user.subject);

    expect(message).toContain(user.subject);
    // The list drops the row only once the backend confirmed the delete.
    await expect(usersListPage.row(user.subject)).toHaveCount(0);
    await page.reload();
    await expect(usersListPage.table).toBeVisible();
    await expect(usersListPage.row(user.subject)).toHaveCount(0);
  });
});
