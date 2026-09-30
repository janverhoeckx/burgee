import { expect, test as setup } from '../fixtures';
import { e2eEnv } from '../env';
import { saveSession } from '../support/session';

setup.use({ signedIn: false });

setup('sign in as the Bootstrap admin', async ({ page, loginPage }) => {
  await loginPage.goto();
  await loginPage.signIn(e2eEnv.admin.username, e2eEnv.admin.password);
  await expect(page).toHaveURL(/\/flags$/);

  await saveSession(page);
});
