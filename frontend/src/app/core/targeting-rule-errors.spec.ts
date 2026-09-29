import { parseConditionFieldErrors } from './targeting-rule-errors';

describe('parseConditionFieldErrors', () => {
  it('returns no errors for an empty map', () => {
    expect(parseConditionFieldErrors({})).toEqual({ conditions: {}, general: [] });
  });

  it('groups conditions[i].attribute under the row attribute', () => {
    const result = parseConditionFieldErrors({ 'conditions[0].attribute': 'must not be blank' });
    expect(result.conditions).toEqual({
      0: { attribute: ['must not be blank'], values: [], other: [] },
    });
    expect(result.general).toEqual([]);
  });

  it('groups conditions[i].values under the row values', () => {
    const result = parseConditionFieldErrors({ 'conditions[2].values': 'must not be empty' });
    expect(result.conditions).toEqual({
      2: { attribute: [], values: ['must not be empty'], other: [] },
    });
  });

  it('prefixes conditions[i].values[j] with the 1-based value number', () => {
    const result = parseConditionFieldErrors({ 'conditions[1].values[0]': 'must not be blank' });
    expect(result.conditions[1].values).toEqual(['Value 1: must not be blank']);
  });

  it('puts conditions[i].operator under the row other errors', () => {
    const result = parseConditionFieldErrors({ 'conditions[0].operator': 'must not be null' });
    expect(result.conditions[0]).toEqual({ attribute: [], values: [], other: ['must not be null'] });
  });

  it('collects several errors for the same row', () => {
    const result = parseConditionFieldErrors({
      'conditions[0].attribute': 'must not be blank',
      'conditions[0].values[1]': 'must not be blank',
    });
    expect(result.conditions[0]).toEqual({
      attribute: ['must not be blank'],
      values: ['Value 2: must not be blank'],
      other: [],
    });
  });

  it('reports non-condition keys as general errors, prefixed with the field', () => {
    const result = parseConditionFieldErrors({ name: 'must not be blank' });
    expect(result).toEqual({ conditions: {}, general: ['name: must not be blank'] });
  });

  it('treats unknown condition fields as general errors', () => {
    const result = parseConditionFieldErrors({ 'conditions[0].unknown': 'bad' });
    expect(result).toEqual({ conditions: {}, general: ['conditions[0].unknown: bad'] });
  });

  it('falls back to "invalid" for a null message', () => {
    const result = parseConditionFieldErrors({
      'conditions[0].attribute': null,
      key: null,
    });
    expect(result.conditions[0].attribute).toEqual(['invalid']);
    expect(result.general).toEqual(['key: invalid']);
  });
});
