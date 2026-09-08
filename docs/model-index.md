# PHP Models — Model Index

Domain semantics of the merge: how an ordered list of document sets becomes
one Model Index, and how a lookup under a guard context reads one Effective
Statement from it. Base entities — Model, Matching Subject, Override Unit,
Model Generator: [model.md](model.md); Document Set, Set Provenance,
Verification, Precedence, Accumulated Vocabulary: [model-sets.md](model-sets.md);
When Guard, Branch, Guard Verdict, Candidate Set, Guard Context:
[model-guards.md](model-guards.md).

## Entities

- **Model Index** — The frozen result of merging an ordered list of
  document sets under one Precedence: the accumulated vocabulary, the
  accumulated policy, one Branch Table per subject, and the generator list.
  Existence condition: the merge completed without a load failure.
  Immutable afterwards; every lookup is a read.
- **Mount Order** — The position of a set in the list handed to the merge.
  Fixed by the caller of the merge, never by the sets. Breaks ties between
  declarations of equal verification; decides nothing else.
- **Section** — The Override Unit as the merge sees it: value semantics
  (returns with propagation), sources, sinks, sanitizers, signature. One
  declaration replaces a section whole.
- **Section Precedence** — The rule choosing the declaration in force per
  (subject, guard, section): the highest-ranked verification under the
  Precedence wins; among equal verifications the later mounted wins. A set
  with no provenance ranks as the merge's default verification kind.
- **Branch Table** — For one subject, the guarded branches in
  first-declaration order across all sets, plus at most one default branch,
  each branch holding at most one Effective Declaration per section. The
  sole structure a lookup reads.
- **Effective Declaration** — The declaration in force for one (subject,
  guard, section) after the merge. Existence condition: some set declared
  the triple. Never empty: a declared section has at least one element.
- **Guard Argument** — What the index knows about one argument position of
  a call: exactly one string, one integer, one boolean, or *unknown*. The
  scalar kinds are the three the When Guard compares; a value of any other
  shape, or a value not statically one scalar, is unknown.
- **Guard Context** — A call's arity when known and one Guard Argument per
  position ([model-guards.md](model-guards.md)). Built by the consumer from
  its own value representation; the index never sees a consumer value.
- **Effective Statement** — What the index answers for one subject under
  one Guard Context: the Candidate Set combined per section by the
  combination table of [model-guards.md](model-guards.md). Each of the four
  guarded sections is *declared* (with its combined elements) or
  *undeclared*. The signature is read from the default branch alone.
- **Generator Answer** — The section a generator supplies for a subject
  that satisfies it and whose default branch declares no such section.
  Serves in the default branch's role; decided once per (subject, section),
  including the answer "none".
- **Existence** — The property of a subject that its default branch holds
  an effective signature ([model-declarations.md](model-declarations.md)).
  The index knows a subject exactly when it exists in this sense; a
  generator answer never creates existence.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Model Index | Document Set | merges | 1:N | Sets in Mount Order under one Precedence |
| Model Index | Branch Table | holds | 1:N | One table per subject any set declares |
| Model Index | Model Generator | registers | 1:N | Names unique across all merged sets |
| Branch Table | Branch | orders | 1:N | Guarded in first-declaration order; at most one default |
| Branch | Effective Declaration | holds | 1:0..5 | At most one per section |
| Effective Declaration | Document Set | comes from | N:1 | The set Section Precedence chose |
| Generator Answer | Effective Declaration | yields to | N:1 | Any explicit default-branch declaration of the section wins |
| Guard Context | Candidate Set | selects | 1:1 | Per the verdicts of every guarded branch |
| Candidate Set | Effective Statement | combines into | 1:1 | Per section, by the combination table |
| Model Index | Accumulated Vocabulary | accumulates | 1:1 | Union over the unmapped sets, in Mount Order |

## State Model

### Merge

| State | Trigger | Target |
|-------|---------|-------|
| No table | first entry for the subject in any set | table holding that entry's sections in its branch |
| Table, section undeclared in branch | an entry declares the section under the same guard | section declared, from that set |
| Table, section declared in branch | a higher-ranked, or equal-ranked later-mounted, entry declares it | section replaced in place; branch order unchanged |
| Table, section declared in branch | a lower-ranked entry declares it | unchanged |
| Table | an entry carries a guard not yet seen | branch appended after the existing guarded branches |
| Merging | every set read | frozen: the Model Index |

### Lookup

```
subject ─► branch table ─► verdicts under guard context ─► candidate set
              │                                                │
       none: unknown subject           per section: combine ─► declared / undeclared
                                                                │
                              default branch section undeclared ─► generator answer ─► none
```

`undeclared` and `unknown subject` stay distinguishable from a declared
answer: a declared section is never empty, so absence is absence, not an
empty statement.

## Invariants

- The merge replaces per (subject, guard, section) only. Replacing one
  section leaves every other section and branch of the subject untouched
  and never reorders branches.
- Verification outranks Mount Order: a manual declaration mounted first
  stays in force over a generated declaration mounted later.
- Vocabulary declarations accumulate by union in Mount Order; a
  redeclaration is admitted only when identical
  ([model-sets.md](model-sets.md)). Policy rows accumulate. A mapped set's
  policy joins translated; its vocabulary joins nothing.
- Every mapping target names a declaration already accumulated when the
  mapped set mounts.
- Every color or category an entry names is accumulated before that entry
  mounts; an undeclared reference fails the merge.
- Generator names are unique across every merged set; a repeat fails the
  merge.
- A Guard Argument that is unknown leaves every guard over that position
  undecided; the Effective Statement then combines every such branch in
  the sound direction. The index never picks one branch for the consumer.
- A guard over a position at or beyond a known arity fails; the default
  branch describes the subject with defaulted arguments.
- The signature section is read from the default branch only, under no
  guard context, with no generator fallback.
- A Generator Answer completes the default branch section by section and
  only where the default branch declares nothing for that section; it is
  never a candidate on its own and never overrides an explicit declaration.
- After the merge the index is immutable. Memoizing generator answers is
  invisible: the same (subject, section) always yields the same answer.

## Cross-Structure Contracts

- **Merge belongs to the index.** A producer of sets tests its own sets by
  merging them; a consumer receives one index and never folds, ranks, or
  mounts. The same sets under the same order and precedence give the same
  index anywhere.
- **Guard Argument is the conversion boundary.** The consumer converts its
  runtime value into one Guard Argument; the index compares. Neither side
  knows the other's value type.
- **Statement, not branch.** A consumer reads an Effective Statement and
  never a branch: which branches contributed is not observable, only the
  combined sections are.
- **Generated ≡ written.** After the merge, no reader can tell a generated
  set's section, a hand-written section, or a generator answer apart except
  through Section Precedence.

Rationale: [concept-index.md](concept-index.md). Algorithms:
[spec-index.md](spec-index.md), [spec-statement.md](spec-statement.md).
Software structure: [design-index.md](design-index.md).
