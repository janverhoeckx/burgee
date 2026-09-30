/**
 * The database lives for the whole run and tests run in parallel, so every test makes its own Key.
 * `prefix` must start with a lowercase letter or digit to match the backend's Key pattern.
 */
export function uniqueKey(prefix: string): string {
  const time = Date.now().toString(36);
  const random = Math.random().toString(36).slice(2, 6).padEnd(4, '0');
  return `${prefix}-${time}-${random}`;
}
