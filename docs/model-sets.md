# PHP Models — Document Sets and Category Mapping

Domain semantics of how models are stored and mounted: the document set a
producer publishes, the vocabulary accumulated across sets, and the mapping
that translates one set's names into a consumer's. Base entities — Model,
Vocabulary, Policy, Origin Color, Danger Category: [model.md](model.md).

## Entities

- **Document Set** — One stored source of models: a root, a manifest, at
  most one vocabulary document, at most one policy document, at most one
  provenance document, and the model documents the manifest lists.
  Existence condition: the root holds a manifest. Identity is the root.
  Order within the set is manifest order; the set's position among other
  sets is the consumer's.
- **Set Provenance** — The statement of where a set's entries come from: a
  producer and a verification kind. Existence condition: the root holds a
  provenance document; a set without one has no provenance and the
  consumer's default stands in. One per set; it applies to every entry
  the set lists and never varies within the set.
- **Producer** — The non-blank identifier of the process or party that
  emitted a set. Documentary: it names the source in diagnostics and is
  never compared for rank.
- **Verification** — The closed kind stating how a set's entries were
  checked. Two kinds exist: *generated*, emitted by a program from an
  upstream source and never reviewed entry by entry; *manual*, written or
  reviewed by hand. Any other spelling is a load failure.
- **Precedence** — The consumer's total order over verification kinds,
  highest first. Existence condition: supplied by the consumer once for
  all the sets it mounts. The default order ranks manual above generated.
- **Manifest** — The ordered list of model document paths under a root,
  relative to it. A path that resolves to nothing is a load failure. A
  comment line or blank line is not an entry.
- **Accumulated Vocabulary** — The vocabulary in force while a consumer
  loads sets in order: the union of every set's declarations loaded so far.
  Every reference in a set's policy and models is checked against the
  accumulated vocabulary after that set's own declarations join it.
- **Redeclaration** — A set declaring a name the accumulated vocabulary
  already holds. Admitted when the declaration is identical to the one in
  force; otherwise a load failure naming the set and the name. The set in
  force never changes on admission.
- **Category Mapping** — A consumer-supplied total function from the names
  one set uses to the consumer's names, one relation per axis: danger
  category to danger category, origin color to origin color. Each source
  name is mapped to one declared target name or marked discarded. Existence
  condition: supplied by the consumer for one set; never stored with the
  set it translates.
- **Mapped Set** — A document set loaded under a category mapping. Its own
  vocabulary document, when present, is read only to know which names it
  uses; none of its declarations join the accumulated vocabulary. Its
  policy, when present, is translated like its models.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Document Set | Manifest | is listed by | 1:1 | The manifest fixes which documents belong and in what order |
| Document Set | Model | contains | 1:N | Every entry of every listed document |
| Document Set | Vocabulary | declares | 1:0..1 | Declarations join the accumulated vocabulary unless the set is mapped |
| Document Set | Policy | declares | 1:0..1 | Rows accumulate with the consumer's policy |
| Category Mapping | Document Set | translates | 1:1 | Applied to exactly one set at its load |
| Category Mapping | Danger Category / Origin Color | targets | N:1 | Every target is declared in the accumulated vocabulary |
| Accumulated Vocabulary | Document Set | grows by | 1:N | Set order is declaration order |
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
| sink element | category replaced | element removed | load failure |
| sanitizer element | category replaced | category removed; an emptied element is removed | load failure |
| source element | color replaced | color removed; an emptied element is removed | load failure |
| policy row | origin and categories replaced | row or category removed; an emptied row is removed | load failure |

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
- Translation never changes a subject, a port, a guard, a signature, or the
  value-semantics unit. Only names on the two vocabulary axes change.
- Loading a set is a read of stored documents; a set is never written by
  the load, and loading the same set twice yields equal results.

## Cross-Structure Contracts

- **Mapped ≡ written.** After translation an entry is indistinguishable
  from one written in the consumer's names; no consumer sees the source
  names.
- **Vocabulary authority is unchanged.** The accumulated vocabulary is the
  sole authority for what names exist; a mapping only spells entries in
  those names.
- **Set provenance is not an origin color.** The origin color a source
  gives a tainted value ([model.md](model.md)) says where data comes from
  at run time; the set provenance says where the *statement* comes from.
  Neither is read for the other.

Concept and rationale: [concept.md](concept.md). Software structure:
[design-sets.md](design-sets.md).
