# PHP Models — Guard Design

The types carrying the optional `when:` guard. Guard semantics:
[model-guards.md](model-guards.md). Entry forms:
[design-generators.md](design-generators.md).

## Design Overview

- **Classes:** `WhenGuard`
- **Sealed hierarchies:** `GuardValue` (`BoolValue`, `IntValue`, `StrValue`)
- **Relationships:** `SubjectModel` contains at most one `WhenGuard`;
  `WhenGuard` contains one `Port.Argument` and one `GuardValue`. All arrows
  one-way into the guard types.
- **Exceptions:** `IllegalArgumentException` from decode — a return or
  malformed guard port, a non-scalar compared value.
- **Dependency roles:** Data holders: `WhenGuard`, the `GuardValue`
  subtypes. Consumers: branch selection in the consuming analyzer.

Package `edu.jhu.cobra.commons.phpmodels`. Public — a guard is part of the
decoded entry a consumer's branch table keys on.

Guard evaluation — verdicts, candidate sets, the guard-context contract a
consumer implements — is consumer structure, not this library's. This
library fixes only what a guard *is*.

## Class / Type Specifications

### WhenGuard

**Responsibility:** One decoded `when:` condition — the argument port tested
and the scalar it must equal. Value identity: two entries with the same port
and value address the same branch, so equality and hash cover both fields.

**State/Fields:** `val port: Port.Argument`, `val value: GuardValue` —
decoded from the YAML fields `when.port` and `when.is`; the scalar decodes
to exactly one of boolean, integer, or string ([impl.md](impl.md) for the
Jackson shape; `is` is a Kotlin keyword, so the config key binds through an
annotated name).

**Validation (decode):** the port is an argument port — a return port fails
decode; a non-scalar `is` value fails decode. The guard field is optional
on `SubjectModel` and absent from `ModelGenerator`: the strict decode
rejects a `when:` key on a generator as an unknown field. A guard beside a
non-callable subject fails in `SubjectModel` validation, the same place the
section-admissibility rules live
([design-generators.md](design-generators.md)).

### GuardValue (sealed)

**Responsibility:** The compared scalar, closed over the three shapes a
guard admits. Owned by this library so the format depends on no external
value-lattice type; a consumer maps a `GuardValue` into its own value
domain at its boundary.

**Subtypes:** `BoolValue(value: Boolean)`, `IntValue(value: Long)`,
`StrValue(value: String)` — value classes with equality by content.

**Decoding:** the `is` field arrives as a raw tree node and narrows by shape
(boolean / integral / textual); any other shape throws in the creator, and an
integral outside `Long` range fails decode instead of wrapping
([impl.md](impl.md)).

## Exception / Error Types

- `IllegalArgumentException` — guard validation failures listed above,
  raised while Jackson instantiates, surfacing at the caller's load boundary
  with every other malformed-entry failure.

Domain semantics: [model-guards.md](model-guards.md).
