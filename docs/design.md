# PHP Models — Type Model Design

The assertion-axis types: vocabulary, policy, and the sectioned model body.
Subjects, ports, and propagations: [design-subjects.md](design-subjects.md).
The entry form: [design-entries.md](design-entries.md). Conditions:
[design-conditions.md](design-conditions.md). Signatures:
[design-declarations.md](design-declarations.md). Document sets and
category mapping: [design-sets.md](design-sets.md).

## Design Overview

- **Classes:** `VulnClassId`, `OriginId` (value classes), `VulnClassDecl`,
  `OriginDecl`, `Vocabulary`, `PolicyRow`, `TaintPolicy`, `KeyPattern`,
  `SourceDecl`, `SinkDecl`, `SanitizerDecl`, `ModelBody`, `ValueSemantics`,
  `ModelYaml`, `VocabularyLoader`, `PolicyLoader`, `ModelLoader` (the last
  four internal objects)
- **Sealed hierarchies:** `ModelSubject` and `Port` — specified in
  [design-subjects.md](design-subjects.md)
- **Enums:** `ReturnKind` (STR, NUM, BOOL, ANY)
- **Relationships:** `ModelBody` contains the optional section values;
  `SinkDecl` contains one `Port.Argument` and one `VulnClassId`;
  `SourceDecl` contains an optional `Port.Argument` site override and
  optional `KeyPattern`s; `TaintPolicy` is built from `PolicyRow`s; the
  three loaders decode through `ModelYaml`. All arrows one-way into the
  data types.
- **Exceptions:** `VocabularyException` extends `IllegalArgumentException`,
  raised on undeclared or duplicate vocabulary references;
  `IllegalArgumentException` from `init` blocks and creators on every other
  format violation.
- **Dependency roles:** Data holders: all model types. Decoder: `ModelYaml`.
  Single-document loaders: `VocabularyLoader`, `PolicyLoader`,
  `ModelLoader`, composed by `DocumentSetLoader`
  ([design-sets.md](design-sets.md)). Consumers live outside this library.

Package `edu.jhu.cobra.commons.phpmodels`, single module, `explicitApi()`.
Every model type is public — exposing them is the library's purpose.
`ModelYaml` and the single-document loaders are internal: no Jackson type
crosses the public API, so Jackson stays an `implementation` dependency,
and the set is the only unit a consumer loads. Dependencies: Jackson
(`jackson-dataformat-yaml`, `jackson-module-kotlin`) as `implementation`;
commons-value as `api`, for the scalars a condition compares
([design-conditions.md](design-conditions.md)). No dependency on any
analyzer.

## Class / Type Specifications

### VulnClassId, OriginId

**Responsibility:** Interned reference tokens (`@JvmInline value class` over
`String`, lowercased) for a declared danger category and origin color.
Replace raw strings past the load boundary so a name mismatch cannot occur
downstream. Decode from the bare scalar.

### VulnClassDecl, OriginDecl

**Responsibility:** One declared vocabulary entry: interned identity plus a
human description that self-documents the file and enriches the
undeclared-reference error message.

### Vocabulary

**Responsibility:** The two closed declared sets. Sole authority for what
category and color names exist.

**State/Fields:** `vulnClasses: Map<VulnClassId, VulnClassDecl>`,
`origins: Map<OriginId, OriginDecl>` (YAML section `provenances:`).

**Methods:**
- `fun requireVulnClass(raw: String): VulnClassId` — interns and validates;
  `VocabularyException` when undeclared.
- `fun requireOrigin(raw: String): OriginId` — same for colors.

### PolicyRow, TaintPolicy

**Responsibility:** `PolicyRow` — one statement: an origin color and the
danger categories it enables. `TaintPolicy` — the folded origin→categories
matrix; rows sharing an origin accumulate by union.

**Validation (`init`):** `enables` non-empty — a row enabling no category
asserts nothing, like every other empty declared section.

**Methods:** `TaintPolicy.isDangerous(color, category): Boolean`.

### SourceDecl

**Responsibility:** One element of the sources section: a non-empty
produced color set, an optional explicit production site, and optional key
patterns restricting production to matching array keys.

**State/Fields:** `origin: Set<OriginId>` (YAML key `provenance:`), `at: Port.Argument?`
(null = the kind-fixed default site, [model.md](model.md)),
`keys: List<KeyPattern>?`.

