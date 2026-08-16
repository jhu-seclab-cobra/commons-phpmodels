# PHP Models — Declaration Types Design

Software structure of the declaration axis: the signature section types and
the declared-type vocabulary. Subject spellings and creators:
[design.md](design.md). Semantics:
[model-declarations.md](model-declarations.md).

## Design Overview

- **Sealed hierarchies:** `SignatureInfo` (`CallableSignature`,
  `ClassSignature`, `TypedSignature`, `PropertySignature`)
- **Classes:** `ParameterInfo`, `DeclaredType` (value class)
- **Enums:** `Classifier` (CLASS, INTERFACE, TRAIT, ENUM), `Visibility`
  (PUBLIC, PROTECTED, PRIVATE)
- **Relationships:** `SubjectModel` contains at most one `SignatureInfo`
  ([design-generators.md](design-generators.md)); `CallableSignature`
  contains `ParameterInfo`s; `ParameterInfo`, `TypedSignature`, and
  `PropertySignature` contain one `DeclaredType` each. All arrows one-way
  into the data types.
- **Exceptions:** `IllegalArgumentException` at decode — a malformed
  spelling or an unknown declared type ([design.md](design.md)).
- **Dependency roles:** Data holders: the signature types, `ParameterInfo`,
  `DeclaredType`. Validation: `init` blocks and the entry-level
  kind-matching rule in `SubjectModel`.

Package `edu.jhu.cobra.commons.phpmodels`. All types public — signatures are
part of the decoded entry surface consumers serve lookups from.

## Class / Type Specifications

### SignatureInfo (sealed)

**Responsibility:** The decoded signature section, one subtype per
describable kind, holding exactly the fields
[model-declarations.md](model-declarations.md) fixes for that kind.

- `CallableSignature` — `params: List<ParameterInfo>`,
  `returnType: DeclaredType` (function, method)
- `ClassSignature` — `classifier: Classifier`, `parent: String?`,
  `interfaces: List<String>`; a `data class` with a private constructor
  whose companion `invoke` factory — also the Jackson creator — folds the
  inheritance edges into the stored, compared form ([impl.md](impl.md))
- `TypedSignature` — `type: DeclaredType`, `value: String?` (constant,
  class constant; the spelled literal, null when the source states none)
- `PropertySignature` — `type: DeclaredType`, `visibility: Visibility`,
  `static: Boolean`

The subtype is deduced from the subject kind at the entry level — the
signature key carries no discriminator of its own; entry-level validation
rejects a signature subtype that does not match the entry's subject kind,
and rejects `returns:` beside a `CallableSignature` (one fact, one source;
[design-generators.md](design-generators.md)).

### ParameterInfo

**Responsibility:** One declared parameter: `name: String`,
`type: DeclaredType`, `optional: Boolean`, `byRef: Boolean`,
`variadic: Boolean`. A pure data holder; position is list order.

### DeclaredType

**Responsibility:** `@JvmInline value class` over the declared type name,
validated against the closed type vocabulary plus class names
([model-declarations.md](model-declarations.md)); owns
`fun toReturnKind(): ReturnKind` — the classification derivation (string →
STR; int, float → NUM; bool → BOOL; else ANY). Decodes from the bare scalar
([impl.md](impl.md)).

The closed keyword-type set is a constant in this file — fixed by the PHP
type system, not configuration.

### Classifier, Visibility

**Responsibility:** Closed enums for the class-like declaration kinds and
member visibility, decoded case-insensitively from the lowercase file
vocabulary ([impl.md](impl.md)).

## Exception / Error Types

- `IllegalArgumentException` — malformed spelling, unknown type name,
  kind-mismatched signature, `returns` beside a callable signature. Raised
  at decode through the shared load-boundary contract
  ([design.md](design.md)).

## Validation Rules

- The `::` splitter in the subject creators ([design-subjects.md](design-subjects.md)) is the
  single authority for member spellings; no other code inspects subject
  strings.
- Case folding happens inside the subject creators, once; every consumer
  downstream compares folded identities for folded kinds and exact
  identities for sensitive kinds.
- Signature validation is entry-level (subject kind ⟺ signature subtype),
  running with the section-admissibility validations before any vocabulary
  interning by the caller.
- A declared callable signature fixes the arity: every argument port the
  entry names — the guard port, both propagation sides, sink ports — lies
  inside the parameter list. Exception: a variadic last parameter admits
  every position. An entry without a signature is not arity-checked.
- Derivation, not storage: `DeclaredType.toReturnKind()` runs where the
  consumer materializes the value-semantics unit; the classification is
  never written into a file that carries a signature.

Semantics: [model-declarations.md](model-declarations.md). Entry forms:
[design-generators.md](design-generators.md).
