import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="topbar">
      <a routerLink="/flags" class="brand">
        <span class="dot"></span>Burgee
      </a>
      @if (auth.isAuthenticated()) {
        <div class="actions">
          <span class="user">{{ auth.username() }}</span>
          <button (click)="logout()">Sign out</button>
        </div>
      }
    </header>
    <main class="content">
      <router-outlet />
    </main>
  `,
  styles: [
    `
      .topbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.85rem 1.5rem;
        background: var(--surface);
        border-bottom: 1px solid var(--border);
      }
      .brand {
        font-weight: 700;
        font-size: 1.1rem;
        color: var(--text);
        display: inline-flex;
        align-items: center;
        gap: 0.55rem;
      }
      .brand:hover { text-decoration: none; }
      .dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: var(--accent);
        display: inline-block;
      }
      .actions { display: flex; align-items: center; gap: 0.75rem; }
      .user { color: var(--muted); font-size: 0.9rem; }
      .content { max-width: 960px; margin: 0 auto; padding: 1.75rem 1.5rem; }
    `,
  ],
})
export class AppComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.clear();
    void this.router.navigate(['/login']);
  }
}
