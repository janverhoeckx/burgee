import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));

export const e2eEnv = {
  port: process.env['E2E_PORT'] ?? '18080',
  project: process.env['E2E_PROJECT'] ?? 'burgee-e2e',
  composeFile: join(here, 'docker-compose.e2e.yml'),
  noBuild: process.env['E2E_NO_BUILD'] === '1',
  keepStack: process.env['E2E_KEEP_STACK'] === '1',
  admin: { username: 'admin', password: 'admin' },
};

export const baseURL = `http://localhost:${e2eEnv.port}`;

export function composeArgs(...args: string[]): string[] {
  return ['compose', '-p', e2eEnv.project, '-f', e2eEnv.composeFile, ...args];
}
