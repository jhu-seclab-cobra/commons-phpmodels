# PHP Models — Declaration Types Design

Software structure of the declaration axis: the signature section types and
the declared-type vocabulary. Subject spellings and creators:
[design-subjects.md](design-subjects.md). Semantics:
[model-declarations.md](model-declarations.md).

## Design Overview

- **Sealed hierarchies:** `SignatureInfo` (`CallableSignature`,
  `ClassSignature`, `TypedSignature`, `PropertySignature`), `NamedExtras`
- **Classes:** `ParameterInfo`, `DeclaredType` (value class); handler types:
  [Handler Facts](design-declarations-handlers.md).
- **Enums:** `Classifier` (CLASS, INTERFACE, TRAIT, ENUM),
  `Visibility` (PUBLIC, PROTECTED, PRIVATE)
- **Relationships:** `ModelEntry` contains at most one `SignatureInfo`
  ([design-entries.md](design-entries.md)); `CallableSignature`
  contains `ParameterInfo`s; `ParameterInfo`, `TypedSignature`, and
  `PropertySignature` contain one `DeclaredType` each. All arrows one-way
  into the data types.
- **Exceptions:** `IllegalArgumentException` at decode — a malformed
  spelling or an unknown declared type ([design.md](design.md)).
- **Dependency roles:** Data holders: the signature types, `ParameterInfo`,
  `DeclaredType`. Validation: `init` blocks and the entry-level
  kind-matching rule in `ModelEntry`.

Package `edu.jhu.cobra.commons.phpmodels`. All types public — signatures are
part of the decoded entry surface consumers serve lookups from.

## Class / Type Specifications

### SignatureInfo (sealed)

**Responsibility:** The decoded signature section, one subtype per
describable kind, holding exactly the fields
[model-declarations.md](model-declarations.md) fixes for that kind.

- `CallableSignature` — `params: List<ParameterInfo>`,
  `returnType: DeclaredType` (function, method); `maxArguments` is the
  parameter count, unbounded with a variadic last parameter, and the
  entry-level argument-port bound reads it too; a function also states how
  php-src compiles a call of it to one opcode:
  - `framelessArities: Set<Int>`: the argument counts at which php-src
    selects a frameless function for a call of the function; each count is
    at most 3. Empty when the function has none.
  - `specialArities: Set<Int>`: the argument counts, each 2 or more, at
    which php-src always compiles a call bound when compiled to one opcode
    reading its arguments. Empty when none.
  - `ropeFormat: String?`: the parameter whose constant value php-src
    compiles into a rope with the arguments after it; null when none.

  **Validation (`init`):** each count lies within the required parameter
  count and `maxArguments`, each frameless count is at most 3, each special
  count is at least 2, and `ropeFormat` names a declared parameter that is
  neither by reference nor variadic; the entry rejects a method signature
  that declares any of the three. A violation raises
  `IllegalArgumentException`. The YAML keys are the field names; an absent
  key reads as empty or null.
- `ClassSignature` — `classifier: Classifier`, `parent: String?`,
  `interfaces: List<String>`, `isFinal: Boolean? = null`,
  `handlers: List<HandlerTable> = emptyList()`; a `data class` with a private
  constructor whose companion `invoke` factory — also the Jackson creator —
  folds the inheritance edges into the stored, compared form ([impl.md](impl.md)).
  A missing final status decodes as null; omitted handlers decode as empty.
  Table alternatives and their validation: [Handler Facts](design-declarations-handlers.md).
  **Validation (`init`):** `parent`, when present, and every interface name
  non-blank. A violation raises `IllegalArgumentException`.
- `TypedSignature` — `type: DeclaredType`, `value: String?` (constant,
  class constant; the spelled literal, null when the source states none)
- `PropertySignature` — `type: DeclaredType`, `visibility: Visibility`,
  `static: Boolean`

The subtype is deduced from the subject kind at the entry level — the
signature key carries no discriminator of its own; entry-level validation
rejects a signature subtype that does not match the entry's subject kind,
and rejects `returns:` beside a `CallableSignature` (one fact, one source;
[design-entries.md](design-entries.md)). It runs with the
section-admissibility validations, before any vocabulary interning by the
caller.

