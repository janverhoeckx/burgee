import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { IdentityProvider, Role, UserService } from '../core/user.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.component.html',
  styleUrl: './user-form.component.scss',
})
export class UserFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly id = signal<string | null>(null);
  protected readonly provider = signal<IdentityProvider>(
    this.auth.method().toUpperCase() as IdentityProvider,
  );

  /** A password only applies to basic-auth users that sign in with a username. */
  protected readonly usesPassword = computed(() => this.provider() === 'BASIC');

  protected readonly form = this.fb.nonNullable.group({
    subject: ['', Validators.required],
    email: ['', Validators.email],
    displayName: [''],
    role: ['USER' as Role, Validators.required],
    password: [''],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id.set(idParam);
      this.service.get(idParam).subscribe({
        next: (user) => {
          this.provider.set(user.provider);
          this.form.patchValue({
            subject: user.subject,
            email: user.email ?? '',
            displayName: user.displayName ?? '',
            role: user.role,
          });
          this.form.controls.subject.disable();
        },
        error: () => this.error.set('Failed to load user.'),
      });
    } else if (this.usesPassword()) {
      this.form.controls.password.addValidators(Validators.required);
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
          email: value.email || null,
          displayName: value.displayName || null,
          role: value.role,
          password: value.password || null,
        })
      : this.service.create({
          subject: value.subject,
          email: value.email || null,
          displayName: value.displayName || null,
          role: value.role,
          password: value.password || null,
        });

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/users']);
      },
      error: (err) => {
        this.saving.set(false);
        if (err?.status === 409) this.error.set('A user with this identity already exists.');
        else if (err?.status === 400) this.error.set('Validation failed. Check the fields.');
        else this.error.set('Save failed.');
      },
    });
  }
}
