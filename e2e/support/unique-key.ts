/**
 * A Feature Flag Key no other test uses, e.g. `uniqueKey('lifecycle')` → `lifecycle-mf3k2x9a-4hq1`.
 * The stack's database lives for the whole run and tests run in parallel, so every test makes its own.
 * Matches the backend's `^[a-z0-9][a-z0-9._-]*$`, so `prefix` must start with a lowercase letter or digit.
 */
export function uniqueKey(prefix: string): string {
  const time = Date.now().toString(36);
  const random = Math.random().toString(36).slice(2, 6).padEnd(4, '0');
  return `${prefix}-${time}-${random}`;
}
