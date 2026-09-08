# PHP Models — Document Set Design

The types loading one document set and translating its names. Semantics:
[model-sets.md](model-sets.md). Base loaders and vocabulary types:
[design.md](design.md).

## Design Overview

- **Classes:** `DocumentSet`, `Document`, `DocumentSetLoader` (object),
  `CategoryMapping`, `CategoryMappingLoader` (object), `SetProvenance`,
  `Verification` (enum), `Precedence`, `ProvenanceLoader` (internal object)
- **Abstract:** `ResourceOpener` (`fun interface`; implemented by callers
  over classpath, file system, or archive)
- **Relationships:** `DocumentSet` contains one `Vocabulary`, a list of
  `PolicyRow`, a list of `Document`, and at most one `SetProvenance`;
  `Document` contains a path and a list of `ModelEntry`; `SetProvenance`
  contains a producer string and one `Verification`; `Precedence` orders
  `Verification`; `DocumentSetLoader` uses `ResourceOpener`,
  `VocabularyLoader`, `PolicyLoader`, `ProvenanceLoader`, `ModelLoader`,
  and `CategoryMapping`; `CategoryMappingLoader` and `ProvenanceLoader`
  decode through `ModelYaml`. All arrows one-way into the data types.
- **Exceptions:** `DocumentSetException` extends `IllegalArgumentException`
  (missing manifest or listed document, duplicate manifest line, malformed
  listed document with the decode failure as cause); `VocabularyException`
  (conflicting redeclaration, undeclared reference naming the document,
  unmapped name, mapping target undeclared); `IllegalArgumentException`
  from the vocabulary, policy, provenance, and mapping decodes as before.
- **Dependency roles:** Data holders: `DocumentSet`, `Document`,
  `CategoryMapping`, `SetProvenance`, `Precedence`. Orchestrator:
  `DocumentSetLoader`. Contract: `ResourceOpener`. Loaders:
  `CategoryMappingLoader` (public: a consumer decodes its own mapping
  document), `ProvenanceLoader` (internal).

Package `edu.jhu.cobra.commons.phpmodels`. The two public loaders,
`DocumentSetLoader` and `CategoryMappingLoader`, are the whole decode
surface; the single-document loaders they compose are internal
([design.md](design.md)).

Value placement: the four fixed file names are constants on
`DocumentSetLoader` (`MANIFEST`, `VOCABULARY`, `POLICY`, `PROVENANCE`),
tier constant — they are the format's convention, never configured. The
default precedence order is the constant `Precedence.DEFAULT`; a consumer
that ranks differently constructs its own `Precedence`.

## Class / Type Specifications

### ResourceOpener

**Responsibility:** The caller's storage. One method: given a path relative
to the set root, return an `InputStream` or `null` when nothing is there.
The library never touches the classpath or the file system itself.

### Document

**Responsibility:** One decoded model document of a set.

**State/Fields:** `val path: String` (manifest spelling, relative to the
root); `val entries: List<ModelEntry>` (document order).

### DocumentSet

**Responsibility:** One loaded set, ready for a consumer to mount.

**State/Fields:** `val vocabulary: Vocabulary` — the declarations this set
contributed (empty for a mapped set); `val policy: List<PolicyRow>` — this
set's rows, already in the consumer's names; `val documents: List<Document>`
— manifest order; `val provenance: SetProvenance?` — the set's declared
provenance, `null` when the root holds no `provenance.yaml`.

**Methods:** `val entries: List<ModelEntry>` — every document's entries in
order, for consumers that do not need document boundaries.

### Verification

**Responsibility:** The closed kind of checking a set's entries received.
`enum class Verification { GENERATED, MANUAL }`; decoded from the
lowercase scalars `generated` and `manual`.

### SetProvenance

**Responsibility:** One set's declared provenance, attached to the loaded
set and read by the consumer's `Precedence`.

**State/Fields:** `val producer: String` — non-blank identifier of the
emitting process or party; `val verification: Verification`.

**Validation (`init`):** `producer` non-blank (`IllegalArgumentException`).

### Precedence

**Responsibility:** The consumer's total order over `Verification`,
highest first; a `Comparator<Verification>` so a consumer's fold compares
two sets' kinds without knowing the order.

**State/Fields:** `val order: List<Verification>` — every kind exactly once,
highest first. `companion val DEFAULT = Precedence(listOf(MANUAL, GENERATED))`.

