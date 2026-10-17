# PHP Models — Type Model Design

The assertion-axis types: vocabulary, policy, the decoder, and the
single-document loaders. The sectioned model body:
[design-bodies.md](design-bodies.md). Subjects, ports, and propagations:
[design-subjects.md](design-subjects.md).
The entry form: [design-entries.md](design-entries.md). Conditions:
[design-conditions.md](design-conditions.md). Signatures:
[design-declarations.md](design-declarations.md). Document sets and
category mapping: [design-sets.md](design-sets.md).

## Design Overview

- **Classes:** `VulnTypeId`, `SourceKindId` (value classes), `VulnTypeDecl`,
  `SourceKindDecl`, `Vocabulary`, `PolicyRow`, `TaintPolicy`, `ModelYaml`,
  `VocabularyLoader`, `PolicyLoader`, `ModelLoader` (the last four internal
  objects); the body types: [design-bodies.md](design-bodies.md)
- **Sealed hierarchies:** `ModelSubject` and `Port` — specified in
  [design-subjects.md](design-subjects.md)
- **Relationships:** `Vocabulary` contains the declarations;
  `TaintPolicy` is built from `PolicyRow`s; the three loaders decode
  through `ModelYaml`. All arrows one-way into the data types.
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

### VulnTypeId, SourceKindId

**Responsibility:** Interned reference tokens (`@JvmInline value class` over
`String`, lowercased) for a declared vulnerability type and source kind.
Replace raw strings past the load boundary so a name mismatch cannot occur
downstream. Decode from the bare scalar.

### VulnTypeDecl, SourceKindDecl

**Responsibility:** One declared vocabulary entry: interned identity plus a
human description that self-documents the file and enriches the
undeclared-reference error message.

### Vocabulary

**Responsibility:** The two closed declared sets. Sole authority for what
vulnerability type and source kind names exist.

**State/Fields:** `vulnTypes: Map<VulnTypeId, VulnTypeDecl>`,
`sourceKinds: Map<SourceKindId, SourceKindDecl>`.

**Methods:**
- `fun requireVulnType(raw: String): VulnTypeId` — interns and validates;
  `VocabularyException` when undeclared.
- `fun requireSourceKind(raw: String): SourceKindId` — same for source
  kinds.

### PolicyRow, TaintPolicy

**Responsibility:** `PolicyRow` — one statement: a source kind and the
vulnerability types it enables. `TaintPolicy` — the folded source kind →
vulnerability types matrix; rows sharing a source kind accumulate by union.

**Validation (`init`):** `enables` non-empty — a row enabling no
vulnerability type asserts nothing, like every other empty declared section.

**Methods:** `TaintPolicy.isDangerous(sourceKind, vulnType): Boolean`.

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
  Vocabulary verification of the entries' source kind and vulnerability
  type references runs in the set loader, which owns the cross-document load order.

## Exception / Error Types

- `VocabularyException` (extends `IllegalArgumentException`) — undeclared
  source kind or vulnerability type reference, duplicate vocabulary entry. Never raised past
  the load boundary.
- `IllegalArgumentException` — every other format violation, raised in
  `init` blocks and creators while Jackson instantiates, so one catch at the
  caller's load boundary covers every decode failure ([impl.md](impl.md)).

Domain semantics: [model.md](model.md). The entry form:
[design-entries.md](design-entries.md).
