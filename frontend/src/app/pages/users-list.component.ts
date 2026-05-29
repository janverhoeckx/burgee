import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { User, UserService } from '../core/user.service';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './users-list.component.html',
  styleUrl: './users-list.component.scss',
})
export class UsersListComponent implements OnInit {
  private readonly service = inject(UserService);
  private readonly router = inject(Router);

  protected readonly users = signal<User[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(new Set<string>());

  ngOnInit(): void {
    this.refresh();
  }

  private refresh(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load users.');
        this.loading.set(false);
      },
    });
  }

  edit(user: User): void {
    void this.router.navigate(['/users', user.id, 'edit']);
  }

  remove(user: User): void {
    if (!confirm(`Delete user "${user.subject}"? This cannot be undone.`)) return;
    this.markBusy(user.id, true);
    this.service.delete(user.id).subscribe({
      next: () => {
        this.users.update((list) => list.filter((u) => u.id !== user.id));
        this.markBusy(user.id, false);
      },
      error: () => {
        this.markBusy(user.id, false);
        this.error.set('Failed to delete user.');
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
