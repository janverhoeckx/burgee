import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FlagFormComponent } from './flag-form.component';
import { Condition, FeatureFlag, FlagService } from '../core/flag.service';

function flag(overrides: Partial<FeatureFlag> = {}): FeatureFlag {
  return {
    id: 'flag-1',
    key: 'new-checkout',
    name: 'New Checkout',
    description: 'desc',
    enabled: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    conditions: [],
    ...overrides,
  };
}

function type(el: HTMLElement, selector: string, value: string) {
  const input = el.querySelector(selector) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

const targeted: Condition[] = [
  { attribute: 'organisationId', operator: 'IN', values: ['acme', 'globex'] },
  { attribute: 'plan', operator: 'IN', values: ['pro'] },
];

function conditionRows(el: HTMLElement): HTMLElement[] {
  return Array.from(el.querySelectorAll('.condition'));
}

function click(el: HTMLElement, selector: string) {
  (el.querySelector(selector) as HTMLButtonElement).click();
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
        conditions: [],
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

    it('lists backend field errors that are not about a Condition', async () => {
      service.create.mockReturnValue(
        throwError(() => ({
          status: 400,
          error: { message: 'Validation failed', fieldErrors: { name: 'size must be between 0 and 256' } },
        })),
      );
      setup();
      await fixture.whenStable();

      type(fixture.nativeElement, '#key', 'my-flag');
      type(fixture.nativeElement, '#name', 'My Flag');
      await fixture.whenStable();
      click(fixture.nativeElement, 'button[type="submit"]');
      await fixture.whenStable();

      expect(fixture.nativeElement.textContent).toContain('name: size must be between 0 and 256');
    });
  });

  describe('conditions editor', () => {
    it('starts without Conditions and adds an IN row with an Attribute input and a values textarea', async () => {
      setup();
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;

      expect(conditionRows(el).length).toBe(0);

      click(el, 'button.add-condition');
      await fixture.whenStable();

      const rows = conditionRows(el);
      expect(rows.length).toBe(1);
      expect(rows[0].querySelector('input.condition-attribute')).toBeTruthy();
      expect(rows[0].querySelector('textarea.condition-values')).toBeTruthy();
      expect(rows[0].querySelector('.condition-operator')?.textContent).toContain('IN');
    });

    it('removes the chosen row and keeps the others', async () => {
      setup();
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;

      click(el, 'button.add-condition');
      click(el, 'button.add-condition');
      await fixture.whenStable();
      type(conditionRows(el)[0], 'input.condition-attribute', 'country');
      type(conditionRows(el)[1], 'input.condition-attribute', 'plan');
      await fixture.whenStable();

      (conditionRows(el)[0].querySelector('button.remove-condition') as HTMLButtonElement).click();
      await fixture.whenStable();

      const rows = conditionRows(el);
      expect(rows.length).toBe(1);
      expect((rows[0].querySelector('input.condition-attribute') as HTMLInputElement).value).toBe(
        'plan',
      );
    });

    it('sends the Conditions on create, trimming values and ignoring empty lines', async () => {
      setup();
      await fixture.whenStable();
      vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      const el: HTMLElement = fixture.nativeElement;

      type(el, '#key', 'my-flag');
      type(el, '#name', 'My Flag');
      click(el, 'button.add-condition');
      await fixture.whenStable();
      type(conditionRows(el)[0], 'input.condition-attribute', ' organisationId ');
      type(conditionRows(el)[0], 'textarea.condition-values', '  acme \n\n globex\n   \n');
      await fixture.whenStable();

      click(el, 'button[type="submit"]');
      await fixture.whenStable();

      expect(service.create).toHaveBeenCalledWith({
        key: 'my-flag',
        name: 'My Flag',
        description: null,
        enabled: false,
        conditions: [{ attribute: 'organisationId', operator: 'IN', values: ['acme', 'globex'] }],
      });
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

    it('loads the flag\'s Conditions into the editor, one value per line', async () => {
      service.get.mockReturnValue(of(flag({ conditions: targeted })));
      setup();
      await fixture.whenStable();

      const rows = conditionRows(fixture.nativeElement);
      expect(rows.length).toBe(2);
      expect((rows[0].querySelector('input.condition-attribute') as HTMLInputElement).value).toBe(
        'organisationId',
      );
      expect((rows[0].querySelector('textarea.condition-values') as HTMLTextAreaElement).value).toBe(
        'acme\nglobex',
      );
      expect((rows[1].querySelector('input.condition-attribute') as HTMLInputElement).value).toBe(
        'plan',
      );
      expect((rows[1].querySelector('textarea.condition-values') as HTMLTextAreaElement).value).toBe(
        'pro',
      );
    });

    it('shows backend field errors for Conditions on the matching row', async () => {
      service.get.mockReturnValue(of(flag({ conditions: targeted })));
      service.update.mockReturnValue(
        throwError(() => ({
          status: 400,
          error: {
            message: 'Validation failed',
            fieldErrors: {
              'conditions[1].attribute': "duplicate attribute 'organisationId'",
              'conditions[0].values[1]': 'must not be blank and be at most 256 characters',
            },
          },
        })),
      );
      setup();
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;

      click(el, 'button[type="submit"]');
      await fixture.whenStable();

      const rows = conditionRows(el);
      expect(rows[0].querySelector('.condition-values-field .error')?.textContent).toContain(
        'Line 2: must not be blank and be at most 256 characters',
      );
      expect(rows[1].querySelector('.condition-attribute-field .error')?.textContent).toContain(
        "duplicate attribute 'organisationId'",
      );
      expect(rows[0].querySelector('.condition-attribute-field .error')).toBeNull();
    });

    it('reports a value error with its original textarea line, counting dropped blank lines', async () => {
      service.get.mockReturnValue(of(flag({ conditions: targeted })));
      service.update.mockReturnValue(
        throwError(() => ({
          status: 400,
          error: {
            message: 'Validation failed',
            fieldErrors: { 'conditions[0].values[1]': 'must be at most 256 characters' },
          },
        })),
      );
      setup();
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;

      type(conditionRows(el)[0], 'textarea.condition-values', `acme\n\n${'x'.repeat(300)}`);
      await fixture.whenStable();
      click(el, 'button[type="submit"]');
      await fixture.whenStable();

      expect(service.update.mock.calls[0][1].conditions[0].values).toEqual(['acme', 'x'.repeat(300)]);
      expect(conditionRows(el)[0].querySelector('.condition-values-field .error')?.textContent).toContain(
        'Line 3: must be at most 256 characters',
      );
    });

    it('round-trips the existing Conditions plus edits on update', async () => {
      service.get.mockReturnValue(of(flag({ conditions: targeted })));
      setup();
      await fixture.whenStable();
      vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      const el: HTMLElement = fixture.nativeElement;

      click(el, 'button.add-condition');
      await fixture.whenStable();
      type(conditionRows(el)[2], 'input.condition-attribute', 'country');
      type(conditionRows(el)[2], 'textarea.condition-values', 'nl');
      await fixture.whenStable();

      click(el, 'button[type="submit"]');
      await fixture.whenStable();

      expect(service.update).toHaveBeenCalledWith('flag-1', {
        name: 'New Checkout',
        description: 'desc',
        enabled: true,
        conditions: [...targeted, { attribute: 'country', operator: 'IN', values: ['nl'] }],
      });
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
        conditions: [],
      });
    });
  });
});
