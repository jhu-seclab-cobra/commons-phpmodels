# PHP Models — Entry and Generator Design

The two entry forms a configuration file decodes into — the flat subject
model and the named model generator — and the constraint types a generator
matches with. The body, port, and subject types both forms share:
[design.md](design.md). Guards: [design-guards.md](design-guards.md).
Signatures: [design-declarations.md](design-declarations.md).

## Design Overview

- **Sealed hierarchies:** `ModelEntry` (top-level decoded entry:
  `SubjectModel`, `ModelGenerator`); `SubjectConstraint` (`NameConstraint`,
  `ClassConstraint`)
- **Data holders:** `SubjectModel`, `ModelGenerator`
- **Enums:** `SubjectKind` (FUNCTION, METHOD, VARIABLE)
- **Relationships:** `SubjectModel` contains one `ModelSubject`, at most one
  `WhenGuard`, at most one `SignatureInfo`, and one `ModelBody`;
  `ModelGenerator` contains one `SubjectKind`, a list of
  `SubjectConstraint`, and one `ModelBody`. All arrows one-way into the body
  and subject types.
- **Abstract:** the two sealed hierarchies — every subtype known at compile
  time, `when` exhaustive with no `else`.
- **Exceptions:** `IllegalArgumentException` from `init` blocks — a pattern
  that does not compile, a class constraint on a non-method find, a missing
  name constraint, a blank generator name, a variable subject declaring a
  non-source section, a kind-mismatched signature, or explicit `returns`
  beside a callable signature.
- **Dependency roles:** Data holders: `SubjectModel`, `ModelGenerator`, the
  constraint subtypes. Consumers: analyzers and compilers outside this
  library.

Package `edu.jhu.cobra.commons.phpmodels`. All types public — a consumer's
loader decodes entries, and a build-time compiler validates them; both live
outside this library.

The two forms are discriminated by deduction over their disjoint field sets —
`subject` marks a model, `name`/`find`/`where`/`model` mark a generator —
with no `type` tag. The generator syntax follows Mariana Trench model
generators (what to `find`, `where` constraints, the `model` to attach) with
Pysa's required query name. Jackson mechanism verified in
[impl.md](impl.md).

## Class / Type Specifications

### ModelEntry

**Responsibility:** One decoded configuration entry. Sealed supertype of the
two entry forms, so one file mixes models and generators and a loader routes
per entry. Carries no discriminator field — the forms are deduced from their
disjoint required fields, and an entry matching neither or both fails the
decode.

### SubjectModel

**Responsibility:** One explicit model: the subject it identifies and the
sectioned statement asserted for it. The subject is the entry's identity —
the form carries no name.

**State/Fields:** `val subject: ModelSubject`, `val guard: WhenGuard?`
(decoded from the optional `when:` field), `val signature: SignatureInfo?`
([design-declarations.md](design-declarations.md)), `val body: ModelBody`.
The five assertion sections decode flat beside `subject` — no wrapper key —
and compose into the body.

**Validation (`init`):**
- `subject` admits only the sections its kind allows: a non-callable subject
  rejects returns/propagation, guard, and a callable signature; a variable
  subject declares only sources.
- `signature` subtype matches the subject kind
  ([design-declarations.md](design-declarations.md)).
- Explicit `returns` beside a `CallableSignature` is rejected (one fact, one
  source); a propagation section beside a callable signature is completed
  into the value-semantics unit with the derived classification before body
  construction, so `ModelBody`'s propagation-requires-returns rule holds
  unchanged.
- At least one section present: a signature or a non-empty body — an entry
  asserting nothing is a load failure.
- Body-internal validation lives in `ModelBody` ([design.md](design.md)).

### SubjectKind

**Responsibility:** The closed set of subject kinds a generator can find.
A generator names the kind as data because it matches many subjects instead
of containing one. Generators find callable and variable subjects; the four
declaration-only kinds are matched explicitly, never by pattern.

### SubjectConstraint

**Responsibility:** One condition a found subject must satisfy. Sealed: the
`constraint` discriminator names the identity field, the subtype carries the
pattern compiled at construction.

**Subtypes:**
- `NameConstraint(pattern: String)` — over the subject's own name.
- `ClassConstraint(pattern: String)` — over a method subject's owning class.

**State:** each holds `val regex: Regex`, compiled in `init`; an invalid
pattern throws there, so no uncompiled pattern survives the load.

**Methods:** `fun matches(field: String): Boolean` — entire-field match
against the case-folded identity, never a substring match.

### ModelGenerator

**Responsibility:** One decoded generator entry: its unique name, the
subject kind to find, the constraints to satisfy, and the body to attach to
every satisfying subject. The only entry form with a `model:` wrapper — it
separates the selection fields from the assertion; the name is the identity
every generated declaration traces back to.

**State/Fields:** `val name: String`, `val find: SubjectKind`,
`val where: List<SubjectConstraint>`, `val model: ModelBody`.

**Validation (`init`):** `name` is non-blank; `where` contains at least one
`NameConstraint`; a `ClassConstraint` requires `find == METHOD`; the body
is non-empty (a generator attaches no signature, so an empty body asserts
nothing); `find == VARIABLE` ⟹ the body declares only sources. Name
uniqueness spans
one whole load across layers, so it is the loading consumer's check, not
this class's.

**Methods:**
- `fun matches(subject: ModelSubject): Boolean` — the subject is of the
  `find` kind and satisfies every constraint.

## Exception / Error Types

- `IllegalArgumentException` from the `init` blocks above. Raised while
  Jackson instantiates the entry, so a caller's load boundary reports it
  through the same malformed-entry path as every other decode failure.

## Validation Rules

- Both forms carry the same `ModelBody`: a body written flat in a model and
  a body written under a generator's `model:` are one type with one
  validation. No generator-specific body kind exists.
- The variable-subjects-only-sources rule is enforced in both forms — on the
  subject in `SubjectModel`, on the find kind in `ModelGenerator` — so no
  entry form is the loophole for the other.
- Patterns are matched with entire-field semantics (`Regex.matches`), never
  `containsMatchIn`: `get_.*` must not silently match `widget_getter`. This
  is the one deliberate departure from Mariana Trench, whose patterns match
  partially.
- Patterns are matched against case-folded identities; explicit subject
  spellings undergo the same folding, so both paths compare in one case.

Concept: [concept.md](concept.md). Domain semantics: [model.md](model.md).
