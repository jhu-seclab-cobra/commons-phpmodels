# PHP Models — Conditional Models (Condition)

Domain semantics of conditional model entries. Base entities — Model,
Matching Subject, Port, Override Unit: [model.md](model.md).

## Entities

- **Condition** — An optional argument pattern attached to one model entry:
  a sequence of expected values by argument position, each either one
  scalar (boolean, integer, float, string, null) or the *wildcard*. The
  condition holds at a call exactly when, for every position of the
  sequence, the call supplies an argument at that position and, unless the
  position is the wildcard, that argument is that scalar. Identity is the
  sequence. Equality is the only predicate.
- **Conditional Entry** — One model entry for a subject that carries a
  Condition. Its sections describe the subject's whole behavior under that
  condition, never a delta over another entry.
- **Unconditional Entry** — The model entry for a subject that carries no
  Condition. It describes the subject's behavior at every call the
  consumer does not attribute to a conditional entry.
- **Call Arguments** — What a consumer knows about one call: the argument
  count, and per position the scalar value when every interpretation of
  that argument is one concrete scalar, or *unknown* otherwise. Owned by
  the consumer; the format only defines the match against it.
- **Match Outcome** — The result of matching one Condition against one
  Call Arguments: *holds*, *fails*, or *undecidable*.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Matching Subject | Conditional Entry | has | 1:N | Alternatives; each a complete model for its condition |
| Matching Subject | Unconditional Entry | has | 1:0..1 | At most one per override unit |
| Condition | position × scalar | expects | 1:N | Position i carries scalar i, or anything when wildcard |
| Condition | Call Arguments | matches | N:N | Yields one Match Outcome |

## State Model

### Match Outcome

Positions are compared in order; the first decisive position decides.

| Outcome | Condition |
|---------|-----------|
| fails | The sequence is longer than the argument count |
| fails | Some non-wildcard position holds a known argument not equal to its scalar |
| holds | Every non-wildcard position holds a known argument equal to its scalar |
| undecidable | Neither: some non-wildcard position holds an unknown argument and no position fails |

The argument count is always known to the consumer: a call site lists its
arguments. A position beyond the count fails, so a condition on a
defaulted parameter never holds; the Unconditional Entry therefore
describes the subject with defaulted arguments.

## Invariants

- The condition is optional. A subject with only an Unconditional Entry has
  that entry in force at every call.
- A Condition is declared only on a callable subject (function, method).
  A condition on a non-callable subject is a load failure.
- A Condition sequence is non-empty and contains at least one non-wildcard
  position. An all-wildcard or empty sequence is a load failure.
- Every scalar in a Condition is one commons-value primitive; a list, map,
  or any other value shape is a load failure.
- Every entry satisfies every model invariant of [model.md](model.md)
  independently; no entry is validated against another.
- Two entries of one document for the same subject with equal Conditions,
  or both unconditional, are a load failure.
- Match Outcome depends only on the Condition and the Call Arguments; the
  format ranks no entry above another and orders no alternatives.
- What a consumer does with *undecidable* — keep the entry, drop it, fall
  back to the Unconditional Entry — is the consumer's rule per section
  kind and is not fixed by the format.

## Cross-Structure Contracts

- **Call Arguments are the consumer's knowledge, not the model's.** A model
  states conditions; how precisely a call's arguments are known is a
  property of the consuming phase. Every-position-unknown is always
  admissible and yields *undecidable* for every condition.
- **Entry ≡ complete model.** No section of one entry leaks into another:
  a consumer that attributes a call to an entry reads every unit from that
  entry, never from a sibling.
- **Merging is downstream.** Which entry is in force per (subject,
  condition, override unit) when several document sets state one subject
  is decided by the consumer that merges sets, by set provenance rank
  ([model-sets.md](model-sets.md)); the format contributes only the
  (subject, condition, unit) key.

Rationale: [concept.md](concept.md). Software structure:
[design-conditions.md](design-conditions.md).
