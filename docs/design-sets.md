# PHP Models — Document Set Design

The types loading one document set and translating its names. Semantics:
[model-sets.md](model-sets.md). Base loaders and vocabulary types:
[design.md](design.md).

## Design Overview

- **Classes:** `DocumentSet`, `Document`, `DocumentSetLoader` (object),
  `CategoryMapping`, `CategoryMappingLoader` (object)
- **Abstract:** `ResourceOpener` (`fun interface`; implemented by callers
  over classpath, file system, or archive)
- **Relationships:** `DocumentSet` contains one `Vocabulary`, a list of
  `PolicyRow`, and a list of `Document`; `Document` contains a path and a
  list of `ModelEntry`; `DocumentSetLoader` uses `ResourceOpener`,
  `VocabularyLoader`, `PolicyLoader`, `ModelLoader`, and `CategoryMapping`;
  `CategoryMappingLoader` decodes through `ModelYaml`. All arrows one-way
  into the data types; no existing type depends on a new one.
- **Exceptions:** `DocumentSetException` extends `IllegalArgumentException`
  (missing manifest or listed document, duplicate manifest line);
  `VocabularyException` (conflicting redeclaration, unmapped name, mapping
  target undeclared); `IllegalArgumentException` from decode as before.
- **Dependency roles:** Data holders: `DocumentSet`, `Document`,
  `CategoryMapping`. Orchestrator: `DocumentSetLoader`. Contract:
  `ResourceOpener`. Loaders: `CategoryMappingLoader`.

Package `edu.jhu.cobra.commons.phpmodels`. All public. Additive: the three
existing loaders keep their signatures and remain the decode surface for
one document; `DocumentSetLoader` composes them.

Value placement: the three fixed file names are constants on
`DocumentSetLoader` (`MANIFEST`, `VOCABULARY`, `POLICY`), tier constant —
they are the format's convention, never configured.

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
— manifest order.

**Methods:** `val entries: List<ModelEntry>` — every document's entries in
order, for consumers that do not need document boundaries.

### CategoryMapping

**Responsibility:** One translation table, both axes.

**State/Fields:** `val categories: Map<VulnClassId, VulnClassId?>`,
`val provenances: Map<ProvenanceId, ProvenanceId?>` — a `null` target marks
a discarded name.

**Methods:**
- `fun category(source: VulnClassId): VulnClassId?` — the target, or `null`
  when discarded. **Errors:** `VocabularyException` when `source` is
  unlisted.
- `fun provenance(source: ProvenanceId): ProvenanceId?` — likewise.
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

### DocumentSetLoader

**Responsibility:** Load one set under the caller's accumulated vocabulary,
optionally through a mapping, in the order the model fixes.

**Methods:**
- `fun load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY,
  mapping: CategoryMapping? = null): DocumentSet`
  - **Behavior:** read `index.txt` (fail when absent); when `mapping` is
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
  - **Output:** the `DocumentSet`; the caller unions `set.vocabulary` into
    its accumulator and appends `set.policy` and `set.entries` in order.
  - **Errors:** `DocumentSetException`, `VocabularyException`,
    `IllegalArgumentException` as listed above.
- `const val MANIFEST = "index.txt"`, `VOCABULARY = "vocabulary.yaml"`,
  `POLICY = "policy.yaml"`.

### Vocabulary (extension)

**Methods added:** `fun merge(other: Vocabulary): Vocabulary` — union;
`VocabularyException` on a name declared in both with different
description. `companion val EMPTY`. `fun verify(entry: ModelEntry)` —
every category and color the entry references is declared; moves the check
consumers currently write by hand into the authority that owns the sets.

## Exception / Error Types

- `DocumentSetException` extends `IllegalArgumentException` — manifest
  absent, listed document absent, duplicate manifest line; message names the
  path.
- `VocabularyException` — conflicting redeclaration (names the name and
  both descriptions), unmapped name (names the name and the set), mapping
  target undeclared (names the target).

Domain semantics: [model-sets.md](model-sets.md). Entry forms:
[design-generators.md](design-generators.md).
