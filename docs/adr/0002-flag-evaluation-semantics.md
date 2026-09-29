# Public flag reads are always Evaluations

Flags can carry a Targeting Rule, so a flag's stored `enabled` value no longer tells a client whether the flag is on *for them*. We therefore removed the plain public GETs (`/api/v1/flags` and `/api/v1/flags/{key}`) and replaced them with `POST /api/v1/flags/{key}/evaluate` and `POST /api/v1/flags/evaluate`, which take an Evaluation Context. An Evaluation is `enabled AND every Condition matches`: `enabled` acts as a kill switch, all Conditions are ANDed, and a Condition whose Attribute is missing from the context fails closed (false, not an error). Breaking the GETs was acceptable because Burgee had no production users yet.

## Considered Options

- **Keep the GETs and return the raw `enabled`.** Rejected: a targeted flag would report true for callers who don't match.
- **Keep the GETs and evaluate them with an empty context.** Rejected in favour of a single, explicit way to read flags.
- **Rules override `enabled`** (a match returns true even when the flag is disabled). Rejected: admins lose the instant off switch for targeted flags.
- **Return 400 when an attribute is missing.** Rejected: callers would need to know which Attributes each flag uses.

## Consequences

- The public evaluate endpoints only ever return the result, never the Conditions. Anyone can still probe whether a value is in a list, so value lists must not contain secrets.
- In the public API, the response field `enabled` holds the Evaluation result, not the master switch.
