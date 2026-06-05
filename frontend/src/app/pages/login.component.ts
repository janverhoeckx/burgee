import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { apiBaseUrl } from '../core/api.config';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      void this.router.navigate(['/flags']);
    }
  }

  async signIn(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.auth.signIn();
    } catch (e) {
      console.error('OIDC sign-in failed', e);
      this.error.set('Sign-in failed');
      this.loading.set(false);
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    const { username, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);

    const probe = `${apiBaseUrl()}/api/admin/flags`;
    const headers = { Authorization: 'Basic ' + btoa(`${username}:${password}`) };

    this.http.get(probe, { headers }).subscribe({
      next: () => {
        this.auth.storeBasic(username, password);
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
