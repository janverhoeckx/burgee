import { expect, test } from '../fixtures';
import { uniqueKey } from '../support/unique-key';

// Conditions are set through the dashboard; Evaluation goes through the public API without credentials,
// as a client application calls it. The semantics under test are ADR 0002: `enabled AND every Condition matches`.
test.describe('targeting rule', () => {
  test('a Condition set in the dashboard decides the Evaluation per Attribute value', async ({
    page,
    adminApi,
    flagFormPage,
    evaluationApi: evaluation,
  }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('targeting'), name: 'Targeted flag', enabled: true });

    await flagFormPage.gotoEdit(flag.id);
    await flagFormPage.addCondition('organisationId', ['acme', 'globex']);
    await flagFormPage.save.click();
    await expect(page).toHaveURL(/\/flags$/);

    await test.step('an Attribute value in the list evaluates to true', async () => {
      expect(await evaluation.isEnabled(flag.key, { organisationId: 'acme' })).toBe(true);
      expect(await evaluation.isEnabled(flag.key, { organisationId: 'globex' })).toBe(true);
    });

    await test.step('a value not in the list evaluates to false, and matching is case-sensitive', async () => {
      expect(await evaluation.isEnabled(flag.key, { organisationId: 'initech' })).toBe(false);
      expect(await evaluation.isEnabled(flag.key, { organisationId: 'Acme' })).toBe(false);
    });

    await test.step('a context that lacks the Attribute evaluates to false', async () => {
      expect(await evaluation.isEnabled(flag.key, { country: 'nl' })).toBe(false);
      expect(await evaluation.isEnabled(flag.key, {})).toBe(false);
    });
  });

  test('with two Conditions, true requires both to match', async ({
    page,
    adminApi,
    flagFormPage,
    evaluationApi: evaluation,
  }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('targeting-and'), name: 'Two Conditions', enabled: true });

    await flagFormPage.gotoEdit(flag.id);
    await flagFormPage.addCondition('organisationId', ['acme', 'globex']);
    await flagFormPage.addCondition('country', ['nl', 'be']);
    await flagFormPage.save.click();
    await expect(page).toHaveURL(/\/flags$/);

    expect(await evaluation.isEnabled(flag.key, { organisationId: 'acme', country: 'nl' })).toBe(true);
    expect(await evaluation.isEnabled(flag.key, { organisationId: 'globex', country: 'be' })).toBe(true);
    expect(await evaluation.isEnabled(flag.key, { organisationId: 'acme', country: 'de' })).toBe(false);
    expect(await evaluation.isEnabled(flag.key, { organisationId: 'initech', country: 'nl' })).toBe(false);
    expect(await evaluation.isEnabled(flag.key, { organisationId: 'acme' })).toBe(false);
    expect(await evaluation.isEnabled(flag.key, { country: 'nl' })).toBe(false);
  });

  test('a disabled flag evaluates to false even when every Condition matches', async ({
    page,
    adminApi,
    flagFormPage,
    evaluationApi: evaluation,
  }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('targeting-off'), name: 'Kill switch', enabled: true });
    const matching = { organisationId: 'acme' };

    await flagFormPage.gotoEdit(flag.id);
    await flagFormPage.addCondition('organisationId', ['acme']);
    await flagFormPage.save.click();
    await expect(page).toHaveURL(/\/flags$/);
    expect(await evaluation.isEnabled(flag.key, matching)).toBe(true);

    await flagFormPage.gotoEdit(flag.id);
    await flagFormPage.update({ enabled: false });
    await expect(page).toHaveURL(/\/flags$/);

    expect(await evaluation.isEnabled(flag.key, matching)).toBe(false);
  });

  test('removing Conditions in the dashboard widens the Targeting Rule, down to everyone', async ({
    page,
    adminApi,
    flagFormPage,
    evaluationApi: evaluation,
  }) => {
    const flag = await adminApi.flags.create({
      key: uniqueKey('targeting-clear'),
      name: 'Widened flag',
      enabled: true,
      conditions: [
        { attribute: 'organisationId', operator: 'IN', values: ['acme'] },
        { attribute: 'country', operator: 'IN', values: ['nl'] },
      ],
    });
    expect(await evaluation.isEnabled(flag.key, { organisationId: 'acme' })).toBe(false);

    await test.step('removing one Condition drops only its requirement', async () => {
      await flagFormPage.gotoEdit(flag.id);
      await flagFormPage.removeCondition('country');
      await flagFormPage.save.click();
      await expect(page).toHaveURL(/\/flags$/);

      expect(await evaluation.isEnabled(flag.key, { organisationId: 'acme' })).toBe(true);
      expect(await evaluation.isEnabled(flag.key, { organisationId: 'initech', country: 'nl' })).toBe(false);
    });

    await test.step('removing every Condition makes the enabled flag true for every context', async () => {
      await flagFormPage.gotoEdit(flag.id);
      await flagFormPage.clearConditions();
      await flagFormPage.save.click();
      await expect(page).toHaveURL(/\/flags$/);

      expect(await evaluation.isEnabled(flag.key, { organisationId: 'initech' })).toBe(true);
      expect(await evaluation.isEnabled(flag.key, {})).toBe(true);
      expect(await evaluation.isEnabled(flag.key)).toBe(true);
    });
  });

  test('the Targeting Rule survives a reload of the edit page', async ({ page, adminApi, flagFormPage }) => {
    const flag = await adminApi.flags.create({ key: uniqueKey('targeting-reload'), name: 'Persisted rule' });

    await flagFormPage.gotoEdit(flag.id);
    await flagFormPage.addCondition('organisationId', ['acme', 'globex']);
    await flagFormPage.addCondition('country', ['nl']);
    await flagFormPage.save.click();
    await expect(page).toHaveURL(/\/flags$/);

    await flagFormPage.gotoEdit(flag.id);
    await page.reload();

    await expect(flagFormPage.conditionAttributes).toHaveCount(2);
    await expect(flagFormPage.conditionAttributes.nth(0)).toHaveValue('organisationId');
    await expect(flagFormPage.conditionValues.nth(0)).toHaveValue('acme\nglobex');
    await expect(flagFormPage.conditionAttributes.nth(1)).toHaveValue('country');
    await expect(flagFormPage.conditionValues.nth(1)).toHaveValue('nl');
  });
});
