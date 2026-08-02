# PHP Models — Type Model Design

The assertion-axis types: vocabulary, policy, subjects, ports, propagations,
and the sectioned model body. Entry forms and generators:
[design-generators.md](design-generators.md). Guards:
[design-guards.md](design-guards.md). Signatures:
[design-declarations.md](design-declarations.md).

## Design Overview

- **Classes:** `VulnClassId`, `ProvenanceId` (value classes), `VulnClassDecl`,
  `ProvenanceDecl`, `Vocabulary`, `PolicyRow`, `TaintPolicy`, `Propagation`,
  `SourceDecl`, `SinkPoint`, `SanitizerDecl`, `ModelBody`, `ValueSemantics`,
  `ModelYaml` (internal object), `VocabularyLoader` (object), `PolicyLoader`
  (object), `ModelLoader` (object)
- **Sealed hierarchies:** `ModelSubject` (seven subtypes:
  `FunctionSubject`, `ClassSubject`, `MethodSubject`, `ClassConstantSubject`,
  `PropertySubject`, `ConstantSubject`, `VariableSubject`); `Port`
  (`Argument`, `Return`)
- **Enums:** `ReturnKind` (STR, NUM, BOOL, ANY)
- **Relationships:** `ModelBody` contains the optional section values;
  `Propagation` contains two `Port`s; `SinkPoint` contains one
  `Port.Argument` and one `VulnClassId`; `TaintPolicy` is built from
  `PolicyRow`s; the three loaders decode through `ModelYaml`. All arrows
  one-way into the data types.
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

**Methods:** `TaintPolicy.isDangerous(color, category): Boolean`.

### ModelSubject (sealed)

**Responsibility:** The PHP declaration a model identifies. One subtype per
kind, holding exactly that kind's identity fields, case-folded per the table
in [model-declarations.md](model-declarations.md). Equality over the folded
identity, so a subject is usable as a lookup key.

**Decoding:** the YAML form is a one-key mapping — the key names the kind,
the value is the PHP-native spelling string (`function: strlen`,
`method: mysqli::query`). Each subtype's companion carries a creator taking
the raw spelling; one private splitter owns the `::` grammar (exactly one
separator, both sides non-empty, `$` mandatory for property names and
forbidden elsewhere, leading namespace slashes stripped). Violations throw
`IllegalArgumentException` inside the creator. Jackson mechanism verified in
[impl.md](impl.md).

**Subtypes, YAML kind keys, and identity:**
- `FunctionSubject(name)` — `function:`, folded.
- `ClassSubject(name)` — `class:`, folded.
- `MethodSubject(owner, name)` — `method:`, both folded; spelled
  `class::name`.
- `ClassConstantSubject(owner, name)` — `class_constant:`, owner folded,
  name sensitive.
- `PropertySubject(owner, name)` — `property:`, owner folded, name
  sensitive; spelled `class::$name`, stored without the `$`.
- `ConstantSubject(name)` — `constant:`, sensitive.
- `VariableSubject(name)` — `variable:`, folded; spelled `$name`, stored
  without the `$`.

**Validation:** blank identity fields are rejected in `init`.

### Port (sealed)

**Responsibility:** One explicitly named location in a call, decoded from
the string spellings `argument(n)` and `return`. No bare integer and no
sentinel value exists anywhere in the port vocabulary.

**Subtypes:** `Argument(position: Int)` — `position >= 0`, with its own
narrowing creator (a field typed as the subtype does not consult the
supertype's creator, [impl.md](impl.md)); `Return` (object).

**Methods:** companion `parse(raw: String): Port` — the decode entry point;
`IllegalArgumentException` on any other spelling.

### Propagation

**Responsibility:** One declared flow between two ports. Accepts the synonym
spellings `from`/`input` and `to`/`output` as four nullable creator
parameters — a Jackson alias would let a pair naming both spellings decode
silently ([impl.md](impl.md)).

**Validation:** exactly one spelling per side; `to != from`.

### SourceDecl, SinkPoint, SanitizerDecl

**Responsibility:** One element of the sources / sinks / sanitizers
sections: a non-empty produced color set; one dangerously consumed argument
port under one category; a non-empty neutralized category set.

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
through, so the strictness is declared once: `FAIL_ON_UNKNOWN_PROPERTIES`
and `ACCEPT_CASE_INSENSITIVE_ENUMS` on a Kotlin-module mapper
([impl.md](impl.md)). Wraps every `JsonProcessingException` in
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
