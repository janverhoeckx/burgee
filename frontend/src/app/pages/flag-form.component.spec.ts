import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FlagFormComponent } from './flag-form.component';
import { FeatureFlag, FlagService } from '../core/flag.service';

function flag(overrides: Partial<FeatureFlag> = {}): FeatureFlag {
  return {
    id: 'flag-1',
    key: 'new-checkout',
    name: 'New Checkout',
    description: 'desc',
    enabled: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

function type(el: HTMLElement, selector: string, value: string) {
  const input = el.querySelector(selector) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

describe('FlagFormComponent', () => {
  let fixture: ComponentFixture<FlagFormComponent>;
  let service: {
    get: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let idParam: string | null;

  function setup() {
    TestBed.configureTestingModule({
      imports: [FlagFormComponent],
      providers: [
        provideRouter([]),
        { provide: FlagService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => idParam } } },
        },
      ],
    });
    fixture = TestBed.createComponent(FlagFormComponent);
    fixture.autoDetectChanges();
  }

  beforeEach(() => {
    idParam = null;
    service = {
      get: vi.fn(() => of(flag())),
      create: vi.fn(() => of(flag())),
      update: vi.fn(() => of(flag())),
    };
  });

  describe('create mode', () => {
    it('renders the "New flag" heading and a disabled save button while empty', async () => {
      setup();
      await fixture.whenStable();

      expect(fixture.nativeElement.querySelector('h1').textContent).toContain('New flag');
      const save = fixture.nativeElement.querySelector(
        'button[type="submit"]',
      ) as HTMLButtonElement;
      expect(save.disabled).toBe(true);
    });

    it('creates a flag and navigates to /flags on a valid submit', async () => {
      setup();
      await fixture.whenStable();
      const router = TestBed.inject(Router);
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      type(fixture.nativeElement, '#key', 'my-flag');
      type(fixture.nativeElement, '#name', 'My Flag');
      type(fixture.nativeElement, '#description', 'hello');
      await fixture.whenStable();

      fixture.componentInstance.submit();
      await fixture.whenStable();

      expect(service.create).toHaveBeenCalledWith({
        key: 'my-flag',
        name: 'My Flag',
        description: 'hello',
        enabled: false,
      });
      expect(navigate).toHaveBeenCalledWith(['/flags']);
    });

    it('shows a key pattern error for an invalid key', async () => {
      setup();
      await fixture.whenStable();

      const key = fixture.nativeElement.querySelector('#key') as HTMLInputElement;
      key.value = 'Invalid Key!';
      key.dispatchEvent(new Event('input'));
      key.dispatchEvent(new Event('blur'));
      await fixture.whenStable();

      expect(fixture.nativeElement.querySelector('.error').textContent).toContain(
        'Lowercase letters',
      );
    });

    it('surfaces a 409 conflict as a friendly message', async () => {
      service.create.mockReturnValue(throwError(() => ({ status: 409 })));
      setup();
      await fixture.whenStable();

      type(fixture.nativeElement, '#key', 'my-flag');
      type(fixture.nativeElement, '#name', 'My Flag');
      await fixture.whenStable();

      fixture.componentInstance.submit();
      await fixture.whenStable();

      expect(fixture.nativeElement.querySelector('.error').textContent).toContain(
        'already exists',
      );
    });
  });

  describe('edit mode', () => {
    beforeEach(() => {
      idParam = 'flag-1';
    });

    it('loads the flag, fills the form, and disables the key field', async () => {
      setup();
      await fixture.whenStable();

      expect(service.get).toHaveBeenCalledWith('flag-1');
      expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Edit flag');
      const key = fixture.nativeElement.querySelector('#key') as HTMLInputElement;
      expect(key.value).toBe('new-checkout');
      expect(key.disabled).toBe(true);
    });

    it('updates the flag on submit without sending the key', async () => {
      setup();
      await fixture.whenStable();
      const router = TestBed.inject(Router);
      vi.spyOn(router, 'navigate').mockResolvedValue(true);

      fixture.componentInstance.submit();
      await fixture.whenStable();

      expect(service.update).toHaveBeenCalledWith('flag-1', {
        name: 'New Checkout',
        description: 'desc',
        enabled: true,
      });
    });
  });
});
