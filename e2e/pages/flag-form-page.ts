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
  readonly newCondition: Locator;
  readonly conditionAttributes: Locator;
  readonly conditionValues: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { level: 1 });
    this.key = page.getByLabel('Key', { exact: true });
    this.name = page.getByLabel('Name', { exact: true });
    this.description = page.getByLabel('Description', { exact: true });
    this.enabled = page.getByLabel('Enabled', { exact: true });
    this.save = page.getByRole('button', { name: 'Save' });
    this.cancel = page.getByRole('button', { name: 'Cancel' });
    this.newCondition = page.getByRole('button', { name: 'Add condition' });
    this.conditionAttributes = page.getByLabel('Attribute', { exact: true });
    this.conditionValues = page.getByLabel('Values (one per line)', { exact: true });
  }

  async gotoNew(): Promise<Response | null> {
    return this.page.goto('/flags/new');
  }

  /** Waits until the flag has loaded (Key turns disabled), so the late load can't overwrite edits. */
  async gotoEdit(id: string): Promise<Response | null> {
    const response = await this.page.goto(`/flags/${id}/edit`);
    await this.page.getByRole('textbox', { name: 'Key', exact: true, disabled: true }).waitFor();
    return response;
  }

  async createFlag(flag: NewFlag): Promise<void> {
    await this.fillNew(flag);
    await this.save.click();
  }

  /** Filling the name after the key marks the key as touched, which shows its validation error. */
  async fillNew(flag: NewFlag): Promise<void> {
    await this.key.fill(flag.key);
    await this.fill(flag);
  }

  error(message: string): Locator {
    return this.page.getByText(message);
  }

  async update(fields: FlagFields): Promise<void> {
    await this.fill(fields);
    await this.save.click();
  }

  async addCondition(attribute: string, values: string[]): Promise<void> {
    // The new row renders asynchronously, so address it by index (`last()` could still be the previous row).
    const index = await this.conditionAttributes.count();
    await this.newCondition.click();
    await this.conditionAttributes.nth(index).fill(attribute);
    await this.conditionValues.nth(index).fill(values.join('\n'));
  }

  async removeCondition(attribute: string): Promise<void> {
    const attributes = await this.conditionAttributes.all();
    for (const [index, input] of attributes.entries()) {
      if ((await input.inputValue()) === attribute) {
        await this.removeConditionButton(index).click();
        return;
      }
    }
    throw new Error(`No Condition on Attribute "${attribute}" in the form`);
  }

  /** Last first, so the remaining remove buttons keep their names. */
  async clearConditions(): Promise<void> {
    for (let index = (await this.conditionAttributes.count()) - 1; index >= 0; index--) {
      await this.removeConditionButton(index).click();
    }
  }

  private removeConditionButton(index: number): Locator {
    return this.page.getByRole('button', { name: `Remove condition ${index + 1}`, exact: true });
  }

  private async fill({ name, description, enabled }: FlagFields): Promise<void> {
    if (name !== undefined) await this.name.fill(name);
    if (description !== undefined) await this.description.fill(description);
    if (enabled !== undefined) await this.enabled.setChecked(enabled);
  }
}
