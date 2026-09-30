import type { Locator, Page, Response } from '@playwright/test';

export interface FlagFields {
  name?: string;
  description?: string;
  enabled?: boolean;
}

export interface NewFlag extends FlagFields {
  key: string;
  name: string;
}

export class FlagFormPage {
  readonly heading: Locator;
  readonly key: Locator;
  readonly name: Locator;
  readonly description: Locator;
  readonly enabled: Locator;
  readonly save: Locator;
  readonly cancel: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { level: 1 });
    this.key = page.getByLabel('Key', { exact: true });
    this.name = page.getByLabel('Name', { exact: true });
    this.description = page.getByLabel('Description', { exact: true });
    this.enabled = page.getByLabel('Enabled', { exact: true });
    this.save = page.getByRole('button', { name: 'Save' });
    this.cancel = page.getByRole('button', { name: 'Cancel' });
  }

  /** Returns the document response, so a deep-link journey can check what the server sent. */
  async gotoNew(): Promise<Response | null> {
    return this.page.goto('/flags/new');
  }

  /** Returns the document response, so a deep-link journey can check what the server sent. */
  async gotoEdit(id: string): Promise<Response | null> {
    return this.page.goto(`/flags/${id}/edit`);
  }

  /** Fills the new-flag form and saves it. */
  async createFlag(flag: NewFlag): Promise<void> {
    await this.key.fill(flag.key);
    await this.fill(flag);
    await this.save.click();
  }

  /** Changes the given fields of a loaded edit form and saves it. */
  async update(fields: FlagFields): Promise<void> {
    await this.fill(fields);
    await this.save.click();
  }

  private async fill({ name, description, enabled }: FlagFields): Promise<void> {
    if (name !== undefined) await this.name.fill(name);
    if (description !== undefined) await this.description.fill(description);
    if (enabled !== undefined) await this.enabled.setChecked(enabled);
  }
}