**Validation (`init`):** `origin` non-empty; a declared `keys` list
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
`SinkDecl(port: Port.Argument, vulnClass: VulnClassId)` — one dangerously
consumed argument port under one category (YAML key `category:`);
`SanitizerDecl(categories: Set<VulnClassId>)` — a non-empty neutralized
category set.

### ModelBody

**Responsibility:** The sectioned statement of one model: five optional
assertion sections. An all-absent body is constructible — a signature-only
entry has one — and the at-least-one-section rule therefore lives at the
entry level, where the signature is visible: `ModelEntry` requires a
signature or a non-empty body ([design-entries.md](design-entries.md)).

**State/Fields:** `returns: ReturnKind?`, `propagation: List<Propagation>?`,
`sources: List<SourceDecl>?`, `sinks: List<SinkDecl>?`,
`sanitizers: List<SanitizerDecl>?`.

**Validation (`init`):** propagation requires returns (the value-semantics
unit is asserted whole or not at all — the signature-derived completion
happens in the entry, before construction); a declared section is non-empty.

**Methods:** `isEmpty: Boolean`; `declaresOnlySources: Boolean`;
`namesReceiverPort: Boolean` — a propagation side names `Port.Receiver`;
`declaresExplicitSourceSite: Boolean` — a source element declares `at`
(the port-admissibility predicates `ModelEntry` reads);
`fun valueSemantics(): ValueSemantics?` — null when returns is undeclared.

### ValueSemantics

**Responsibility:** The value-semantics unit a lookup serves: the returns
classification with the exhaustive propagation set. An absent propagation
section in the declaring body becomes the empty list — no flow, not unknown
flow.

### ReturnKind

**Responsibility:** Closed classification of the result of a call with no
interpretable body: `STR`, `NUM`, `BOOL`, `ANY`.

**Methods:** `fun join(other: ReturnKind): ReturnKind` — itself when equal,
`ANY` otherwise. Mapping into a consumer's value lattice is the consumer's
extension; this library owns no lattice type.

### ModelYaml (internal)

**Responsibility:** The one YAML decoder every document of the format passes
through, so the strictness is declared once: `FAIL_ON_UNKNOWN_PROPERTIES`,
`ACCEPT_CASE_INSENSITIVE_ENUMS`, and `STRICT_DUPLICATE_DETECTION` (a doubled
key never decodes last-wins) on a Kotlin-module mapper
([impl.md](impl.md)). Decodes one document per stream: after the root value
binds, the parser must be exhausted, so a second `---` document is a load
failure, never a silent drop. Reads the stream as UTF-8 and rejects any
YAML alias before decoding — Jackson substitutes a scalar alias with its
anchor's name, not the anchored value ([impl.md](impl.md)), so an alias
would corrupt silently. Wraps every `JsonProcessingException` in
`IllegalArgumentException` with the underlying reason. Internal, so no
Jackson type crosses the API.

### VocabularyLoader, PolicyLoader, ModelLoader (internal)

**Responsibility:** One decoder per document kind, each reading an
`InputStream` the set loader opens. Internal: a consumer loads a set, never
a single document, so the set loader is the only place these run.

**Methods:**
- `VocabularyLoader.load(input): Vocabulary` — interns names; a repeated
  name within a section is a `VocabularyException` (one of the two
  declarations would be lost).
- `PolicyLoader.load(input, vocabulary): List<PolicyRow>` — validates every
  row tag against the supplied vocabulary.
- `ModelLoader.load(input): List<ModelEntry>` — decodes one model document's
  entries ([design-entries.md](design-entries.md)); construction-time
  validation runs during the decode; two entries sharing subject and
  condition are rejected (`IllegalArgumentException` naming the subject).
  Vocabulary verification of the entries' color and category references
  runs in the set loader, which owns the cross-document load order.

## Exception / Error Types

- `VocabularyException` (extends `IllegalArgumentException`) — undeclared
  color/category reference, duplicate vocabulary entry. Never raised past
  the load boundary.
- `IllegalArgumentException` — every other format violation, raised in
  `init` blocks and creators while Jackson instantiates, so one catch at the
  caller's load boundary covers every decode failure ([impl.md](impl.md)).

Domain semantics: [model.md](model.md). The entry form:
[design-entries.md](design-entries.md).
