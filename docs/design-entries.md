# PHP Models — Entry Design

The one entry form a model document decodes into. The body, port, and
subject types it composes: [design.md](design.md),
[design-subjects.md](design-subjects.md). Conditions:
[design-conditions.md](design-conditions.md). Signatures:
[design-declarations.md](design-declarations.md).

## Design Overview

- **Classes:** `ModelEntry` (data class), `SectionFields` (internal decode
  holder)
- **Relationships:** `ModelEntry` contains one `ModelSubject`, at most one
  `ArgPattern`, at most one `SignatureInfo`, and one `ModelBody`. All
  arrows one-way into the body, condition, and subject types.
- **Exceptions:** `IllegalArgumentException` from `init` and the creator —
  a variable subject declaring a non-source section, a kind-mismatched
  signature, explicit `returns` beside a callable signature, a receiver
  port on a non-method subject, an explicit source site or a condition on
  a non-callable subject, a port or condition position beyond the
  signature's arity, an entry asserting nothing, a stray key.
- **Dependency roles:** Data holder: `ModelEntry`. Consumers: the set
  loader of this library and analyzers outside it.

Package `edu.jhu.cobra.commons.phpmodels`. `ModelEntry` is public — a
consumer reads entries out of a loaded set; `SectionFields` is internal.
There is one entry form: a document is a sequence of mappings, each
carrying `subject:` and its sections. No discriminator, no deduction.

## Class / Type Specifications

### ModelEntry

**Responsibility:** One model: the subject it identifies and the sectioned
statement asserted for it, under an optional condition. The subject is the
entry's identity — the form carries no name. Several entries for one
subject are alternatives; the set loader rejects two entries of one
document sharing subject and condition ([design-sets.md](design-sets.md)).

**State/Fields:** `val subject: ModelSubject`, `val condition: ArgPattern?`
(decoded from the optional `when:` field), `val signature: SignatureInfo?`,
`val body: ModelBody`. The five assertion sections decode flat beside
`subject` — no wrapper key — gathered into the creator through one
unwrapped parameter of `SectionFields`, which carries the raw sections
before the signature-derived returns completion and rejects any unknown
key ([impl.md](impl.md)); the creator completes them into the body.

**Validation (`init`):**
- `subject` admits only the sections its kind allows: a non-callable
  subject rejects returns, propagation, and the condition; a class subject
  declares nothing besides its signature; every other non-callable kind
  declares only sources.
- Port admissibility follows the subject kind: a receiver port anywhere in
  the body requires a method subject; a source element's explicit
  production site requires a callable subject.
- `signature` subtype matches the subject kind
  ([design-declarations.md](design-declarations.md)).
- A declared `CallableSignature` constrains every argument position the
  entry names — propagation sides, sink ports, explicit source sites, and
  the condition's non-wildcard positions — to the parameter list, with the
  write-direction and `void` rules of
  [design-declarations.md](design-declarations.md).
- Explicit `returns` beside a `CallableSignature` is rejected (one fact, one
  source); a propagation section beside a callable signature is completed
  into the value-semantics unit with the derived classification before body
  construction, so `ModelBody`'s propagation-requires-returns rule holds.
- At least one section present: a signature or a non-empty body — an entry
  asserting nothing is a load failure.
- Body-internal validation lives in `ModelBody` ([design.md](design.md)).

**Decoding:** the companion creator takes `subject`, the optional `when`,
the raw `signature` node, and the unwrapped sections; it narrows the
signature by the subject kind (the mapping carries no discriminator) and
derives `returns` from a callable signature when a propagation section is
present ([impl.md](impl.md)).

### SectionFields (internal)

**Responsibility:** The five section fields as they decode flat beside
`subject`: `returns`, `propagation`, `sources`, `sinks`, `sanitizers`, all
optional. `fun complete(returns: ReturnKind?): ModelBody` builds the body
after the creator's returns completion. A throwing any-setter restores the
strict unknown-key failure the unwrapped decode path would otherwise
absorb ([impl.md](impl.md)).

## Exception / Error Types

- `IllegalArgumentException` — every failure listed above, raised while
  Jackson instantiates, surfacing at the caller's load boundary through
  the shared contract ([design.md](design.md)).

Domain semantics: [model.md](model.md),
[model-conditions.md](model-conditions.md).
