import { execFileSync } from 'node:child_process';
import { composeArgs, e2eEnv } from './env';

// Waits on the image's own HEALTHCHECK, so a broken health check fails the suite too.
export default async function globalSetup(): Promise<void> {
  const up = ['up', '-d', '--wait', '--wait-timeout', '180', ...(e2eEnv.noBuild ? [] : ['--build'])];
  try {
    execFileSync('docker', composeArgs(...up), { stdio: 'inherit' });
  } catch (error) {
    execFileSync('docker', composeArgs('logs', '--tail', '200', 'burgee'), { stdio: 'inherit' });
    throw error;
  }
}
