import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { FeatureFlag, FlagService } from '../core/flag.service';

@Component({
  selector: 'app-flags-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './flags-list.component.html',
  styleUrl: './flags-list.component.scss',
})
export class FlagsListComponent implements OnInit {
  private readonly service = inject(FlagService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly flags = signal<FeatureFlag[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly forbidden = signal(false);
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
      error: (err) => {
        if (err?.status === 403) {
          this.forbidden.set(true);
        } else {
          this.error.set('Failed to load flags.');
        }
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
