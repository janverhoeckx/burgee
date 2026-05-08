import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { apiBaseUrl } from '../core/api.config';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="card">
      <h1>Sign in</h1>
      <p class="muted">Use your Burgee admin credentials.</p>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="field">
          <label for="username">Username</label>
          <input id="username" type="text" formControlName="username" autocomplete="username" />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" />
        </div>
        @if (error()) { <div class="error">{{ error() }}</div> }
        <button class="primary" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>
    </div>
  `,
  styles: [
    `
      .card {
        max-width: 360px;
        margin: 4rem auto 0;
        background: var(--surface);
        border: 1px solid var(--border);
        border-radius: var(--radius);
        padding: 1.75rem;
      }
      h1 { margin: 0 0 0.4rem; font-size: 1.4rem; }
      .muted { color: var(--muted); margin: 0 0 1.25rem; font-size: 0.9rem; }
      button { width: 100%; margin-top: 0.5rem; }
    `,
  ],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) return;
    const { username, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);

    const probe = `${apiBaseUrl()}/api/admin/flags`;
    const headers = { Authorization: 'Basic ' + btoa(`${username}:${password}`) };

    this.http.get(probe, { headers }).subscribe({
      next: () => {
        this.auth.store(username, password);
        this.loading.set(false);
        void this.router.navigate(['/flags']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.status === 401 ? 'Invalid credentials' : 'Sign-in failed');
      },
    });
  }
}
