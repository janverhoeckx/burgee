# Burgee

Burgee is a self-hostable feature flag service: admins manage flags, client applications ask whether a flag is on for them.

## Language

### Running Burgee

**Storage mode**:
Where Burgee keeps flags, users and audit entries: **Postgres** (durable) or **In-memory** (everything is lost on restart, meant for evaluation and local development).
_Avoid_: H2 mode, demo mode, dev mode, database mode

### Flags and evaluation

**Feature Flag**:
A named switch, identified by a unique key, that client applications query to decide whether a feature is on.
_Avoid_: toggle, feature switch

**Enabled**:
The flag's master switch. A disabled flag always evaluates to false, whatever its Targeting Rule says.
_Avoid_: active, on

**Evaluation Context**:
The set of Attributes a client submits when asking for a flag's value. It may be empty.
_Avoid_: properties, payload, request data

**Attribute**:
A single named string value in an Evaluation Context, e.g. `organisationId = acme`. Matched exactly and case-sensitively.
_Avoid_: property, parameter, claim

**Targeting Rule**:
The set of Conditions on a flag that decides, for an enabled flag, which Evaluation Contexts get true. All Conditions must match. An enabled flag with no Conditions is true for everyone.
_Avoid_: filter, segment, properties

**Condition**:
One requirement in a Targeting Rule: an Attribute name, an operator (currently only `IN`), and a list of values. A Condition whose Attribute is missing from the Evaluation Context does not match.

**Evaluation**:
Computing a flag's true/false result for one Evaluation Context: `enabled AND every Condition matches`.
_Avoid_: check, resolve

## Relationships

- A **Feature Flag** has exactly one **Targeting Rule**, made of zero or more **Conditions**
- An **Evaluation** combines one **Feature Flag** with one **Evaluation Context**

## Example dialogue

> **Dev:** "The client sent `organisationId=acme`, but the flag came back false."
> **Domain expert:** "Is the flag **Enabled**? If it is, check its **Targeting Rule**: acme has to be in the **Condition**'s value list, and every other **Condition** has to match too."

## Flagged ambiguities

- "enabled" means the master switch on a **Feature Flag**, but the public evaluate response also uses an `enabled` field for the **Evaluation** result. Resolved: in the public API, `enabled` is always the Evaluation result. Everywhere else it is the master switch.
- "properties" was used both for what the client sends and for what the flag checks. Resolved: the client sends an **Evaluation Context** of **Attributes**, and the flag holds a **Targeting Rule** of **Conditions**.
