import type { APIResponse } from '@playwright/test';

/** Throws with the status and body on a non-2xx response, so a failed precondition says why. */
export async function ok(response: APIResponse): Promise<APIResponse> {
  if (!response.ok()) {
    throw new Error(`${response.status()} from ${response.url()}: ${await response.text()}`);
  }
  return response;
}

export async function json<T>(response: APIResponse): Promise<T> {
  return (await ok(response)).json() as Promise<T>;
}
