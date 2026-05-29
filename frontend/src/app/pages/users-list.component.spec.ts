import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { UsersListComponent } from './users-list.component';
import { User, UserService } from '../core/user.service';

function user(overrides: Partial<User> = {}): User {
  return {
    id: 'user-1',
    subject: 'jane',
    email: 'jane@example.com',
    displayName: 'Jane Doe',
    role: 'USER',
    provider: 'OAUTH2',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('UsersListComponent', () => {
  let fixture: ComponentFixture<UsersListComponent>;
  let service: {
    list: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  function setup() {
    TestBed.configureTestingModule({
      imports: [UsersListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserService, useValue: service },
      ],
    });
    fixture = TestBed.createComponent(UsersListComponent);
    fixture.autoDetectChanges();
  }

  beforeEach(() => {
    service = {
      list: vi.fn(() => of([user()])),
      delete: vi.fn(),
    };
  });

  it('renders a row per user once loaded', async () => {
    service.list.mockReturnValue(of([user({ id: 'a', subject: 's-a' }), user({ id: 'b', subject: 's-b' })]));
    setup();
    await fixture.whenStable();

    const rows = fixture.nativeElement.querySelectorAll('table.users tbody tr');
    expect(rows.length).toBe(2);
    expect(fixture.nativeElement.querySelector('.header .muted').textContent).toContain('2 users');
  });

  it('shows the empty state when there are no users', async () => {
    service.list.mockReturnValue(of([]));
    setup();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('table.users')).toBeNull();
  });

  it('shows an error message when loading fails', async () => {
    service.list.mockReturnValue(throwError(() => new Error('boom')));
    setup();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.error').textContent).toContain(
      'Failed to load users.',
    );
  });

  it('navigates to the edit route on edit', async () => {
    setup();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.edit(user());
    expect(navigate).toHaveBeenCalledWith(['/users', 'user-1', 'edit']);
  });

  it('removes a user after confirmation', async () => {
    service.list.mockReturnValue(of([user()]));
    service.delete.mockReturnValue(of(undefined));
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    setup();
    await fixture.whenStable();

    fixture.componentInstance.remove(user());
    await fixture.whenStable();

    expect(service.delete).toHaveBeenCalledWith('user-1');
    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
  });

  it('does not delete when confirmation is declined', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    setup();
    await fixture.whenStable();

    fixture.componentInstance.remove(user());

    expect(service.delete).not.toHaveBeenCalled();
  });
});
