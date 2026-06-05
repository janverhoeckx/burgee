import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  const method = signal<'basic' | 'jwt'>('basic');
  const basicHeader = signal<string | null>(null);
  const jwtToken = signal<string | null>(null);
  const clear = vi.fn();
  const navigate = vi.fn();

  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    method.set('basic');
    basicHeader.set(null);
    jwtToken.set(null);
    clear.mockClear();
    navigate.mockClear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { method, basicHeader, jwtToken, clear },
        },
        { provide: Router, useValue: { navigate } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('attaches the basic auth header to admin requests', () => {
    basicHeader.set('Basic abc');
    http.get('/api/admin/flags').subscribe();

    const req = httpMock.expectOne('/api/admin/flags');
    expect(req.request.headers.get('Authorization')).toBe('Basic abc');
    req.flush([]);
  });

  it('does not attach an auth header to non-admin requests', () => {
    basicHeader.set('Basic abc');
    http.get('/api/auth/info').subscribe();

    const req = httpMock.expectOne('/api/auth/info');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('attaches a bearer token for jwt api requests', () => {
    method.set('jwt');
    jwtToken.set('jwt-token');
    http.get('/api/admin/flags').subscribe();

    const req = httpMock.expectOne('/api/admin/flags');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
  });

  it('clears credentials and redirects on a 401 for basic admin requests', () => {
    basicHeader.set('Basic abc');
    http.get('/api/admin/flags').subscribe({ error: () => {} });

    httpMock
      .expectOne('/api/admin/flags')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(clear).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('does not redirect on a 401 for non-admin basic requests', () => {
    http.get('/api/auth/info').subscribe({ error: () => {} });

    httpMock
      .expectOne('/api/auth/info')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(navigate).not.toHaveBeenCalled();
  });
});
