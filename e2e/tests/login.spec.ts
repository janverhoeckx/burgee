import { expect, test } from '../fixtures';
import { e2eEnv } from '../env';

const { admin } = e2eEnv;

// Exercises the login page itself, so it starts without the saved session.
test.use({ signedIn: false });

test.describe('login', () => {
  test('rejects a wrong password and stays on the login page', async ({ page, loginPage }) => {
    await loginPage.goto();
    await loginPage.signIn(admin.username, 'not-the-password');

    await expect(loginPage.error('Invalid credentials')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('signs the Bootstrap admin in and lands on the flags list', async ({ page, loginPage }) => {
    await loginPage.goto();
    await loginPage.signIn(admin.username, admin.password);

    await expect(page).toHaveURL(/\/flags$/);
    await expect(page.getByRole('heading', { name: 'Feature flags' })).toBeVisible();
  });

  test('signing out returns to login and protected pages stay closed', async ({ page, loginPage }) => {
    await loginPage.goto();
    await loginPage.signIn(admin.username, admin.password);
    await expect(page).toHaveURL(/\/flags$/);

    await loginPage.signOut.click();
    await expect(loginPage.heading).toBeVisible();

    await page.goto('/flags');
    await expect(page).toHaveURL(/\/login$/);
  });
});
