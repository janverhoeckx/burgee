import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { initializeApp } from 'firebase/app';
import {
  Auth,
  GoogleAuthProvider,
  getAuth,
  onIdTokenChanged,
  signInWithPopup,
  signOut,
} from 'firebase/auth';
import { apiBaseUrl } from './api.config';

const STORAGE_KEY = 'burgee.auth';

interface StoredCredentials {
  username: string;
  basic: string;
}

export interface OAuthProvider {
  id: string;
  name: string;
  loginUrl: string;
}

interface FirebaseConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
}

interface AuthInfo {
  method: 'basic' | 'oauth2' | 'firebase';
  providers: OAuthProvider[];
  firebase?: FirebaseConfig;
}

interface UserInfo {
  name: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _method = signal<'basic' | 'oauth2' | 'firebase'>('basic');
  private readonly _providers = signal<OAuthProvider[]>([]);
  private readonly _basicState = signal<StoredCredentials | null>(this.readBasic());
  private readonly _oauthUser = signal<UserInfo | null>(null);
  private readonly _firebaseToken = signal<string | null>(null);
  private readonly _firebaseUser = signal<UserInfo | null>(null);

  private firebaseAuth: Auth | null = null;

  readonly method = this._method.asReadonly();
  readonly providers = this._providers.asReadonly();
  readonly firebaseToken = this._firebaseToken.asReadonly();

  readonly username = computed(() => {
    switch (this._method()) {
      case 'firebase':
        return this._firebaseUser()?.name ?? null;
      case 'oauth2':
        return this._oauthUser()?.name ?? null;
      default:
        return this._basicState()?.username ?? null;
    }
  });

  readonly basicHeader = computed(() =>
    this._method() === 'basic' ? (this._basicState()?.basic ?? null) : null,
  );

  readonly isAuthenticated = computed(() => {
    switch (this._method()) {
      case 'firebase':
        return this._firebaseToken() !== null;
      case 'oauth2':
        return this._oauthUser() !== null;
      default:
        return this._basicState() !== null;
    }
  });

  init(): Observable<void> {
    return this.http.get<AuthInfo>(`${apiBaseUrl()}/api/auth/info`).pipe(
      switchMap((info) => {
        this._method.set(info.method);
        this._providers.set(info.providers ?? []);
        if (info.method === 'oauth2') {
          return this.fetchOAuthUser();
        }
        if (info.method === 'firebase' && info.firebase) {
          return this.initFirebase(info.firebase);
        }
        return of(undefined as void);
      }),
      catchError(() => of(undefined as void)),
    );
  }

  storeBasic(username: string, password: string): void {
    const basic = 'Basic ' + btoa(`${username}:${password}`);
    const creds: StoredCredentials = { username, basic };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(creds));
    this._basicState.set(creds);
  }

  async signInWithGoogle(): Promise<void> {
    if (!this.firebaseAuth) return;
    const provider = new GoogleAuthProvider();
    const result = await signInWithPopup(this.firebaseAuth, provider);
    const token = await result.user.getIdToken();
    this._firebaseToken.set(token);
    this._firebaseUser.set({
      name: result.user.displayName || result.user.email || result.user.uid,
    });
  }

  clear(): void {
    if (this._method() === 'firebase') {
      if (this.firebaseAuth) {
        signOut(this.firebaseAuth);
      }
      this._firebaseToken.set(null);
      this._firebaseUser.set(null);
      return;
    }
    if (this._method() === 'oauth2') {
      window.location.href = `${apiBaseUrl()}/api/auth/logout`;
      return;
    }
    sessionStorage.removeItem(STORAGE_KEY);
    this._basicState.set(null);
  }

  private initFirebase(config: FirebaseConfig): Observable<void> {
    const app = initializeApp(config);
    this.firebaseAuth = getAuth(app);

    return new Observable<void>((subscriber) => {
      let first = true;
      onIdTokenChanged(this.firebaseAuth!, (user) => {
        const complete = () => {
          if (first) {
            first = false;
            subscriber.next();
            subscriber.complete();
          }
        };
        if (user) {
          user.getIdToken().then(
            (token) => {
              this._firebaseToken.set(token);
              this._firebaseUser.set({
                name: user.displayName || user.email || user.uid,
              });
              complete();
            },
            () => {
              this._firebaseToken.set(null);
              this._firebaseUser.set(null);
              complete();
            },
          );
        } else {
          this._firebaseToken.set(null);
          this._firebaseUser.set(null);
          complete();
        }
      });
    });
  }

  private fetchOAuthUser(): Observable<void> {
    return this.http
      .get<UserInfo>(`${apiBaseUrl()}/api/auth/user`, { withCredentials: true })
      .pipe(
        tap((user) => this._oauthUser.set(user)),
        map(() => undefined as void),
        catchError(() => of(undefined as void)),
      );
  }

  private readBasic(): StoredCredentials | null {
    const raw =
      typeof sessionStorage === 'undefined' ? null : sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredCredentials;
    } catch {
      return null;
    }
  }
}
