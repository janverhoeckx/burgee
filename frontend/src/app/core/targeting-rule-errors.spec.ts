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

  it('prefixes conditions[i].values[j] with j+1 when no line mapping is given', () => {
    const result = parseConditionFieldErrors({ 'conditions[1].values[0]': 'must not be blank' });
    expect(result.conditions[1].values).toEqual(['Line 1: must not be blank']);
  });

  it('prefixes conditions[i].values[j] with the original textarea line of the sent value', () => {
    // Textarea "acme", "", "<300 chars>": the blank line is dropped, so values[1] came from line 3.
    const result = parseConditionFieldErrors(
      { 'conditions[0].values[1]': 'must be at most 256 characters' },
      [[1, 3]],
    );
    expect(result.conditions[0].values).toEqual(['Line 3: must be at most 256 characters']);
  });

  it('falls back to j+1 when the line mapping has no entry for the row or value', () => {
    const result = parseConditionFieldErrors(
      { 'conditions[0].values[2]': 'bad', 'conditions[1].values[0]': 'bad' },
      [[1, 3]],
    );
    expect(result.conditions[0].values).toEqual(['Line 3: bad']);
    expect(result.conditions[1].values).toEqual(['Line 1: bad']);
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
      values: ['Line 2: must not be blank'],
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
