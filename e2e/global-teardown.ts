import { execFileSync } from 'node:child_process';
import { composeArgs, e2eEnv } from './env';

export default async function globalTeardown(): Promise<void> {
  if (e2eEnv.keepStack) {
    console.log(`E2E_KEEP_STACK=1: leaving compose project "${e2eEnv.project}" running`);
    return;
  }
  execFileSync('docker', composeArgs('down', '-v', '--remove-orphans'), { stdio: 'inherit' });
}
