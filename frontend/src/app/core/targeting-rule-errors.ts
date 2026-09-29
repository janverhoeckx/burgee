/** Backend validation messages for one Condition row, grouped by the field they belong to. */
export interface ConditionRowErrors {
  attribute: string[];
  values: string[];
  /** Errors on the Condition that belong to no editable field, such as its operator. */
  other: string[];
}

/** A 400 response's `fieldErrors`, split into per-Condition-row errors and everything else. */
export interface ParsedFieldErrors {
  /** Errors per Condition row, keyed by the row's index in the submitted `conditions` list. */
  conditions: Record<number, ConditionRowErrors>;
  /** Errors that belong to no Condition row, formatted as `field: message`. */
  general: string[];
}

/**
 * Mirrors the backend's violation-key format (the Bean Validation property path of the request
 * body): `conditions[i].attribute`, `conditions[i].operator`, `conditions[i].values` and
 * `conditions[i].values[j]`, with 0-based indexes. Keep in sync if the backend's request DTO
 * or its field names change.
 */
const CONDITION_FIELD = /^conditions\[(\d+)\]\.(attribute|values|operator)(?:\[(\d+)\])?$/;

/**
 * Splits the backend's `fieldErrors` map into errors per Condition row and general errors.
 * A `values[j]` error is prefixed with the 1-based value number; a missing message reads "invalid".
 */
export function parseConditionFieldErrors(
  fieldErrors: Record<string, string | null>,
): ParsedFieldErrors {
  const conditions: Record<number, ConditionRowErrors> = {};
  const general: string[] = [];
  for (const [field, message] of Object.entries(fieldErrors)) {
    const text = message ?? 'invalid';
    const match = CONDITION_FIELD.exec(field);
    if (!match) {
      general.push(`${field}: ${text}`);
      continue;
    }
    const [, index, part, valueIndex] = match;
    const row = (conditions[+index] ??= { attribute: [], values: [], other: [] });
    if (part === 'attribute') row.attribute.push(text);
    else if (part === 'values')
      row.values.push(valueIndex === undefined ? text : `Value ${+valueIndex + 1}: ${text}`);
    else row.other.push(text);
  }
  return { conditions, general };
}
