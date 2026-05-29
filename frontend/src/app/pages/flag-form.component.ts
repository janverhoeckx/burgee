import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FlagService } from '../core/flag.service';

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
