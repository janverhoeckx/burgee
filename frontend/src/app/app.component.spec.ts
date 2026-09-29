import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, signal, WritableSignal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthService, StorageMode } from './core/auth.service';

@Component({ standalone: true, template: 'login page' })
class LoginStubComponent {}

@Component({ standalone: true, template: 'flags page' })
class FlagsStubComponent {}

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let auth: {
    storage: WritableSignal<StorageMode>;
    isAuthenticated: WritableSignal<boolean>;
    username: WritableSignal<string | null>;
    clear: ReturnType<typeof vi.fn>;
  };

  async function renderAt(url: string) {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([
          { path: 'login', component: LoginStubComponent },
          { path: 'flags', component: FlagsStubComponent },
        ]),
        { provide: AuthService, useValue: auth },
      ],
    });
    fixture = TestBed.createComponent(AppComponent);
    fixture.autoDetectChanges();
    await TestBed.inject(Router).navigateByUrl(url);
    await fixture.whenStable();
  }

  function banner(): HTMLElement | null {
    return fixture.nativeElement.querySelector('[role="alert"]');
  }

  beforeEach(() => {
    auth = {
      storage: signal('memory'),
      isAuthenticated: signal(false),
      username: signal(null),
      clear: vi.fn(),
    };
  });

  it('warns on the login page that in-memory data is lost on restart', async () => {
    await renderAt('/login');

    expect(fixture.nativeElement.textContent).toContain('login page');
    expect(banner()?.textContent).toContain('In-memory mode: all changes are lost on restart');
  });

  it('keeps warning after signing in', async () => {
    auth.isAuthenticated.set(true);
    auth.username.set('admin');

    await renderAt('/flags');

    expect(fixture.nativeElement.textContent).toContain('flags page');
    expect(banner()?.textContent).toContain('In-memory mode: all changes are lost on restart');
  });

  it('offers no way to dismiss the warning', async () => {
    await renderAt('/login');

    expect(banner()?.querySelector('button')).toBeNull();
  });

  it('shows no warning when storage is postgres', async () => {
    auth.storage.set('postgres');

    await renderAt('/login');

    expect(banner()).toBeNull();
  });
});
