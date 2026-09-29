# Embedded H2 for in-memory storage

Burgee can run without Postgres (`BURGEE_STORAGE=memory`) for evaluation and local development, and this option ships in the production image. We back it with an embedded H2 database in PostgreSQL mode, not with hand-written in-memory adapters behind the repository ports. That way the existing JDBC adapters, Flyway migrations, UNIQUE constraints and transactions (flag changes and their audit entries commit or roll back together) behave the same in both modes.

## Considered Options

- **In-memory port adapters (`ConcurrentHashMap` per port):** rejected. We would have to rebuild key/subject uniqueness and ordering, use a no-op transaction manager with no rollback (so a flag and its audit entry could get out of sync), and maintain two implementations of every future port method.

## Consequences

- Every Flyway migration must run on both Postgres and H2's PostgreSQL mode. Postgres-only features (`jsonb`, partial indexes, `ON CONFLICT`, …) are off-limits unless this decision is revisited. An integration test that runs in CI enforces this.
- Setting `BURGEE_STORAGE=memory` together with an explicit `DB_URL` fails at startup, so a production config can never quietly fall back to memory. Burgee never switches to memory on its own.
- Data is ephemeral. There is deliberately no file-backed H2 mode, because it would become an unsupported second production database.
