import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, from, map, of, switchMap, tap } from 'rxjs';
import { User as OidcUser, UserManager, WebStorageStateStore } from 'oidc-client-ts';
import { apiBaseUrl } from './api.config';

const STORAGE_KEY = 'burgee.auth';

type AuthMethod = 'basic' | 'jwt';

interface StoredCredentials {
  username: string;
  basic: string;
}

interface OidcConfig {
  issuerUri: string;
  clientId: string;
  scope: string;
  resourceUri?: string | null;
}

interface AuthInfo {
  method: AuthMethod;
  oidc?: OidcConfig;
}

interface UserInfo {
  name: string;
  role?: string;
  isAdmin?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _method = signal<AuthMethod>('basic');
  private readonly _basicState = signal<StoredCredentials | null>(this.readBasic());
  private readonly _jwtToken = signal<string | null>(null);
  private readonly _jwtUser = signal<UserInfo | null>(null);
  private readonly _role = signal<string | null>(null);

  private userManager: UserManager | null = null;

  readonly method = this._method.asReadonly();
  readonly jwtToken = this._jwtToken.asReadonly();
  readonly role = this._role.asReadonly();
  readonly isAdmin = computed(() => this._role() === 'ADMIN');

  readonly username = computed(() => {
    switch (this._method()) {
      case 'jwt':
        return this._jwtUser()?.name ?? null;
      default:
        return this._basicState()?.username ?? null;
    }
  });

  readonly basicHeader = computed(() =>
    this._method() === 'basic' ? (this._basicState()?.basic ?? null) : null,
  );

  readonly isAuthenticated = computed(() => {
    switch (this._method()) {
      case 'jwt':
        return this._jwtToken() !== null;
      default:
        return this._basicState() !== null;
    }
  });

  init(): Observable<void> {
    return this.http.get<AuthInfo>(`${apiBaseUrl()}/api/auth/info`).pipe(
      switchMap((info) => {
        this._method.set(info.method);
        if (info.method === 'jwt' && info.oidc) {
          return this.initOidc(info.oidc).pipe(
            switchMap(() => (this._jwtToken() ? this.fetchBackendUser() : of(undefined as void))),
          );
        }
        if (this._basicState()) {
          return this.fetchBackendUser();
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
    this.fetchBackendUser().subscribe();
  }

  async signIn(): Promise<void> {
    if (!this.userManager) return;
    await this.userManager.signinRedirect();
  }

  clear(): void {
    this._role.set(null);
    if (this._method() === 'jwt') {
      this._jwtToken.set(null);
      this._jwtUser.set(null);
      void this.userManager?.removeUser();
      return;
    }
    sessionStorage.removeItem(STORAGE_KEY);
    this._basicState.set(null);
  }

  private initOidc(config: OidcConfig): Observable<void> {
    const redirectUri = `${window.location.origin}/login`;
    this.userManager = new UserManager({
      authority: config.issuerUri,
      client_id: config.clientId,
      redirect_uri: redirectUri,
      post_logout_redirect_uri: redirectUri,
      response_type: 'code',
      scope: config.scope,

      ...(config.resourceUri ? { resource: config.resourceUri } : {}),
      userStore: new WebStorageStateStore({ store: window.localStorage }),
    });

    const isRedirectCallback =
      window.location.search.includes('code=') && window.location.search.includes('state=');

    const resolveUser = isRedirectCallback
      ? this.userManager.signinRedirectCallback().then((user) => {
          window.history.replaceState({}, document.title, redirectUri);
          return user;
        })
      : this.userManager.getUser();

    return from(resolveUser).pipe(
      tap((user) => this.applyOidcUser(user)),
      map(() => undefined as void),
      catchError(() => of(undefined as void)),
    );
  }

  private applyOidcUser(user: OidcUser | null): void {
    if (!user || user.expired) {
      this._jwtToken.set(null);
      this._jwtUser.set(null);
      return;
    }
    this._jwtToken.set(user.access_token ?? null);
    const profile = user.profile;
    this._jwtUser.set({
      name: profile.name || profile.email || profile.sub,
    });
  }

  private fetchBackendUser(): Observable<void> {
    return this.http
      .get<UserInfo>(`${apiBaseUrl()}/api/auth/user`, { withCredentials: true })
      .pipe(
        tap((user) => {
          this._role.set(user.role ?? null);
        }),
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
