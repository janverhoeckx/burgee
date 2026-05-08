import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FeatureFlag, FlagService } from '../core/flag.service';

@Component({
  selector: 'app-flags-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="header">
      <div>
        <h1>Feature flags</h1>
        <p class="muted">{{ flags().length }} flag{{ flags().length === 1 ? '' : 's' }}</p>
      </div>
      <button class="primary" routerLink="/flags/new">New flag</button>
    </div>

    @if (loading()) {
      <p class="muted">Loading…</p>
    } @else if (error()) {
      <div class="error">{{ error() }}</div>
    } @else if (flags().length === 0) {
      <div class="empty">
        <p>No flags yet.</p>
        <button class="primary" routerLink="/flags/new">Create your first flag</button>
      </div>
    } @else {
      <table class="flags">
        <thead>
          <tr>
            <th>Key</th>
            <th>Name</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (flag of flags(); track flag.id) {
            <tr>
              <td><code>{{ flag.key }}</code></td>
              <td>
                <div>{{ flag.name }}</div>
                @if (flag.description) { <div class="muted small">{{ flag.description }}</div> }
              </td>
              <td>
                <input
                  type="checkbox"
                  class="toggle"
                  [checked]="flag.enabled"
                  [disabled]="busy().has(flag.id)"
                  (change)="toggle(flag)"
                />
              </td>
              <td class="actions">
                <button (click)="edit(flag)">Edit</button>
                <button class="danger" (click)="remove(flag)">Delete</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: [
    `
      .header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1.5rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      .muted { color: var(--muted); }
      .small { font-size: 0.85rem; margin-top: 0.2rem; }
      .empty { text-align: center; padding: 3rem 1rem; background: var(--surface); border: 1px dashed var(--border); border-radius: var(--radius); }
      table.flags { width: 100%; border-collapse: collapse; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
      th, td { text-align: left; padding: 0.75rem 1rem; border-bottom: 1px solid var(--border); }
      th { color: var(--muted); font-weight: 500; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.04em; background: var(--surface-2); }
      tr:last-child td { border-bottom: none; }
      code { background: var(--surface-2); padding: 0.15rem 0.45rem; border-radius: 4px; font-size: 0.85rem; }
      td.actions { white-space: nowrap; display: flex; gap: 0.5rem; justify-content: flex-end; }
    `,
  ],
})
export class FlagsListComponent implements OnInit {
  private readonly service = inject(FlagService);
  private readonly router = inject(Router);

  protected readonly flags = signal<FeatureFlag[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(new Set<string>());

  ngOnInit(): void {
    this.refresh();
  }

  private refresh(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (flags) => {
        this.flags.set(flags);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load flags.');
        this.loading.set(false);
      },
    });
  }

  toggle(flag: FeatureFlag): void {
    this.markBusy(flag.id, true);
    this.service.toggle(flag.id).subscribe({
      next: (updated) => {
        this.flags.update((list) => list.map((f) => (f.id === updated.id ? updated : f)));
        this.markBusy(flag.id, false);
      },
      error: () => {
        this.markBusy(flag.id, false);
        this.error.set('Failed to toggle flag.');
      },
    });
  }

  edit(flag: FeatureFlag): void {
    void this.router.navigate(['/flags', flag.id, 'edit']);
  }

  remove(flag: FeatureFlag): void {
    if (!confirm(`Delete flag "${flag.key}"? This cannot be undone.`)) return;
    this.markBusy(flag.id, true);
    this.service.delete(flag.id).subscribe({
      next: () => {
        this.flags.update((list) => list.filter((f) => f.id !== flag.id));
        this.markBusy(flag.id, false);
      },
      error: () => {
        this.markBusy(flag.id, false);
        this.error.set('Failed to delete flag.');
      },
    });
  }

  private markBusy(id: string, busy: boolean): void {
    this.busy.update((set) => {
      const next = new Set(set);
      if (busy) next.add(id);
      else next.delete(id);
      return next;
    });
  }
}
