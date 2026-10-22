# PHP Models — Body Design

The sectioned model body and its element types. Vocabulary, policy, and the
decoder: [design.md](design.md). Ports and propagations:
[design-subjects.md](design-subjects.md). The entry that carries a body:
[design-entries.md](design-entries.md).

## Design Overview

- **Classes:** `ModelBody`, `ValueUnit`, `CharacterBound`,
  `CharacterClass`, `SourceDecl`, `KeyPattern`, `SinkDecl`, `SanitizerDecl`,
  `ArgumentCheck` (data classes)
- **Enums:** `ReturnKind` (STR, NUM, BOOL, ANY)
- **Relationships:** `ModelBody` contains the optional section values;
  `CharacterBound` contains optional `Port.Argument`s and an optional
  `CharacterClass`; `SinkDecl` contains one `Port.Argument` and one
  `VulnTypeId`; `SanitizerDecl` contains `VulnTypeId`s and an optional
  `ArgumentCheck`, which contains one `Port.Argument`, one `IPrimitiveVal`,
  an optional `CharacterClass` and an optional `DeclaredType`;
  `SourceDecl` contains an optional `Port.Argument` site
  override and optional `KeyPattern`s; `ValueUnit` is built from a
  `ModelBody`. All arrows one-way into the data types.
- **Exceptions:** `IllegalArgumentException` from `init` blocks and
  creators ([design.md](design.md) Exception / Error Types).
- **Dependency roles:** Data holders: every type here. Decoded by
  `ModelYaml` ([design.md](design.md)).

Package `edu.jhu.cobra.commons.phpmodels`; every type is public.

## Class / Type Specifications

### SourceDecl

**Responsibility:** One element of the sources section: a non-empty
produced source kind set, an optional explicit production site, and optional key
patterns restricting production to matching array keys.

**State/Fields:** `sourceKinds: Set<SourceKindId>`, `at: Port.Argument?`
(null = the kind-fixed default site, [model.md](model.md)),
`keys: List<KeyPattern>?`.

**Validation (`init`):** `sourceKinds` non-empty; a declared `keys` list
non-empty. Whether the subject admits an explicit site is entry-level
validation ([design-entries.md](design-entries.md)).

### KeyPattern

**Responsibility:** One key pattern of a source element: the declared
pattern with its regex, compiled at construction so no uncompiled pattern
survives the load; equality over the pattern text (`Regex` carries no value
equality). Decoded from the bare pattern scalar.

**Methods:** `fun matches(key: String): Boolean` — entire-key,
case-sensitive match, never a substring match.

### SinkDecl, SanitizerDecl

**Responsibility:** One element of the sinks / sanitizers sections:
`SinkDecl(port: Port.Argument, vulnType: VulnTypeId)` — one dangerously
consumed argument port under one vulnerability type;
`SanitizerDecl(vulnTypes: Set<VulnTypeId> = emptySet(), check:
ArgumentCheck? = null)` — the neutralized vulnerability type set of the
call result, or, with a check, of the argument the call accepts (agent
decision CK-1, [model.md](model.md)).

**Validation (`init`):** without a check, `vulnTypes` non-empty; with a
check, `vulnTypes`, `check.within`, or `check.types` declared.

### ArgumentCheck

**Responsibility:** The check of one sanitizers element: `argument:
Port.Argument` — the checked port; `accepts: IPrimitiveVal` — the result
value that accepts the argument; `within: CharacterClass?` — the class
every byte of the accepted argument belongs to; `types: DeclaredType?` —
the type set the call accepts exactly. YAML: `check: {argument:
argument(0), accepts: true, within: "[0-9]"}`.

**Validation (creator, `init`):** `accepts` present and a scalar, read
as a condition scalar is ([impl.md](impl.md)); `types` is not `void`.
That `argument` lies inside the signature is entry-level validation
([design-entries.md](design-entries.md)).

### CharacterBound

**Responsibility:** The characters section: `of: Set<Port.Argument>?` — the
ports whose bytes the result copies; `within: CharacterClass?` — the class
every result byte belongs to. A null field states no bound of its kind.

**Validation (`init`):** at least one field declared; a declared `of` set
non-empty. That each `of` port flows into the result is body-level
validation (ModelBody).

### CharacterClass

**Responsibility:** One declared class of bytes: the pattern text with a
membership flag per byte value, evaluated at construction so no uncompiled
pattern survives the load; equality over the pattern text, as for
`KeyPattern`. Decoded from the bare pattern scalar.

**Validation (`init`):** the pattern compiles and matches at least one
byte value; a class of no byte would state an always-empty result.

**Methods:** `fun contains(byte: Int): Boolean` — the pattern matches the
one-character Latin-1 string of the byte value; a value outside 0..255 is
misuse (`IllegalArgumentException`).

### ModelBody

**Responsibility:** The sectioned statement of one model: seven optional
assertion sections. An all-absent body is constructible — a signature-only
entry has one — and the at-least-one-section rule therefore lives at the
entry level, where the signature is visible: `ModelEntry` requires a
signature or a non-empty body ([design-entries.md](design-entries.md)).

**State/Fields:** `returns: ReturnKind?`, `propagation: List<Propagation>?`,
`characters: CharacterBound?`, `sources: List<SourceDecl>?`, `sinks: List<SinkDecl>?`,
`sanitizers: List<SanitizerDecl>?`, `throws: Set<ClassSubject>?` (each
element decoded by the `ClassSubject` creator; empty = throws none).

**Validation (`init`):** propagation and characters each require returns
(the value unit is asserted whole or not at all — the
signature-derived completion happens in the entry, before construction);
each `characters.of` port is the from-port of a declared flow into
`Port.Return`; a declared section other than `throws` is non-empty.

**Methods:** `isEmpty: Boolean`; `declaresOnlySources: Boolean`;
`namesReceiverPort: Boolean` — a propagation side names `Port.Receiver`;
`declaresExplicitSourceSite: Boolean` — a source element declares `at`
(the port-admissibility predicates `ModelEntry` reads);
`fun valueUnit(): ValueUnit?` — null when returns is undeclared.

### ValueUnit

**Responsibility:** The value unit a lookup serves: the set of returns
classifications (`returns: Set<ReturnKind>`, never empty), the exhaustive
propagation set, and `characters: CharacterBound?`. A body declares one
classification, so `ValueUnit(returns: ReturnKind, ...)` builds the unit of
that one kind; the set grows only through `join`. An absent propagation
section in the declaring body becomes the empty list — no flow, not unknown
flow; an absent characters section stays null — no bound.

**Validation (`init`):** the classification set is non-empty.

**Methods:** `fun join(other: ValueUnit): ValueUnit` — the union of the
classifications, the distinct flows of both (this unit's first), and the
characters bound when both units carry equal bounds, else null.

### ReturnKind

**Responsibility:** Closed classification of the result of a call with no
interpretable body: `STR`, `NUM`, `BOOL`, `ANY`.

**Methods:** `fun join(other: ReturnKind): ReturnKind` — itself when equal,
`ANY` otherwise. Mapping into a consumer's value lattice is the consumer's
extension; this library owns no lattice type.

Domain semantics: [model.md](model.md).
