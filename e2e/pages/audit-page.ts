import type { Locator, Page } from '@playwright/test';

/** Mirrors the backend's `AuditAction`. */
export type AuditAction = 'CREATE' | 'UPDATE' | 'TOGGLE' | 'DELETE';

export class AuditPage {
  readonly heading: Locator;
  /** The "History for one flag · N events" line, shown only when the trail is filtered to one flag. */
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

  /** Narrows the list to entries whose flag, actor, action or details contain `term`. */
  async search(term: string): Promise<void> {
    await this.searchBox.fill(term);
  }

  /** Every Audit entry of the flag with this Key. */
  entries(key: string): Locator {
    return this.page.getByRole('row').filter({ hasText: key });
  }

  /** The flag's Audit entries for one action, matched on the Action cell rather than anywhere in the row. */
  entry(key: string, action: AuditAction): Locator {
    return this.entries(key).filter({ has: this.page.getByRole('cell', { name: action, exact: true }) });
  }
}
