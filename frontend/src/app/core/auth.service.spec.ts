import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

const { getUser, signinRedirect, signinRedirectCallback, removeUser } = vi.hoisted(() => ({
  getUser: vi.fn((..._args: any[]) => Promise.resolve(null as unknown)),
  signinRedirect: vi.fn((..._args: any[]) => Promise.resolve()),
  signinRedirectCallback: vi.fn((..._args: any[]) => Promise.resolve(null as unknown)),
  removeUser: vi.fn((..._args: any[]) => Promise.resolve()),
}));

vi.mock('oidc-client-ts', () => ({
  UserManager: class {
    getUser = getUser;
    signinRedirect = signinRedirect;
    signinRedirectCallback = signinRedirectCallback;
    removeUser = removeUser;
  },
  WebStorageStateStore: class {},
}));

import { AuthService } from './auth.service';

const STORAGE_KEY = 'burgee.auth';

function makeService(): { service: AuthService; httpMock: HttpTestingController } {
  TestBed.configureTestingModule({
    providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
  });
  return {
    service: TestBed.inject(AuthService),
    httpMock: TestBed.inject(HttpTestingController),
  };
}

describe('AuthService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
    delete window.__burgeeConfig;
  });

  describe('basic auth', () => {
    it('defaults to an unauthenticated basic state with no stored credentials', () => {
      const { service } = makeService();
      expect(service.method()).toBe('basic');
      expect(service.isAuthenticated()).toBe(false);
      expect(service.username()).toBeNull();
      expect(service.basicHeader()).toBeNull();
    });

    it('hydrates state from existing sessionStorage credentials', () => {
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ username: 'bob', basic: 'Basic Ym9iOnB3' }),
      );
      const { service } = makeService();
      expect(service.isAuthenticated()).toBe(true);
      expect(service.username()).toBe('bob');
      expect(service.basicHeader()).toBe('Basic Ym9iOnB3');
    });

    it('ignores malformed sessionStorage data', () => {
      sessionStorage.setItem(STORAGE_KEY, 'not-json');
      const { service } = makeService();
      expect(service.isAuthenticated()).toBe(false);
    });

    it('stores base64-encoded credentials on storeBasic', () => {
      const { service } = makeService();
      service.storeBasic('alice', 'secret');

      const expected = 'Basic ' + btoa('alice:secret');
      expect(service.basicHeader()).toBe(expected);
      expect(service.username()).toBe('alice');
      expect(service.isAuthenticated()).toBe(true);
      expect(JSON.parse(sessionStorage.getItem(STORAGE_KEY)!)).toEqual({
        username: 'alice',
        basic: expected,
      });
    });

    it('clears stored credentials on clear', () => {
      const { service } = makeService();
      service.storeBasic('alice', 'secret');
      service.clear();

      expect(service.isAuthenticated()).toBe(false);
      expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
    });
  });

  describe('init', () => {
    it('swallows errors from the auth info endpoint and stays on basic', () => {
      const { service, httpMock } = makeService();
      let completed = false;
      service.init().subscribe({ complete: () => (completed = true) });

      httpMock.expectOne('/api/auth/info').error(new ProgressEvent('error'));

      expect(completed).toBe(true);
      expect(service.method()).toBe('basic');
      httpMock.verify();
    });

    it('keeps jwt unauthenticated when the stored token fails to resolve', async () => {
      getUser.mockResolvedValue(null);

      const { service, httpMock } = makeService();
      const done = new Promise<void>((resolve) =>
        service.init().subscribe({ complete: resolve }),
      );

      httpMock.expectOne('/api/auth/info').flush({
        method: 'jwt',
        oidc: { issuerUri: 'https://idp.example.com', clientId: 'spa', scope: 'openid' },
      });

      await done;

      expect(service.method()).toBe('jwt');
      expect(service.isAuthenticated()).toBe(false);
      httpMock.verify();
    });

    it('initializes the oidc client and resolves the stored user token', async () => {
      getUser.mockResolvedValue({
        id_token: 'jwt-token',
        access_token: 'opaque-access-token',
        expired: false,
        profile: { name: 'Dana', email: 'dana@example.com', sub: 'uid-1' },
      });

      const { service, httpMock } = makeService();
      const done = new Promise<void>((resolve) =>
        service.init().subscribe({ complete: resolve }),
      );

      httpMock.expectOne('/api/auth/info').flush({
        method: 'jwt',
        oidc: { issuerUri: 'https://idp.example.com', clientId: 'spa', scope: 'openid' },
      });

      // Once the stored token resolves, the backend user (role) is fetched.
      await new Promise((resolve) => setTimeout(resolve, 0));
      httpMock.expectOne('/api/auth/user').flush({ name: 'Dana', role: 'USER', isAdmin: false });

      await done;

      expect(getUser).toHaveBeenCalled();
      expect(service.method()).toBe('jwt');
      expect(service.jwtToken()).toBe('opaque-access-token');
      expect(service.username()).toBe('Dana');
      expect(service.isAuthenticated()).toBe(true);
      expect(service.isAdmin()).toBe(false);
      httpMock.verify();
    });
  });

  describe('signIn', () => {
    it('does nothing when the oidc client is not initialized', async () => {
      const { service } = makeService();
      await service.signIn();
      expect(signinRedirect).not.toHaveBeenCalled();
    });
  });

});
