import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuditListComponent } from './audit-list.component';
import { AuditEntry, AuditService } from '../core/audit.service';

function entry(overrides: Partial<AuditEntry> = {}): AuditEntry {
  return {
    id: 'audit-1',
    flagId: 'flag-1',
    flagKey: 'new-checkout',
    action: 'CREATE',
    actor: 'alice',
    detail: 'created',
    occurredAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('AuditListComponent', () => {
  let fixture: ComponentFixture<AuditListComponent>;
  let service: { list: ReturnType<typeof vi.fn> };
  let flagIdParam: string | null;

  function setup() {
    TestBed.configureTestingModule({
      imports: [AuditListComponent],
      providers: [
        provideRouter([]),
        { provide: AuditService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: { get: () => flagIdParam } },
          },
        },
      ],
    });
    fixture = TestBed.createComponent(AuditListComponent);
    fixture.autoDetectChanges();
  }

  beforeEach(() => {
    flagIdParam = null;
    service = { list: vi.fn(() => of([entry()])) };
  });

  it('lists all entries and passes no flag filter by default', async () => {
    service.list.mockReturnValue(of([entry({ id: 'a' }), entry({ id: 'b', actor: 'bob' })]));
    setup();
    await fixture.whenStable();

    expect(service.list).toHaveBeenCalledWith(undefined);
    expect(fixture.nativeElement.querySelectorAll('table.audit tbody tr').length).toBe(2);
  });

  it('forwards the flagId query param to the service and header', async () => {
    flagIdParam = 'flag-99';
    setup();
    await fixture.whenStable();

    expect(service.list).toHaveBeenCalledWith('flag-99');
    expect(fixture.nativeElement.querySelector('.header .muted').textContent).toContain(
      'History for one flag',
    );
  });

  it('shows the empty state when there are no entries', async () => {
    service.list.mockReturnValue(of([]));
    setup();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.empty').textContent).toContain(
      'No audit events yet.',
    );
  });

  it('shows an error message when loading fails', async () => {
    service.list.mockReturnValue(throwError(() => new Error('boom')));
    setup();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.error').textContent).toContain(
      'Failed to load audit trail.',
    );
  });

  it('filters entries by the search term', async () => {
    service.list.mockReturnValue(
      of([entry({ id: 'a', actor: 'alice' }), entry({ id: 'b', actor: 'bob' })]),
    );
    setup();
    await fixture.whenStable();

    const input = fixture.nativeElement.querySelector(
      'input[type="search"]',
    ) as HTMLInputElement;
    input.value = 'bob';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    const rows = fixture.nativeElement.querySelectorAll('table.audit tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('bob');
  });

  it('shows a no-match message when the search excludes everything', async () => {
    setup();
    await fixture.whenStable();

    const input = fixture.nativeElement.querySelector(
      'input[type="search"]',
    ) as HTMLInputElement;
    input.value = 'zzz';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.empty').textContent).toContain(
      'No events match',
    );
  });
});
