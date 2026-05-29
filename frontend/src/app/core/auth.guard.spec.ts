import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { signal } from '@angular/core';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

function runGuard() {
  return TestBed.runInInjectionContext(() =>
    authGuard({} as never, {} as never),
  );
}

describe('authGuard', () => {
  const authenticated = signal(false);
  const createUrlTree = vi.fn(() => ({}) as UrlTree);

  beforeEach(() => {
    authenticated.set(false);
    createUrlTree.mockClear();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: authenticated } },
        { provide: Router, useValue: { createUrlTree } },
      ],
    });
  });

  it('allows activation when authenticated', () => {
    authenticated.set(true);
    expect(runGuard()).toBe(true);
    expect(createUrlTree).not.toHaveBeenCalled();
  });

  it('redirects to /login when not authenticated', () => {
    authenticated.set(false);
    runGuard();
    expect(createUrlTree).toHaveBeenCalledWith(['/login']);
  });
});
