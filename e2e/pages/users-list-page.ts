import type { Locator, Page } from '@playwright/test';
import type { Role } from '../api/users-api';

export class UsersListPage {
  readonly heading: Locator;
  readonly newUser: Locator;
  /** Rendered once the list has loaded, so a missing row means the user is gone rather than not loaded yet. */
  readonly table: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { name: 'Users' });
    this.newUser = page.getByRole('button', { name: 'New user' });
    this.table = page.getByRole('table');
  }

  async goto(): Promise<void> {
    await this.page.goto('/users');
  }

  row(username: string): Locator {
    return this.page.getByRole('row').filter({ hasText: username });
  }

  /** The user's Role badge, e.g. `role(username, 'ADMIN')`; it has no count when the user holds another Role. */
  role(username: string, role: Role): Locator {
    return this.row(username).getByRole('cell', { name: role, exact: true });
  }

  async edit(username: string): Promise<void> {
    await this.row(username).getByRole('button', { name: 'Edit' }).click();
  }

  /** Accepts the confirmation and returns its message. */
  async delete(username: string): Promise<string> {
    const answered = this.page.waitForEvent('dialog').then(async (dialog) => {
      await dialog.accept();
      return dialog.message();
    });
    await this.row(username).getByRole('button', { name: 'Delete' }).click();
    return answered;
  }
}
