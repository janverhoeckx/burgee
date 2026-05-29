import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { signal, WritableSignal } from '@angular/core';
import { LoginComponent } from './login.component';
import { AuthService, OAuthProvider } from '../core/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let httpMock: HttpTestingController;
  let auth: {
    method: WritableSignal<'basic' | 'oauth2' | 'firebase'>;
    providers: WritableSignal<OAuthProvider[]>;
    isAuthenticated: WritableSignal<boolean>;
    storeBasic: ReturnType<typeof vi.fn>;
    signInWithGoogle: ReturnType<typeof vi.fn>;
  };

  function setup() {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    fixture = TestBed.createComponent(LoginComponent);
    fixture.autoDetectChanges();
    httpMock = TestBed.inject(HttpTestingController);
  }

  beforeEach(() => {
    auth = {
      method: signal('basic'),
      providers: signal<OAuthProvider[]>([]),
      isAuthenticated: signal(false),
      storeBasic: vi.fn(),
      signInWithGoogle: vi.fn(() => Promise.resolve()),
    };
  });

  function type(selector: string, value: string) {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('redirects to /flags when already authenticated', async () => {
    const navigate = vi.spyOn(Router.prototype, 'navigate').mockResolvedValue(true);
    auth.isAuthenticated.set(true);
    setup();
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(['/flags']);
  });

  it('renders the basic credentials form by default', async () => {
    setup();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#username')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#password')).toBeTruthy();
  });

  it('probes the API, stores credentials, and navigates on a successful basic login', async () => {
    setup();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    type('#username', 'alice');
    type('#password', 'secret');
    await fixture.whenStable();

    fixture.componentInstance.submit();

    const req = httpMock.expectOne('/api/admin/flags');
    expect(req.request.headers.get('Authorization')).toBe('Basic ' + btoa('alice:secret'));
    req.flush([]);
    await fixture.whenStable();

    expect(auth.storeBasic).toHaveBeenCalledWith('alice', 'secret');
    expect(navigate).toHaveBeenCalledWith(['/flags']);
  });

  it('shows an invalid-credentials message on a 401', async () => {
    setup();
    await fixture.whenStable();

    type('#username', 'alice');
    type('#password', 'wrong');
    await fixture.whenStable();

    fixture.componentInstance.submit();

    httpMock
      .expectOne('/api/admin/flags')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.error').textContent).toContain(
      'Invalid credentials',
    );
    expect(auth.storeBasic).not.toHaveBeenCalled();
  });

  it('renders provider links in oauth2 mode', async () => {
    auth.method.set('oauth2');
    auth.providers.set([{ id: 'google', name: 'Google', loginUrl: '/oauth/google' }]);
    setup();
    await fixture.whenStable();

    const link = fixture.nativeElement.querySelector('a.provider-btn') as HTMLAnchorElement;
    expect(link.textContent).toContain('Sign in with Google');
    expect(link.getAttribute('href')).toContain('/oauth/google');
  });

  it('signs in with Google and navigates in firebase mode', async () => {
    auth.method.set('firebase');
    setup();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    await fixture.componentInstance.signInWithGoogle();

    expect(auth.signInWithGoogle).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/flags']);
  });
});
