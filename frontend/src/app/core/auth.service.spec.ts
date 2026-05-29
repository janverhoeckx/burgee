import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

const { onIdTokenChanged, signInWithPopup, signOut, getAuth, initializeApp } = vi.hoisted(
  () => ({
    onIdTokenChanged: vi.fn((..._args: any[]) => undefined),
    signInWithPopup: vi.fn((..._args: any[]) => undefined),
    signOut: vi.fn((..._args: any[]) => undefined),
    getAuth: vi.fn((..._args: any[]) => ({}) as unknown),
    initializeApp: vi.fn((..._args: any[]) => ({}) as unknown),
  }),
);

vi.mock('firebase/app', () => ({ initializeApp }));

vi.mock('firebase/auth', () => ({
  GoogleAuthProvider: class {},
  getAuth,
  onIdTokenChanged,
  signInWithPopup,
  signOut,
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
    it('sets method and providers from the auth info endpoint', () => {
      const { service, httpMock } = makeService();
      service.init().subscribe();

      const req = httpMock.expectOne('/api/auth/info');
      req.flush({
        method: 'oauth2',
        providers: [{ id: 'google', name: 'Google', loginUrl: '/oauth/google' }],
      });

      // oauth2 method triggers a follow-up user fetch
      const userReq = httpMock.expectOne('/api/auth/user');
      userReq.flush({ name: 'Charlie' });

      expect(service.method()).toBe('oauth2');
      expect(service.providers()).toHaveLength(1);
      expect(service.username()).toBe('Charlie');
      expect(service.isAuthenticated()).toBe(true);
      httpMock.verify();
    });

    it('swallows errors from the auth info endpoint and stays on basic', () => {
      const { service, httpMock } = makeService();
      let completed = false;
      service.init().subscribe({ complete: () => (completed = true) });

      httpMock.expectOne('/api/auth/info').error(new ProgressEvent('error'));

      expect(completed).toBe(true);
      expect(service.method()).toBe('basic');
      httpMock.verify();
    });

    it('keeps oauth unauthenticated when the user fetch fails', () => {
      const { service, httpMock } = makeService();
      service.init().subscribe();

      httpMock.expectOne('/api/auth/info').flush({ method: 'oauth2', providers: [] });
      httpMock.expectOne('/api/auth/user').error(new ProgressEvent('error'));

      expect(service.method()).toBe('oauth2');
      expect(service.isAuthenticated()).toBe(false);
      httpMock.verify();
    });

    it('initializes firebase and resolves the first token from onIdTokenChanged', async () => {
      onIdTokenChanged.mockImplementation((_auth: unknown, cb: (user: unknown) => void) => {
        cb({
          displayName: 'Dana',
          email: 'dana@example.com',
          uid: 'uid-1',
          getIdToken: () => Promise.resolve('fb-token'),
        });
      });

      const { service, httpMock } = makeService();
      const done = new Promise<void>((resolve) =>
        service.init().subscribe({ complete: resolve }),
      );

      httpMock
        .expectOne('/api/auth/info')
        .flush({ method: 'firebase', providers: [], firebase: { apiKey: 'k', authDomain: 'd', projectId: 'p' } });

      await done;

      expect(initializeApp).toHaveBeenCalled();
      expect(service.method()).toBe('firebase');
      expect(service.firebaseToken()).toBe('fb-token');
      expect(service.username()).toBe('Dana');
      expect(service.isAuthenticated()).toBe(true);
      httpMock.verify();
    });
  });

  describe('signInWithGoogle', () => {
    it('does nothing when firebase is not initialized', async () => {
      const { service } = makeService();
      await service.signInWithGoogle();
      expect(signInWithPopup).not.toHaveBeenCalled();
    });
  });

  describe('clear for oauth', () => {
    it('redirects to the logout endpoint', () => {
      const { service, httpMock } = makeService();
      service.init().subscribe();
      httpMock.expectOne('/api/auth/info').flush({ method: 'oauth2', providers: [] });
      httpMock.expectOne('/api/auth/user').flush({ name: 'Charlie' });

      const hrefSpy = vi.fn();
      Object.defineProperty(window, 'location', {
        value: { ...window.location, set href(v: string) { hrefSpy(v); } },
        writable: true,
      });

      service.clear();
      expect(hrefSpy).toHaveBeenCalledWith('/api/auth/logout');
      httpMock.verify();
    });
  });
});
