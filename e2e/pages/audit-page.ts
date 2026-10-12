import type { Locator, Page } from '@playwright/test';

export type AuditAction = 'CREATE' | 'UPDATE' | 'TOGGLE' | 'DELETE';

export class AuditPage {
  readonly heading: Locator;
  readonly historySummary: Locator;
  readonly searchBox: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { name: 'Audit trail' });
    this.historySummary = page.getByText('History for one flag');
    this.searchBox = page.getByRole('searchbox');
  }

  async goto(): Promise<void> {
    await this.page.goto('/audit');
  }

  async search(term: string): Promise<void> {
    await this.searchBox.fill(term);
  }

  entries(key: string): Locator {
    return this.page.getByRole('row').filter({ hasText: key });
  }

  /** Matches the Action cell only: the details column also names actions, e.g. "Created flag". */
  entry(key: string, action: AuditAction): Locator {
    return this.entries(key).filter({ has: this.page.getByRole('cell', { name: action, exact: true }) });
  }
}
