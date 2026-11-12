# PHP Models — Document Set and Vocabulary Mapping

Domain semantics of how models are stored and mounted: the Document Set a
producer publishes, the vocabulary accumulated across sets, and the mapping
that translates one set's names into a consumer's. Base entities — Model,
Vocabulary, Policy, Source kind, Vulnerability type: [model.md](model.md).

## Entities

- **[Document Set](glossary/glossary-sets.md#document-set)** — Supplies one root of model input to a consumer; it carries a manifest, at most one Vocabulary, Policy and provenance document, and the listed Model documents.
  Existence condition: the root holds a manifest. Identity is the root.
  Order within the set is manifest order; the set's position among other
  sets is the consumer's.
- **[Set Provenance](glossary/glossary-sets.md#set-provenance)** — Supplies the producer and Verification used by the consumer's ranking. Existence condition: the root holds a
  provenance document; a set without one has no provenance and the
  consumer's default stands in. One per set; it applies to every entry
  the set lists and never varies within the set.
- **Producer** — The non-blank identifier of the process or party that
  emitted a set. Documentary: it names the source in diagnostics and is
  never compared for rank.
- **[Verification](glossary/glossary-sets.md#verification)** — Supplies the checking category for ranking; a spelling outside its glossary enumeration is a load failure.
- **[Precedence](glossary/glossary-sets.md#precedence)** — Ranks sets through their Verification, highest first. Existence condition: supplied by the consumer once for
  all the sets it mounts. The default order ranks manual above generated.
- **Manifest** — The ordered list of model document paths under a root,
  relative to it. A path that resolves to nothing is a load failure. A
  comment line or blank line is not an entry.
- **Vocabulary accumulated by the consumer** — The vocabulary in force while a consumer
  loads sets in order: the union of every set's declarations loaded so far.
  Every reference in a set's policy and models is checked against the
  accumulated vocabulary after that set's own declarations join it.
- **Repeated declaration** — A set declaring a name the accumulated vocabulary
  already holds. Admitted when the declaration is identical to the one in
  force; otherwise a load failure naming the set and the name. The set in
  force never changes on admission.
- **[Vocabulary Mapping](glossary/glossary-sets.md#vocabulary-mapping)** — Adapts a set's references to the consumer Vocabulary, one relation per axis:
  Vulnerability type to Vulnerability type, Source kind to Source kind.
  Each source name is mapped to one declared target name or marked
  discarded. Existence condition: supplied by the consumer for one set;
  never stored with the set it translates.
- **Document Set loaded with a Vocabulary Mapping** — A Document Set loaded under a Vocabulary Mapping. Its own
  vocabulary document, when present, is not read: the names the mapping
  lists are the names the set may use; none of its declarations join the
  accumulated vocabulary. Its
  policy, when present, is translated like its models.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Document Set | Manifest | is listed by | 1:1 | The manifest fixes which documents belong and in what order |
| Document Set | Model | contains | 1:N | Every entry of every listed document |
| Document Set | Vocabulary | declares | 1:0..1 | Declarations join the accumulated vocabulary unless the set is mapped |
| Document Set | Policy | declares | 1:0..1 | Rows accumulate with the consumer's policy |
| Vocabulary Mapping | Document Set | translates | 1:1 | Applied to exactly one set at its load |
| Vocabulary Mapping | Vulnerability type / Source kind | targets | N:1 | Every target is declared in the accumulated vocabulary |
| Vocabulary accumulated by the consumer | Document Set | grows by | 1:N | Set order is declaration order |
| Document Set | Set Provenance | declares | 1:0..1 | One statement covering every listed entry |
| Set Provenance | Producer | names | 1:1 | Documentary; never ranked |
| Set Provenance | Verification | states | 1:1 | The rank input |
| Precedence | Verification | orders | 1:N | Total, highest first; every kind ranked exactly once |

## State Model

### Name Lifecycle Across Sets

| State | Trigger | Target |
|-------|---------|-------|
| Undeclared | a set declares it | Declared, from that set |
| Declared | a later set declares it identically | Declared, unchanged |
| Declared | a later set declares it differently | load failure |
| Undeclared | a set references it | load failure |
| Undeclared | a mapping targets it | load failure |

### Translation of One Entry

| Element | Mapped name | Discarded name | Unlisted name |
|---------|-------------|----------------|---------------|
| sink element | Vulnerability type replaced | element removed | load failure |
| sanitizer element | Vulnerability type replaced | Vulnerability type removed; an element left with no Vulnerability type and no other statement of its check is removed | load failure |
| source element | Source kind replaced | Source kind removed; an emptied element is removed | load failure |
| policy row | Source kind and Vulnerability type entries replaced | row or Vulnerability type removed; an emptied row is removed | load failure |

An entry whose translation removes its last section, and which carries no
signature, is removed from the set. Removal is not a failure: the set said
nothing the consumer keeps.

### Rank of Two Sets Stating One Subject and Unit

| Set A verification | Set B verification | Winner |
|--------------------|--------------------|--------|
| ranks higher under the Precedence | ranks lower | A, whatever the mount order |
| equal | equal | the consumer's mount order decides |
| absent (no provenance document) | any | A ranks as the consumer's default kind |

The rank is read from the two sets' provenance alone; no entry field
takes part.

## Invariants

- A set's manifest lists each document at most once.
- The four fixed file names are `index.txt`, `vocabulary.yaml`,
  `policy.yaml`, and `provenance.yaml`, directly under the root. No other
  location is searched.
- A set's provenance is the same value for every entry the set lists; a
  mapping never changes it.
- A precedence ranks every verification kind exactly once; two kinds never
  share a rank.
- A set's own declarations are in force before its policy and models are
  checked; a set may reference what it declares.
- A mapping is total over the names its set uses: every name is mapped or
  discarded. A name listed in the mapping but unused by the set is not a
  failure.
- A mapping never introduces a name: every target is already declared in
  the accumulated vocabulary when the mapped set loads.
- Translation never changes a subject, a port, a condition, a signature, the
  Value unit, or the throws section. Only names on the two
  vocabulary axes change.
- Loading a set is a read of stored documents; a set is never written by
  the load, and loading the same set twice yields equal results.

## Cross-Structure Contracts

- **Mapped ≡ written.** After translation an entry is indistinguishable
  from one written in the consumer's names; no consumer sees the source
  names.
- **Vocabulary authority is unchanged.** The accumulated vocabulary is the
  sole authority for what names exist; a mapping only spells entries in
  those names.
- **Set Provenance is not a Source kind.** The Source kind a source
  gives a tainted value ([model.md](model.md)) says where data comes from
  at run time; the Set Provenance says where the *statement* comes from.
  Neither is read for the other.

Concept and rationale: [concept.md](concept.md). Software structure:
[design-sets.md](design-sets.md).
