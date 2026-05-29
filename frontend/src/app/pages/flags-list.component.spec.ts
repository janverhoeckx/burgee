import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FlagsListComponent } from './flags-list.component';
import { FeatureFlag, FlagService } from '../core/flag.service';

function flag(overrides: Partial<FeatureFlag> = {}): FeatureFlag {
  return {
    id: 'flag-1',
    key: 'new-checkout',
    name: 'New Checkout',
    description: 'desc',
    enabled: false,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('FlagsListComponent', () => {
  let fixture: ComponentFixture<FlagsListComponent>;
  let service: {
    list: ReturnType<typeof vi.fn>;
    toggle: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  function setup() {
    TestBed.configureTestingModule({
      imports: [FlagsListComponent],
      providers: [provideRouter([]), { provide: FlagService, useValue: service }],
    });
    fixture = TestBed.createComponent(FlagsListComponent);
    fixture.autoDetectChanges();
  }

  beforeEach(() => {
    service = {
      list: vi.fn(() => of([flag()])),
      toggle: vi.fn(),
      delete: vi.fn(),
    };
  });

  it('renders a row per flag once loaded', async () => {
    service.list.mockReturnValue(of([flag({ id: 'a', key: 'k-a' }), flag({ id: 'b', key: 'k-b' })]));
    setup();
    await fixture.whenStable();

    const rows = fixture.nativeElement.querySelectorAll('table.flags tbody tr');
    expect(rows.length).toBe(2);
    expect(fixture.nativeElement.querySelector('.header .muted').textContent).toContain('2 flags');
  });

  it('shows the empty state when there are no flags', async () => {
    service.list.mockReturnValue(of([]));
    setup();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('table.flags')).toBeNull();
  });

  it('shows an error message when loading fails', async () => {
    service.list.mockReturnValue(throwError(() => new Error('boom')));
    setup();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.error').textContent).toContain(
      'Failed to load flags.',
    );
  });

  it('replaces the flag in the list when toggled', async () => {
    const original = flag({ enabled: false });
    service.list.mockReturnValue(of([original]));
    service.toggle.mockReturnValue(of(flag({ enabled: true })));
    setup();
    await fixture.whenStable();

    fixture.componentInstance.toggle(original);
    await fixture.whenStable();

    expect(service.toggle).toHaveBeenCalledWith('flag-1');
    const checkbox = fixture.nativeElement.querySelector('input.toggle') as HTMLInputElement;
    expect(checkbox.checked).toBe(true);
  });

  it('navigates to the edit route on edit', async () => {
    setup();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.edit(flag());
    expect(navigate).toHaveBeenCalledWith(['/flags', 'flag-1', 'edit']);
  });

  it('removes a flag after confirmation', async () => {
    service.list.mockReturnValue(of([flag()]));
    service.delete.mockReturnValue(of(undefined));
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    setup();
    await fixture.whenStable();

    fixture.componentInstance.remove(flag());
    await fixture.whenStable();

    expect(service.delete).toHaveBeenCalledWith('flag-1');
    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
  });

  it('does not delete when confirmation is declined', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    setup();
    await fixture.whenStable();

    fixture.componentInstance.remove(flag());

    expect(service.delete).not.toHaveBeenCalled();
  });
});
