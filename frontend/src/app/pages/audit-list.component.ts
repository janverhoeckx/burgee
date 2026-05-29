import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuditEntry, AuditService } from '../core/audit.service';

@Component({
  selector: 'app-audit-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './audit-list.component.html',
  styleUrl: './audit-list.component.scss',
})
export class AuditListComponent implements OnInit {
  private readonly service = inject(AuditService);
  private readonly route = inject(ActivatedRoute);

  protected readonly entries = signal<AuditEntry[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly flagId = signal<string | null>(null);
  protected readonly search = signal('');

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) return this.entries();
    return this.entries().filter((e) =>
      [e.flagKey, e.actor, e.action, e.detail].some((field) =>
        field?.toLowerCase().includes(term),
      ),
    );
  });

  ngOnInit(): void {
    const flagId = this.route.snapshot.queryParamMap.get('flagId');
    this.flagId.set(flagId);
    this.service.list(flagId ?? undefined).subscribe({
      next: (entries) => {
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load audit trail.');
        this.loading.set(false);
      },
    });
  }

  protected onSearch(value: string): void {
    this.search.set(value);
  }
}
