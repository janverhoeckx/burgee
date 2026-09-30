import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import type { BrowserContext, Page } from '@playwright/test';
import { baseURL } from '../env';

/**
 * The SPA keeps basic-auth credentials in sessionStorage under this key (`STORAGE_KEY` in
 * `frontend/src/app/core/auth.service.ts`). Playwright's `storageState` skips sessionStorage,
 * so the auth setup saves this one entry itself and `restoreSession` puts it back.
 */
const STORAGE_KEY = 'burgee.auth';
const sessionFile = join(dirname(fileURLToPath(import.meta.url)), '..', '.auth', 'admin.json');

interface SavedSession {
  key: string;
  value: string;
}

/** Saves the signed-in page's credentials entry for later tests. */
export async function saveSession(page: Page): Promise<void> {
  const value = await page.evaluate((key) => sessionStorage.getItem(key), STORAGE_KEY);
  if (value === null) throw new Error(`No "${STORAGE_KEY}" entry in sessionStorage: is the page signed in?`);
  mkdirSync(dirname(sessionFile), { recursive: true });
  writeFileSync(sessionFile, JSON.stringify({ key: STORAGE_KEY, value } satisfies SavedSession));
}

/** Writes the saved credentials into sessionStorage before every page load on the app's origin. */
export async function restoreSession(context: BrowserContext): Promise<void> {
  const session = JSON.parse(readFileSync(sessionFile, 'utf8')) as SavedSession;
  await context.addInitScript(
    ({ origin, key, value }) => {
      if (window.location.origin === origin) window.sessionStorage.setItem(key, value);
    },
    { origin: new URL(baseURL).origin, ...session },
  );
}