**Entry-level constraints of a declared `CallableSignature`** (enforced in
`ModelEntry`, [design-entries.md](design-entries.md)):

- Arity: every argument position the entry names — the condition's
  non-wildcard positions, both propagation sides, sink ports, explicit
  source sites, checked ports — lies inside the parameter list. Exception: a variadic last parameter admits every
  position. An entry without a signature is not arity-checked.
- Write direction: every written-into argument port — a propagation `to:`
  side, a source element's explicit site — names a by-reference parameter.
  A position beyond the parameter list is reachable only through a variadic
  tail and resolves to the variadic parameter's by-reference flag.
- A `void` return type declares there is no result: a propagation into
  `return` beside it is a load failure.

### ParameterInfo

**Responsibility:** One declared parameter: `name: String`,
`type: DeclaredType`, `optional: Boolean`, `byRef: Boolean`,
`variadic: Boolean`, `namedExtras: NamedExtras? = null`,
`default: String? = null`. Position is list order.
**Validation (`init`):** `name` non-blank; a non-null `namedExtras` requires
`variadic`; a non-null `default` requires `optional` and not `variadic`, and
must spell a PHP scalar literal.

`default` is the default the stub declares when it is a scalar literal:
`true`, `false` or `null`, a decimal integer or float, or a quoted string
without an escape. It is absent for a parameter without a default and for a
default of any other form (a constant, an expression, an array).
`defaultValue: IPrimitiveVal?` is the scalar `default` spells. The YAML form
is `default: 'false'`, the spelled literal as text.

A builtin variadic parameter carries `namedExtras: NamedExtras?`, one of REJECT(k) (named extras are refused at reception position k), FORWARD (they reach a target callable) or null (not extracted). The extractor reads the value from the builtin's argument parsing in php-src.

### NamedExtras

**Responsibility:** Sealed alternatives `Reject(position: Int)` and `Forward`
(data object). `position` is the zero-based start of the argument parsing
group that refuses extra names; it may precede the declared variadic tail.
**Validation (`init`):** `Reject.position` is non-negative.

The YAML forms are `namedExtras: {kind: reject, position: k}` and
`namedExtras: {kind: forward}`. An absent or null field decodes as null.
Unknown kinds, unknown keys and a missing rejection position fail decoding
through the existing strict mapper. No policy is inferred from variadicness.

### DeclaredType

**Responsibility:** `@JvmInline value class` over the declaration as
spelled (`raw`), validated against the declared-type grammar and its
composition rules ([model-declarations.md](model-declarations.md)). Lives
in its own file `DeclaredType.kt` with the grammar's parser, a private
function the validation and every derived member call.

- `val alternatives: List<Set<String>>` — the union read of the
  declaration: one set per alternative, holding the single types it
  intersects; a keyword lowercased, a class name folded and stripped of
  its leading namespace slash. `?T` reads as `[{t}, {null}]`.
- `fun toReturnKind(): ReturnKind` — the classification derivation: each
  alternative by the table in [model-declarations.md](model-declarations.md),
  joined with `ReturnKind.join`; it runs where the consumer materializes the
  value unit and is never written into a file that carries a
  signature.
- `val isVoid: Boolean` — true when the declaration is the `void` keyword.

Decodes from the bare scalar ([impl.md](impl.md)). The keyword-type set and
the standalone-only keywords (`mixed`, `void`, `never`) are constants in
this file, fixed by the PHP type system, not configuration. The relative
class types `self`, `static`, and `parent` match the class-name spelling
and derive to `ANY`.

### Classifier, Visibility

**Responsibility:** Closed enums for the class-like declaration kinds
and member visibility, decoded
case-insensitively from the lowercase file vocabulary ([impl.md](impl.md)).

## Exception / Error Types

- `IllegalArgumentException` — malformed spelling, unknown type name,
  kind-mismatched signature, `returns` beside a callable signature. Raised
  at decode through the shared load-boundary contract
  ([design.md](design.md)).

Semantics: [model-declarations.md](model-declarations.md). The entry form:
[design-entries.md](design-entries.md).
