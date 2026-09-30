import { uniqueKey } from './unique-key';

/**
 * A username no other test uses, e.g. `uniqueUsername('manage')` → `manage-mf3k2x9a-4hq1`.
 * Users share the run's database like Feature Flags do, and the backend rejects a duplicate subject.
 * Lowercase letters, digits and dashes only, well within the backend's 256 characters, so it is also
 * safe to type into the login form and to find in the users list by text.
 */
export function uniqueUsername(prefix: string): string {
  return uniqueKey(prefix);
}
