# Burgee

A self-hostable feature flag service: administrators manage feature flags, and client applications read them.

## Language

### Running Burgee

**Storage mode**:
Where Burgee keeps flags, users and audit entries: **Postgres** (durable) or **In-memory** (everything is lost on restart, meant for evaluation and local development).
_Avoid_: H2 mode, demo mode, dev mode, database mode
