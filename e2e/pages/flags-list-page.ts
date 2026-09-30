import type { Locator, Page } from '@playwright/test';

export class FlagsListPage {
  readonly heading: Locator;
  readonly newFlag: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { name: 'Feature flags' });
    this.newFlag = page.getByRole('button', { name: 'New flag' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/flags');
  }

  row(key: string): Locator {
    return this.page.getByRole('row').filter({ hasText: key });
  }

  toggleSwitch(key: string): Locator {
    return this.page.getByRole('checkbox', { name: `Toggle ${key}`, exact: true });
  }

  /** Resolves once the backend has answered, so a reload right after can't overtake the toggle. */
  async toggle(key: string): Promise<void> {
    const toggled = this.page.waitForResponse(
      (response) => response.request().method() === 'POST' && response.url().endsWith('/toggle'),
      { timeout: 10_000 },
    );
    await this.toggleSwitch(key).click();
    await toggled;
  }

  async history(key: string): Promise<void> {
    await this.row(key).getByRole('button', { name: 'History' }).click();
  }

  async edit(key: string): Promise<void> {
    await this.row(key).getByRole('button', { name: 'Edit' }).click();
  }

  async delete(key: string): Promise<string> {
    return this.deleteAnswering(key, true);
  }

  async deleteButDecline(key: string): Promise<string> {
    return this.deleteAnswering(key, false);
  }

  private async deleteAnswering(key: string, accept: boolean): Promise<string> {
    const answered = this.page.waitForEvent('dialog').then(async (dialog) => {
      await (accept ? dialog.accept() : dialog.dismiss());
      return dialog.message();
    });
    await this.row(key).getByRole('button', { name: 'Delete' }).click();
    return answered;
  }
}
