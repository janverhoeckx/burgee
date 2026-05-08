import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'burgee.auth';

interface StoredCredentials {
  username: string;
  basic: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly state = signal<StoredCredentials | null>(this.read());

  readonly username = (): string | null => this.state()?.username ?? null;
  readonly basicHeader = (): string | null => this.state()?.basic ?? null;
  readonly isAuthenticated = (): boolean => this.state() !== null;

  store(username: string, password: string): void {
    const basic = 'Basic ' + btoa(`${username}:${password}`);
    const creds = { username, basic };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(creds));
    this.state.set(creds);
  }

  clear(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.state.set(null);
  }

  private read(): StoredCredentials | null {
    const raw = typeof sessionStorage === 'undefined' ? null : sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredCredentials;
    } catch {
      return null;
    }
  }
}
