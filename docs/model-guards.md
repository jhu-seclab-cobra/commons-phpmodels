# PHP Models — Conditional Models (When Guard)

Domain semantics of guarded model branches. Base entities — Model, Matching
Subject, Port, Override Unit: [model.md](model.md).

## Entities

- **When Guard** — An optional condition attached to one model entry: one
  argument Port and one required scalar value (boolean, integer, or string).
  The guard holds at a call exactly when the argument at that port is that
  value. Identity is the pair (port, value). Equality is the only predicate.
- **Branch** — One model entry for a subject, together with its guard status:
  a *guarded branch* carries a When Guard; the *default branch* carries none.
  Each branch is a complete model — its sections describe the subject's whole
  behavior under that condition, never a delta over another branch.
- **Guard Verdict** — The evaluation of one When Guard at one call: *holds*
  (the argument is statically that value), *fails* (the argument is
  statically a different value, or the call does not supply the port), or
  *undecided* (the argument's value or the call's arity is not statically
  known).
- **Candidate Set** — The branches that may govern one call: the branches
  undecided before the first held guard, plus that held branch — or, when
  no guard holds, every undecided branch plus the default branch when one
  exists.
- **Guard Context** — What a consumer knows about one call's arguments: the
  argument count when known, and per position the scalar value when every
  interpretation of that argument is one concrete scalar. The *unknown
  context* knows neither; under it every guard is undecided.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Matching Subject | Branch | branches into | 1:N | Guarded branches in declaration order, plus at most one default branch |
| When Guard | Port × scalar | tests | 1:1 | Holds iff the argument at the port is the scalar |
| Branch | Model | asserts | 1:1 | The complete model in force when the branch governs |
| Call | Candidate Set | selects | 1:N | The branches that may govern, per the guard verdicts |

## State Model

### Guard Verdict at One Call

| Verdict | Condition |
|---------|-----------|
| fails | Arity known and port position ≥ arity (defaulted parameter) |
| fails | Argument's value known and ≠ the guard's scalar |
| holds | Argument's value known and = the guard's scalar |
| undecided | Arity unknown, or the argument's value not statically one scalar |

### Branch Selection at One Call

The Candidate Set entity fixes which branches may govern; the verdicts above
drive it. One candidate → its model applies exactly. Several candidates → the units
combine per direction:

| Unit | Combination over candidates | Direction |
|------|----------------------------|-----------|
| sinks | union of declared sink elements | over-approximate danger |
| sources | union of produced color sets | over-approximate danger |
| sanitizers | intersection of neutralized categories; a candidate without the unit neutralizes nothing | never trust a maybe-inactive neutralizer |
| value semantics | answered only when every candidate declares it: returns joined (equal → itself, else unknown classification), propagations unioned | over-approximate flow |

Selection and combination run in the consumer; the semantics above are the
format's meaning of a guard, fixed here so every consumer interprets guarded
branches identically.

## Invariants

- The guard is optional. A subject with only a default branch has that
  branch's model in force at every call.
- A When Guard is declared only on a callable subject (function, method) and
  only on the model entry form. A guard on a non-callable subject, and a
  guard on a generator, are load failures.
- The guard's port is an argument port. The return port, the receiver
  port, and any malformed port, are load failures. The compared value is
  exactly one scalar:
  boolean, integer, or string; any other value shape is a load failure.
- Every branch satisfies every model invariant of [model.md](model.md)
  independently; no branch is validated against another.
- At most one effective declaration exists per (subject, guard, unit): the
  override unit key gains the guard, with "no guard" as the default branch's
  key.
- Guarded branches keep first-declaration order across layers; overriding a
  (subject, guard, unit) replaces the unit in place, never reorders.
- Model generators declare no guard; a generated body serves in the default
  branch's role, per unit, only where no explicit branch declares that unit.
- An unsupplied guard port fails the guard; the default branch therefore
  describes the subject's behavior with defaulted arguments.
- Under the unknown guard context every guarded branch is a candidate beside
  the default branch — the selection degrades to the sound union, never to
  one arbitrary branch.
- A verdict never depends on evaluation order beyond the declared branch
  order: the first held guard wins, and undecided branches collected before
  it remain candidates.

## Cross-Structure Contracts

- **Guard context is the consumer's knowledge, not the model's.** A model
  states conditions; how precisely a call's arguments are known is a
  property of the consuming phase. The same branches answer differently
  under different contexts, and the unknown context is always admissible.
- **Branch ≡ complete model.** No section of one branch leaks into another:
  selecting a branch answers every unit from that branch (or the combination
  rule above), never from a sibling.

Rationale: [concept.md](concept.md). Software structure:
[design-guards.md](design-guards.md).
