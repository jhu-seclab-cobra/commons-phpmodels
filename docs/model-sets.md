# PHP Models — Document Sets and Category Mapping

Domain semantics of how models are stored and mounted: the document set a
producer publishes, the vocabulary accumulated across sets, and the mapping
that translates one set's names into a consumer's. Base entities — Model,
Vocabulary, Policy, Origin Color, Danger Category: [model.md](model.md).

## Entities

- **Document Set** — One stored source of models: a root, a manifest, at
  most one vocabulary document, at most one policy document, and the model
  documents the manifest lists. Existence condition: the root holds a
  manifest. Identity is the root. Order within the set is manifest order;
  the set's position among other sets is the consumer's.
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

## Invariants

- A set's manifest lists each document at most once.
- The three fixed file names are `index.txt`, `vocabulary.yaml`, and
  `policy.yaml`, directly under the root. No other location is searched.
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

Concept and rationale: [concept.md](concept.md). Software structure:
[design-sets.md](design-sets.md).
