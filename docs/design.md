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
  `ModelYaml` (object), `VocabularyLoader` (object), `PolicyLoader` (object)
- **Sealed hierarchies:** `ModelSubject` (seven subtypes:
  `FunctionSubject`, `ClassSubject`, `MethodSubject`, `ClassConstantSubject`,
  `PropertySubject`, `ConstantSubject`, `VariableSubject`); `Port`
  (`Argument`, `Return`)
- **Enums:** `ReturnKind` (STR, NUM, BOOL, ANY)
- **Relationships:** `ModelBody` contains the optional section values;
  `Propagation` contains two `Port`s; `SinkPoint` contains one
  `Port.Argument` and one `VulnClassId`; `TaintPolicy` is built from
  `PolicyRow`s; `VocabularyLoader` and `PolicyLoader` decode through
  `ModelYaml`. All arrows one-way into the data types.
- **Exceptions:** `VocabularyException` extends `IllegalArgumentException`,
  raised on undeclared or duplicate vocabulary references;
  `IllegalArgumentException` from `init` blocks and creators on every other
  format violation.
- **Dependency roles:** Data holders: all model types. Decoder: `ModelYaml`.
  Loaders: `VocabularyLoader`, `PolicyLoader`. Consumers live outside this
  library.

Package `edu.jhu.cobra.commons.phpmodels`, single module, `explicitApi()`.
Every type above is public — exposing them is the library's purpose.
Dependencies: Jackson (`jackson-dataformat-yaml`, `jackson-module-kotlin`)
only. No dependency on any analyzer or value-lattice library.

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

**Subtypes and identity:**
- `FunctionSubject(name)` — folded.
- `ClassSubject(name)` — folded.
- `MethodSubject(owner, name)` — both folded; spelled `class::name`.
- `ClassConstantSubject(owner, name)` — owner folded, name sensitive.
- `PropertySubject(owner, name)` — owner folded, name sensitive; spelled
  `class::$name`, stored without the `$`.
- `ConstantSubject(name)` — sensitive.
- `VariableSubject(name)` — folded; spelled `$name`, stored without the `$`.

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
assertion sections, at least one section present (counting the signature at
the entry level). One shape shared by the flat entry and the generator body,
so a body written in either form carries the same validation.

**State/Fields:** `returns: ReturnKind?`, `propagation: List<Propagation>?`,
`sources: List<SourceDecl>?`, `sinks: List<SinkPoint>?`,
`sanitizers: List<SanitizerDecl>?`.

**Validation (`init`):** at least one section declared; propagation requires
returns (the value-semantics unit is asserted whole or not at all — the
signature-derived completion happens in the entry, before construction); a
declared section is non-empty.

**Methods:** `declaresOnlySources: Boolean`;
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

### ModelYaml

**Responsibility:** The one YAML decoder every document of the format passes
through, so the strictness is declared once: `FAIL_ON_UNKNOWN_PROPERTIES`
and `ACCEPT_CASE_INSENSITIVE_ENUMS` on a Kotlin-module mapper
([impl.md](impl.md)).

**Methods:**
- `fun <T> decode(input: InputStream, shape: TypeReference<T>): T` — decodes
  one document; wraps every `JsonProcessingException` in
  `IllegalArgumentException` with the underlying reason. The caller owns the
  stream and its origin — this library reads no classpath resource of its
  own.

### VocabularyLoader, PolicyLoader

**Responsibility:** Decode one vocabulary / policy document from a
caller-supplied stream into `Vocabulary` / `List<PolicyRow>`, interning
names and validating references. A repeated name within a vocabulary section
is a `VocabularyException` (one of the two declarations would be lost).
`PolicyLoader.load(input, vocabulary)` validates every row tag against the
supplied vocabulary. Where the documents live — classpath resource, file,
artifact — is the caller's value placement, not this library's.

## Exception / Error Types

- `VocabularyException` (extends `IllegalArgumentException`) — undeclared
  color/category reference, duplicate vocabulary entry. Never raised past
  the load boundary.
- `IllegalArgumentException` — every other format violation, raised in
  `init` blocks and creators while Jackson instantiates, so one catch at the
  caller's load boundary covers every decode failure ([impl.md](impl.md)).

Domain semantics: [model.md](model.md). Entry forms:
[design-generators.md](design-generators.md).
