# PHP Models — Merge Algorithm

Turning an ordered list of document sets into one immutable Model Index.
Entity vocabulary: [model-index.md](model-index.md), [model-sets.md](model-sets.md).
Reading the index: [spec-statement.md](spec-statement.md).

## Algorithm: Merge Sets

### Problem

Input: an ordered list of loaded document sets (each already decoded and
verified by the set loader: entries, optional vocabulary, optional policy,
optional provenance; a mapped set arrives translated) and a precedence (a
total order over verification kinds). Output: the accumulated vocabulary,
the policy matrix, the generator list, and one branch table per subject
whose branches hold at most one declaration per section — five sections,
the signature among them — each declaration remembering which set declared
it. Correctness: every color or category an entry names is declared in the
vocabulary accumulated up to that set; a name declared twice carries one
description; per (subject, guard, section) the highest-ranked set's
declaration is in force and, among equal ranks, the later mounted; guarded
branches keep first-mount order; generator names are unique; the result is
immutable.

### Steps

```
merge(sets, precedence):
    vocab = ∅ ; rows = [] ; generators = [] ; mounted = []
    for (position, set) in sets, in order:
        vocab = accumulate(vocab, set.vocabulary)      # redeclaration must be identical
        for entry in set.entries: verify(entry, vocab) # undeclared name → failure
        rows += set.policy
        verification = set.provenance.verification or precedence.default
        for entry in set.entries:
            if entry is a generator:
                require entry.name ∉ names(generators) # duplicate → failure
                generators.append(entry)               # declaration order kept
            else:
                mounted.append((position, verification, entry))
    tables = fold(mounted, precedence)
    return frozen(vocab, matrix(rows), tables, generators)

fold(mounted, precedence):                             # per subject
    tables = {}
    for (position, verification, m) in mounted:        # mount order
        tables[m.subject].getOrCreate(m.guard)         # branches keep first-mount order
    for (position, verification, m) in mounted
            sorted by rank(precedence, verification) descending, stable:
        branch = tables[m.subject][m.guard]            # highest rank lands last
        for section in declaredSections(m):
            branch[section] = (m[section], position)   # per section, in place
    return tables

declaredSections(m):                                   # five sections
    { value      if m.body.returns declared            # returns + propagation
      sources    if m.body.sources declared
      sinks      if m.body.sinks declared
      sanitizers if m.body.sanitizers declared
      signature  if m.signature declared }
```

`accumulate` and `verify` are the vocabulary operations of
[model-sets.md](model-sets.md). Every format rule — entry forms, subject
spellings, ports, sections, guards, signatures, arity, mapping translation
and the dropping of elements left without a name, the value-semantics
completion of a propagation beside a callable signature — is applied by
the set loader before the merge and is not restated here.

### Edge Cases

- A vocabulary document declaring a name an earlier set declared with the
  same description is a no-op; with a different description it fails the
  merge. Declarations accumulate, they never replace.
- A mapped set's mapping targets were resolved by the loader against the
  vocabulary accumulated before it; its own vocabulary joins nothing.
- A set declaring one section for a subject another set declared a
  different section for overrides only its own section.
- Two entries for one subject with the same guard (or both unguarded)
  override per section by rank, then by mount order; a different guard
  opens a new branch, appended in first-mount order whatever the ranks.
- A set without a provenance document mounts as the precedence's default
  kind, manual under the default precedence.
- A generator declares no signature, so no generator ever answers the
  signature section ([spec-statement.md](spec-statement.md)).
- An empty list of sets merges into an empty index: no vocabulary, no
  policy, no tables, no generators. Every lookup answers unknown.

### Invariants

- After a set is mounted, every name that set's entries reference is
  interned exactly once in the accumulated vocabulary.
- `tables[subject][guard][section]` holds the declaration from the
  highest-ranked set that declared that triple, the last mounted among
  equal ranks; overriding one section leaves every other section and every
  other branch of the subject untouched and never reorders the guarded
  branches.
- `generators` keeps declaration order across sets and holds pairwise
  distinct names.
- Subjects are compared by the format's identity (folded per kind), the
  same identity a lookup key carries.
- The same sets in the same order under the same precedence merge into an
  equal index.

### Termination

- Finitely many sets, each with finitely many entries. One linear pass, no
  fixpoint.

### Complexity

- O(E log E) over entries E: one stable sort of the mounted entries, one
  hash lookup per reference check and one per override write.
