import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FlagService } from '../core/flag.service';

@Component({
  selector: 'app-flag-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <a routerLink="/flags" class="back">&larr; Back to flags</a>
    <div class="card">
      <h1>{{ id() ? 'Edit flag' : 'New flag' }}</h1>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="field">
          <label for="key">Key</label>
          <input
            id="key"
            type="text"
            formControlName="key"
            placeholder="my-feature"
            [readonly]="!!id()"
          />
          @if (form.controls.key.touched && form.controls.key.errors?.['pattern']) {
            <div class="error">Lowercase letters, digits, and . _ - only.</div>
          }
        </div>

        <div class="field">
          <label for="name">Name</label>
          <input id="name" type="text" formControlName="name" placeholder="My new feature" />
        </div>

        <div class="field">
          <label for="description">Description</label>
          <textarea id="description" rows="3" formControlName="description"></textarea>
        </div>

        <div class="field row">
          <input id="enabled" type="checkbox" class="toggle" formControlName="enabled" />
          <label for="enabled" class="inline">Enabled</label>
        </div>

        @if (error()) { <div class="error">{{ error() }}</div> }

        <div class="actions">
          <button type="button" routerLink="/flags">Cancel</button>
          <button class="primary" type="submit" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [
    `
      .back { color: var(--muted); font-size: 0.9rem; display: inline-block; margin-bottom: 1rem; }
      .card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 1.75rem; }
      h1 { margin: 0 0 1.25rem; font-size: 1.4rem; }
      input[readonly] { color: var(--muted); }
      .row { display: flex; align-items: center; gap: 0.7rem; }
      .row label.inline { margin: 0; color: var(--text); font-size: 0.95rem; }
      .actions { display: flex; gap: 0.6rem; justify-content: flex-end; margin-top: 1.25rem; }
    `,
  ],
})
export class FlagFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(FlagService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly id = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    key: ['', [Validators.required, Validators.pattern(/^[a-z0-9][a-z0-9._-]*$/)]],
    name: ['', Validators.required],
    description: [''],
    enabled: [false],
  });

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

    const id = this.id();
    const obs = id
      ? this.service.update(id, {
          name: value.name,
          description: value.description || null,
          enabled: value.enabled,
        })
      : this.service.create({
          key: value.key,
          name: value.name,
          description: value.description || null,
          enabled: value.enabled,
        });

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/flags']);
      },
      error: (err) => {
        this.saving.set(false);
        if (err?.status === 409) this.error.set('A flag with this key already exists.');
        else if (err?.status === 400) this.error.set('Validation failed. Check the fields.');
        else this.error.set('Save failed.');
      },
    });
  }
}
