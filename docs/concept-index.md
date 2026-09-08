# PHP Models — Model Index Concept

Extends [concept.md](concept.md) with the merge of several document sets
into one queryable whole. Document Set, Model, Section, Matching Subject,
Model Generator, and When Guard are defined there; Set Provenance,
Verification, and Precedence in [concept-provenance.md](concept-provenance.md).
All are used here unchanged.

## 1. Context

**Problem Statement**
A consumer mounts several document sets: a generated declaration set, a
reviewed correction set, third-party fragments. Two sets can state the
same section for one subject, a subject can carry guarded branches spread
over sets, and every name must be checked against the vocabulary all sets
declare together. Left to consumers, each one re-implements the merge, the
per-section replacement, branch grouping, and guard evaluation, and a data
library has no way to check that the sets it ships merge cleanly.

**System Role**
The Model Index is the library's merged view over an ordered list of
document sets: one read-only whole that answers, per subject, which
signature, flows, sources, sinks, and sanitizers are in force, branch by
branch. Guard evaluation over scalar arguments is part of it. What a
consumer knows about a call, and what it does with an undecided guard,
stays with the consumer.

**Data Flow**
- **Inputs:** loaded Document Sets in the consumer's mount order; one
  Precedence.
- **Outputs:** one Model Index: the accumulated vocabulary and policy, and
  for every subject its effective statement per section as declared
  branches; guard verdicts on request.
- **Connections:** producers → document sets → this library (load, merge)
  → Model Index → consumer queries by subject.

**Scope Boundaries**
- **Owned:** the merge rule, the section as the unit of replacement,
  branch retention by guard, the guard argument scalar and guard
  evaluation, generator answers where no explicit entry declares a
  section, vocabulary accumulation, and generator name uniqueness.
- **Not Owned:** which sets a consumer mounts and in what order; turning a
  consumer's run-time values into guard arguments; computing a call's
  result.

## 2. Concepts

**Conceptual Diagram**
```
set 1 (generated) ─┐                       ┌─ subject ─┬─ default branch ─┬─ signature
set 2 (manual) ────┤─► merge by precedence ─┤           │                  ├─ flows
set 3 (manual) ────┘   per subject, guard,  │           │                  ├─ sources / sinks / sanitizers
                       and section          │           └─ guarded branch ─┴─ (same sections)
                                            └─ vocabulary + policy
consumer: subject + guard arguments ─► verdicts ─► candidate set ─► effective statement per section
```

**Core Concepts**

- **Name:** Model Index
- **Definition:** The merged, read-only whole over the mounted sets. Built
  once from the ordered sets and a precedence; answered by subject.
- **Scope:** one per merge; every subject any set or generator states.
- **Relationships:** built from Document Sets; applies Section Precedence;
  holds Branches; consulted by consumers.

- **Name:** Section Precedence
- **Definition:** The replacement rule: for one subject, one guard, and one
  section, the statement from the set whose verification ranks highest
  wins; equal ranks fall to the later-mounted set. A set silent about a
  section leaves the lower set's statement in force. Two statements are
  never unioned.
- **Scope:** the five sections; the signature is one of them.
- **Relationships:** reads Set Provenance through the Precedence; applied
  by the Model Index at build.

- **Name:** Branch
- **Definition:** The entries for one subject that share one guard: the
  unguarded entries form the default branch, and each distinct guard forms
  one guarded branch. Each branch is resolved by Section Precedence on its
  own; how a guarded branch combines with the default at a call is fixed by
  [model-guards.md](model-guards.md).
- **Scope:** every subject with at least one entry.
- **Relationships:** part of the Model Index; carries a When Guard or none.

- **Name:** Guard Argument
- **Definition:** What a consumer states about one argument position when
  it asks: a string, an integer, a boolean, or unknown. A library scalar;
  no run-time value type of any consumer crosses the boundary.
- **Scope:** one per argument position the consumer names; unnamed
  positions are unknown.
- **Relationships:** compared against a When Guard; produces a Guard
  Verdict.

- **Name:** Guard Verdict
- **Definition:** The outcome of one guard against the guard arguments:
  holds when the named position carries the guard's value, fails when it
  carries another value or the call does not supply the position,
  undecided when it is unknown.
- **Scope:** one per guarded Branch per query.
- **Relationships:** selects the Candidate Set; defined with it in
  [model-guards.md](model-guards.md).

- **Name:** Effective Statement
- **Definition:** The answer for one subject, one section, and one set of
  guard arguments: the Candidate Set of [model-guards.md](model-guards.md)
  combined per section — sinks and sources unioned, sanitizers reduced to
  the categories every candidate neutralizes, value semantics joined to
  the common return with the flows unioned. Under wholly unknown
  arguments this is the sound union over every branch; a consumer never
  picks a branch itself.
- **Scope:** one per query; none when no candidate declares the section.
- **Relationships:** computed by the Model Index from Branches and Guard
  Verdicts; the only form in which a consumer reads a guarded subject.

- **Name:** Generator Answer
- **Definition:** The statement a Model Generator supplies for a section of
  a subject that satisfies its constraints, when no explicit entry of any
  set declares that section for the subject.
- **Scope:** explicit entries always win over generators; generators never
  contribute a signature.
- **Relationships:** part of the Model Index; derived from Model
  Generators.

## 3. Contracts & Flow

**Data Contracts**
- **With producers:** nothing changes; a set is stored and validated as
  before. A producer that ships several sets can merge them in its own
  tests, so a set that cannot merge — an undeclared name, a conflicting
  redeclaration, a repeated generator name — fails at the producer.
- **With consumers:** the consumer hands over loaded sets in order and a
  precedence and receives one index. Every answer is in interned names;
  every guarded subject is answered as an Effective Statement for the
  consumer's guard arguments, never as branches for the consumer to choose
  among. The branches stay readable for tools that display data.

**Internal Processing Flow**
1. Load — resolve each set in order against the vocabulary accumulated so
   far; append its policy rows.
2. Group — entries by subject, then by guard into Branches.
3. Replace — per Branch and section, keep the statement Section Precedence
   selects.
4. Register — generators, with unique names.
5. Freeze — the index is immutable from here on.
6. Answer — for a subject, section, and guard arguments: evaluate each
   guard, form the Candidate Set, combine it into the Effective
   Statement; a Generator Answer where no Branch declares the section.

## 4. Scenarios

- **Typical:** the generated set declares `strlen` with a signature and a
  flow; the reviewed set restates the flow. The reviewed flow is in force,
  the generated signature stays.
- **Guarded:** `print_r` has a default branch and a branch guarded on the
  second argument being true. With the argument known true the guarded
  value semantics answer alone; with it unknown the answer joins both:
  an unknown return with the union of their flows.
- **Boundary:** two manual sets both declare sinks for one subject. The
  later-mounted set's sinks are in force alone; the lists are not unioned.
- **Interaction:** a data library merges the four sets it ships in its
  build test. A correction entry that names an undeclared category fails
  there, not in a consumer.

Domain semantics: [model-guards.md](model-guards.md), [model-sets.md](model-sets.md).
Software structure: [design-index.md](design-index.md).
