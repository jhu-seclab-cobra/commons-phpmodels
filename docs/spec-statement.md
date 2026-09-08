# PHP Models — Effective Statement Algorithm

Reading one subject's Effective Statement from the Model Index under a guard
context: candidate selection, per-section combination, and the generator
answer completing the default branch. Entity vocabulary:
[model-guards.md](model-guards.md), [model-index.md](model-index.md).
Index construction: [spec-index.md](spec-index.md).

## Algorithm: Statement Under a Guard Context

### Problem

Input: one subject's branch table (guarded branches in first-declaration
order, at most one default branch), the ordered generator list, and a guard
context (arity when known, one guard argument per position). Output: the
Effective Statement — for each of the four guarded sections, the combined
declaration or none. Correctness: when exactly one branch can govern the
call, its declaration is answered exactly; when several can, the answer
over-approximates every candidate in the section's sound direction; the
answer never reflects a branch whose guard statically fails; a generator
answer serves only where the default branch declares nothing.

The signature section is outside this algorithm: existence is read from the
default branch alone, with no guard context and no generator fallback.

### Steps

```
statement(table, subject, generators, context):
    candidates = []
    for branch in table.guarded, in declaration order:
        verdict = evaluate(branch.guard, context)
        if verdict = holds:      candidates.append(branch); break
        if verdict = undecided:  candidates.append(branch)
        # fails: skip
    if no guard held:
        default = complete(table.default, subject, generators)
        if default is not empty: candidates.append(default)
    return { section: combine(candidates, section) for section in guarded sections }

evaluate(guard, context):                       # guard: (port: argument(n), value: scalar)
    if context.arity known and guard.port.position ≥ context.arity: return fails
    v = context.argumentAt(guard.port.position) # a guard argument: scalar or unknown
    if v is unknown: return undecided
    return holds if admits(guard.value, v) else fails

admits(declared, actual):                       # one kind, one comparison
    boolean declared: actual is a boolean and equal
    integer declared: actual is an integer and equal
    string  declared: actual is a string and equal
    otherwise: false                            # a kind mismatch never holds

complete(default, subject, generators):         # generator answer per section
    for section in guarded sections:
        if default declares section: continue
        default[section] = generated(section, subject, generators)   # may be none
    return default

generated(section, subject, generators):
    if (section, subject) ∈ memo: return memo[(section, subject)]
    answer = none
    for gen in generators, last declared first:
        if section ∉ declaredSections(gen.body): continue
        if gen.findKind ≠ kindOf(subject): continue
        if satisfies(subject, gen.constraints):
            answer = gen.body[section]; break   # last declared wins
    memo[(section, subject)] = answer
    return answer

satisfies(subject, constraints):
    for c in constraints:
        field = the identity field c names       # name, or class of a method
        if not fullMatch(c.pattern, field): return false
    return true

combine(candidates, section):
    if candidates is empty: return none
    if |candidates| = 1:    return candidates[0][section]     # may be none
    declaring = [ b[section] for b in candidates if b declares section ]
    switch section:
        sinks:      return none if declaring empty else ⋃ declaring
        sources:    return none if declaring empty else ⋃ declaring
        sanitizers: neutralized = ⋂ over ALL candidates of
                        (b.sanitizers' categories, or ∅ if undeclared)
                    return none if neutralized = ∅ else neutralized
        value:      if some candidate does not declare value: return none
                    returns = join over declaring of returns   # equal → itself,
                    flows   = ⋃ over declaring of propagations #   else unknown
                    return (returns, flows)
```

### Edge Cases

- Under the unknown context every guard is undecided: the candidate set is
  all guarded branches plus the completed default. An unguarded-only
  subject then answers exactly its default branch.
- A guard on a position past a known arity fails — the parameter was
  defaulted, and the default branch describes defaulted-argument behavior.
- A held guard on the first branch yields one candidate: that branch's
  model applies exactly, including its absent sections answering none. No
  generator completes a guarded branch.
- Every guarded branch fails and the completed default is empty: the
  candidate set is empty and every section answers none.
- A named (non-positional) argument at the guard's position is an unknown
  guard argument: undecided, never resolved through parameter names.
- A concrete scalar of another kind than the guard declares fails the
  guard; no coercion is applied.
- The sanitizer intersection where any candidate lacks the section is
  empty, answering none: a neutralizer that may be inactive on this call
  never suppresses a finding.
- A generator whose find kind differs from the subject's kind never
  matches, even when a name pattern would.
- A pattern is matched against the case-folded identity, so it matches
  case-insensitively iff written in lowercase.
- The memo stores none too: a subject no generator satisfies is decided
  once per section.

### Invariants

- A branch whose guard fails contributes to no section's answer.
- With one candidate the answer equals that branch's own declaration —
  combination is the multi-candidate case only.
- The sinks and sources answers grow monotonically with the candidate set;
  the sanitizers answer shrinks monotonically. Adding uncertainty never
  removes a finding.
- The value answer is declared-exhaustive only when every candidate asserts
  value semantics.
- Evaluation reads the context only through arity and per-position guard
  arguments; no parameter name, default value, or callee body is consulted.
- The declared guard scalar and the guard argument are compared in exactly
  one place.
- A generator answer completes the default branch section by section and
  never overrides an explicit default-branch declaration, from any set.
- A memoized generator answer and a scanning one agree: tables, generators,
  and bodies are immutable after the merge.
- Every generated declaration is traceable to its generator's unique name.

### Termination

- One pass over finitely many branches; each section combination is one
  pass over the candidates; each generator miss is one bounded scan.

### Complexity

- O(B) guard evaluations over B branches, O(C · S) to combine C candidates
  with at most S elements per section, plus O(G · K) on the first
  generator miss per (section, subject) over G generators with at most K
  constraints; every later miss is one hash get.
