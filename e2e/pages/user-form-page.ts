import type { Locator, Page } from '@playwright/test';
import type { Role } from '../api/users-api';

export interface UserFields {
  displayName?: string;
  role?: Role;
  password?: string;
}

export interface NewUser extends UserFields {
  username: string;
  password: string;
}

/** The user form in basic auth mode, where the subject is labelled Username and a password applies. */
export class UserFormPage {
  readonly heading: Locator;
  readonly username: Locator;
  readonly displayName: Locator;
  readonly email: Locator;
  readonly role: Locator;
  readonly password: Locator;
  readonly save: Locator;
  readonly cancel: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { level: 1 });
    this.username = page.getByLabel('Username', { exact: true });
    this.displayName = page.getByLabel('Display name', { exact: true });
    this.email = page.getByLabel('Email', { exact: true });
    this.role = page.getByLabel('Role', { exact: true });
    this.password = page.getByLabel('Password', { exact: true });
    this.save = page.getByRole('button', { name: 'Save' });
    this.cancel = page.getByRole('button', { name: 'Cancel' });
  }

  async gotoNew(): Promise<void> {
    await this.page.goto('/users/new');
  }

  /** Fills the new-user form and saves it. */
  async createUser(user: NewUser): Promise<void> {
    await this.username.fill(user.username);
    await this.fill(user);
    await this.save.click();
  }

  /** Changes the given fields of a loaded edit form and saves it. */
  async update(fields: UserFields): Promise<void> {
    await this.fill(fields);
    await this.save.click();
  }

  private async fill({ displayName, role, password }: UserFields): Promise<void> {
    if (displayName !== undefined) await this.displayName.fill(displayName);
    if (role !== undefined) await this.role.selectOption(role);
    if (password !== undefined) await this.password.fill(password);
  }
}