**Methods:**
- `fun rank(kind: Verification): Int` — position in `order`; `0` is highest.
- `override fun compare(a: Verification, b: Verification): Int` — positive
  when `a` ranks higher than `b`, zero when equal.

**Validation (`init`):** `order` lists every `Verification` exactly once
(`IllegalArgumentException`).

### ProvenanceLoader (internal)

**Responsibility:** Decode one provenance document.

**Methods:** `fun load(input: InputStream): SetProvenance`. Document shape:
two scalars, `producer:` and `verification:`. Unknown keys, a missing
field, a blank producer, and a verification spelling outside the enum fail
the decode with `IllegalArgumentException`.

### CategoryMapping

**Responsibility:** One translation table, both axes.

**State/Fields:** `val categories: Map<VulnClassId, VulnClassId?>`,
`val origins: Map<OriginId, OriginId?>` (YAML section `provenances:`) — a
`null` target marks a discarded name.

**Methods:**
- `fun category(source: VulnClassId): VulnClassId?` — the target, or `null`
  when discarded. **Errors:** `VocabularyException` when `source` is
  unlisted.
- `fun origin(source: OriginId): OriginId?` — likewise.
- `fun apply(entry: ModelEntry): ModelEntry?` — the entry translated per
  [model-sets.md](model-sets.md); `null` when translation empties it.
- `fun apply(rows: List<PolicyRow>): List<PolicyRow>` — rows translated,
  emptied rows dropped.
- `init`: no structural rule; whether each target is declared is checked
  at load against the accumulated vocabulary, because the mapping alone
  does not know the consumer's vocabulary.

### CategoryMappingLoader

**Responsibility:** Decode one mapping document.

**Methods:** `fun load(input: InputStream): CategoryMapping`. Document
shape: two maps, `categories:` and `provenances:`, source name to target
name; the literal target `ignore` marks a discarded name. Unknown keys and
a `null` value fail the decode. A source name may not be `ignore`.
`const val IGNORE = "ignore"` — the discard literal.

### DocumentSetLoader

**Responsibility:** Load one set under the caller's accumulated vocabulary,
optionally through a mapping, in the order the model fixes.

**Methods:**
- `fun load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY,
  mapping: CategoryMapping? = null): DocumentSet`
  - **Behavior:** read `index.txt` (fail when absent); read
    `provenance.yaml` when present, mapped or not; when `mapping` is
    null: load `vocabulary.yaml` if present and merge into `context`
    (identical redeclaration admitted, conflicting fails); load
    `policy.yaml` if present against the merged vocabulary; decode each
    listed document; verify every entry's references against the merged
    vocabulary. When `mapping` is non-null: `vocabulary.yaml` is ignored,
    every mapping target is verified declared in `context`, policy rows
    and entries are translated, references verified against `context`,
    emptied entries dropped, and the returned set's `vocabulary` is empty.
  - **Input:** `open` resolves paths relative to the root; `context` is the
    accumulated vocabulary; `mapping` translates this set.
  - **Output:** the `DocumentSet` with its provenance; the caller unions
    `set.vocabulary` into its accumulator, appends `set.policy` and
    `set.entries` in order, and keeps `set.provenance` beside the entries
    for its fold.
  - **Errors:** `DocumentSetException`, `VocabularyException`,
    `IllegalArgumentException` as listed above; a malformed
    `provenance.yaml` is an `IllegalArgumentException` like a malformed
    vocabulary.
- `const val MANIFEST = "index.txt"`, `VOCABULARY = "vocabulary.yaml"`,
  `POLICY = "policy.yaml"`, `PROVENANCE = "provenance.yaml"`.

### Vocabulary (extension)

**Methods added:** `fun merge(other: Vocabulary): Vocabulary` — union;
`VocabularyException` on a name declared in both with different
description. `companion val EMPTY`. `fun verify(entry: ModelEntry)` —
every category and color the entry references is declared; moves the check
consumers currently write by hand into the authority that owns the sets.

## Exception / Error Types

- `DocumentSetException(path, detail, cause?)` extends
  `IllegalArgumentException` — manifest absent, listed document absent,
  duplicate manifest line, listed document malformed (the format failure
  is the cause); message and `path` name the document.
- `VocabularyException` — conflicting redeclaration (names the name and
  both descriptions), undeclared reference in a listed document (names the
  name and the document), unmapped name (names the name and the set),
  mapping target undeclared (names the target).

Domain semantics: [model-sets.md](model-sets.md). The entry form:
[design-entries.md](design-entries.md).
