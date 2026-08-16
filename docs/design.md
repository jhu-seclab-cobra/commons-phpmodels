# PHP Models — Type Model Design

The assertion-axis types: vocabulary, policy, and the sectioned model body.
Subjects, ports, and propagations: [design-subjects.md](design-subjects.md).
Entry forms and generators: [design-generators.md](design-generators.md).
Guards: [design-guards.md](design-guards.md). Signatures:
[design-declarations.md](design-declarations.md).

## Design Overview

- **Classes:** `VulnClassId`, `ProvenanceId` (value classes), `VulnClassDecl`,
  `ProvenanceDecl`, `Vocabulary`, `PolicyRow`, `TaintPolicy`, `KeyPattern`,
  `SourceDecl`, `SinkPoint`, `SanitizerDecl`, `ModelBody`, `ValueSemantics`,
  `ModelYaml` (internal object), `VocabularyLoader` (object), `PolicyLoader`
  (object), `ModelLoader` (object)
- **Sealed hierarchies:** `ModelSubject` and `Port` — specified in
  [design-subjects.md](design-subjects.md)
- **Enums:** `ReturnKind` (STR, NUM, BOOL, ANY)
- **Relationships:** `ModelBody` contains the optional section values;
  `SinkPoint` contains one `Port.Argument` and one `VulnClassId`;
  `SourceDecl` contains an optional `Port.Argument` site override and
  optional `KeyPattern`s; `TaintPolicy` is built from `PolicyRow`s; the
  three loaders decode through `ModelYaml`. All arrows one-way into the
  data types.
- **Exceptions:** `VocabularyException` extends `IllegalArgumentException`,
  raised on undeclared or duplicate vocabulary references;
  `IllegalArgumentException` from `init` blocks and creators on every other
  format violation.
- **Dependency roles:** Data holders: all model types. Decoder: `ModelYaml`.
  Loaders: `VocabularyLoader`, `PolicyLoader`. Consumers live outside this
  library.

Package `edu.jhu.cobra.commons.phpmodels`, single module, `explicitApi()`.
Every model type is public — exposing them is the library's purpose.
`ModelYaml` is internal: no Jackson type crosses the public API, so Jackson
stays an `implementation` dependency. Dependencies: Jackson
(`jackson-dataformat-yaml`, `jackson-module-kotlin`) only. No dependency on
any analyzer or value-lattice library.

## Class / Type Specifications

### VulnClassId, ProvenanceId

**Responsibility:** Interned reference tokens (`@JvmInline value class` over
`String`, lowercased) for a declared danger category and origin color.
Replace raw strings past the load boundary so a name mismatch cannot occur
downstream. Decode from the bare scalar.

### VulnClassDecl, ProvenanceDecl

**Responsibility:** One declared vocabulary entry: interned identity plus a
human description that self-documents the file and enriches the
undeclared-reference error message.

### Vocabulary

**Responsibility:** The two closed declared sets. Sole authority for what
category and color names exist.

**State/Fields:** `vulnClasses: Map<VulnClassId, VulnClassDecl>`,
`provenances: Map<ProvenanceId, ProvenanceDecl>`.

**Methods:**
- `fun requireVulnClass(raw: String): VulnClassId` — interns and validates;
  `VocabularyException` when undeclared.
- `fun requireProvenance(raw: String): ProvenanceId` — same for colors.

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

**State/Fields:** `provenance: Set<ProvenanceId>`, `at: Port.Argument?`
(null = the kind-fixed default site, [model.md](model.md)),
`keys: List<KeyPattern>?`.

**Validation (`init`):** `provenance` non-empty; a declared `keys` list
non-empty. Whether the subject admits an explicit site is entry-level
validation ([design-generators.md](design-generators.md)).

### KeyPattern

**Responsibility:** One key pattern of a source element: the declared
pattern with its regex, compiled at construction so no uncompiled pattern
survives the load; equality over the pattern text (`Regex` carries no value
equality). Decoded from the bare pattern scalar.

**Methods:** `fun matches(key: String): Boolean` — entire-key,
case-sensitive match, never a substring match.

### SinkPoint, SanitizerDecl

**Responsibility:** One element of the sinks / sanitizers sections: one
dangerously consumed argument port under one category; a non-empty
neutralized category set.

### ModelBody

**Responsibility:** The sectioned statement of one model: five optional
assertion sections. One shape shared by the flat entry and the generator
body, so a body written in either form carries the same validation. An
all-absent body is constructible — a signature-only entry has one — and the
at-least-one-section rule therefore lives at the entry level, where the
signature is visible: `SubjectModel` requires a signature or a non-empty
body, `ModelGenerator` requires a non-empty body.

**State/Fields:** `returns: ReturnKind?`, `propagation: List<Propagation>?`,
`sources: List<SourceDecl>?`, `sinks: List<SinkPoint>?`,
`sanitizers: List<SanitizerDecl>?`.

**Validation (`init`):** propagation requires returns (the value-semantics
unit is asserted whole or not at all — the signature-derived completion
happens in the entry, before construction); a declared section is non-empty.

**Methods:** `isEmpty: Boolean`; `declaresOnlySources: Boolean`;
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
failure, never a silent drop. Wraps every `JsonProcessingException` in
`IllegalArgumentException` with the underlying reason. Internal — the three
loaders are the public surface, so no Jackson type crosses the API.

### VocabularyLoader, PolicyLoader, ModelLoader

**Responsibility:** The public decode surface, one loader per document kind,
each reading a caller-supplied `InputStream`. Where the documents live —
classpath resource, file, artifact — is the caller's value placement, not
this library's.

**Methods:**
- `VocabularyLoader.load(input): Vocabulary` — interns names; a repeated
  name within a section is a `VocabularyException` (one of the two
  declarations would be lost).
- `PolicyLoader.load(input, vocabulary): List<PolicyRow>` — validates every
  row tag against the supplied vocabulary.
- `ModelLoader.load(input): List<ModelEntry>` — decodes one model document's
  entries ([design-generators.md](design-generators.md)); construction-time
  validation runs during the decode. Vocabulary interning of the entries'
  color and category references stays with the caller, which owns the
  cross-document load order.

## Exception / Error Types

- `VocabularyException` (extends `IllegalArgumentException`) — undeclared
  color/category reference, duplicate vocabulary entry. Never raised past
  the load boundary.
- `IllegalArgumentException` — every other format violation, raised in
  `init` blocks and creators while Jackson instantiates, so one catch at the
  caller's load boundary covers every decode failure ([impl.md](impl.md)).

Domain semantics: [model.md](model.md). Entry forms:
[design-generators.md](design-generators.md).
