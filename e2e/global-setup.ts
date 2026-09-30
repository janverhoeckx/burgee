import { execFileSync } from 'node:child_process';
import { composeArgs, e2eEnv } from './env';

// `up --wait` returns once the image's HEALTHCHECK reports healthy, which is also what the published image promises.
export default async function globalSetup(): Promise<void> {
  const up = ['up', '-d', '--wait', '--wait-timeout', '180', ...(e2eEnv.noBuild ? [] : ['--build'])];
  try {
    execFileSync('docker', composeArgs(...up), { stdio: 'inherit' });
  } catch (error) {
    execFileSync('docker', composeArgs('logs', '--tail', '200', 'burgee'), { stdio: 'inherit' });
    throw error;
  }
}
