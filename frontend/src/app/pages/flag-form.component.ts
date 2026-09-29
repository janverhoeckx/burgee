import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Condition, ConditionOperator, FlagService } from '../core/flag.service';
import { ConditionRowErrors, parseConditionFieldErrors } from '../core/targeting-rule-errors';

/** One editor row: an Attribute name and its values, one per line. The operator is always IN. */
type ConditionRow = FormGroup<{
  attribute: FormControl<string>;
  values: FormControl<string>;
}>;

@Component({
  selector: 'app-flag-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './flag-form.component.html',
  styleUrl: './flag-form.component.scss',
})
export class FlagFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(FlagService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly id = signal<string | null>(null);
  /** Backend field errors per Condition row index. Cleared when rows shift or on a new save. */
  protected readonly conditionErrors = signal<Record<number, ConditionRowErrors>>({});
  /** Backend field errors that don't belong to a Condition row. */
  protected readonly fieldErrors = signal<string[]>([]);
  /** The operator every editor row uses; shown read-only next to the Attribute. */
  protected readonly operator = ConditionOperator.In;

  protected readonly form = this.fb.nonNullable.group({
    key: ['', [Validators.required, Validators.pattern(/^[a-z0-9][a-z0-9._-]*$/)]],
    name: ['', Validators.required],
    description: [''],
    enabled: [false],
    conditions: this.fb.nonNullable.array<ConditionRow>([]),
  });

  protected get conditions(): FormArray<ConditionRow> {
    return this.form.controls.conditions;
  }

  addCondition(): void {
    this.conditions.push(this.conditionRow());
  }

  removeCondition(index: number): void {
    this.conditions.removeAt(index);
    this.conditionErrors.set({});
  }

  private showFieldErrors(fieldErrors: Record<string, string | null>): void {
    const { conditions, general } = parseConditionFieldErrors(fieldErrors);
    this.conditionErrors.set(conditions);
    this.fieldErrors.set(general);
  }

  /** Turns editor rows into Conditions: values one per line, trimmed, empty lines dropped. */
  private parseConditions(): Condition[] {
    return this.conditions.getRawValue().map((row) => ({
      attribute: row.attribute.trim(),
      operator: ConditionOperator.In,
      values: row.values
        .split('\n')
        .map((v) => v.trim())
        .filter((v) => v.length > 0),
    }));
  }

  private conditionRow(attribute = '', values = ''): ConditionRow {
    return this.fb.nonNullable.group({ attribute: [attribute], values: [values] });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id.set(idParam);
      this.service.get(idParam).subscribe({
        next: (flag) => {
          this.form.patchValue({
            key: flag.key,
            name: flag.name,
            description: flag.description ?? '',
            enabled: flag.enabled,
          });
          this.conditions.clear();
          for (const c of flag.conditions) {
            this.conditions.push(this.conditionRow(c.attribute, c.values.join('\n')));
          }
          this.form.controls.key.disable();
        },
        error: () => this.error.set('Failed to load flag.'),
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.conditionErrors.set({});
    this.fieldErrors.set([]);

    const conditions = this.parseConditions();
    const id = this.id();
    const obs = id
      ? this.service.update(id, {
          name: value.name,
          description: value.description || null,
          enabled: value.enabled,
          conditions,
        })
      : this.service.create({
          key: value.key,
          name: value.name,
          description: value.description || null,
          enabled: value.enabled,
          conditions,
        });

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/flags']);
      },
      error: (err) => {
        this.saving.set(false);
        if (err?.status === 409) this.error.set('A flag with this key already exists.');
        else if (err?.status === 400) {
          this.error.set('Validation failed. Check the fields.');
          this.showFieldErrors(err.error?.fieldErrors ?? {});
        }
        else this.error.set('Save failed.');
      },
    });
  }
}
