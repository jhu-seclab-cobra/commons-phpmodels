# PHP Models — Concept

## Context

**Problem Statement.** Static analyses of PHP need declarative knowledge about
declarations they cannot see the body of: what a call returns, how values flow
through it, which declarations produce untrusted data, consume it dangerously,
or neutralize it, and what the declaration itself looks like (parameters,
types, class structure). Several producers emit this knowledge — a generated
layer extracted from upstream stub sources, hand-written rule files, and
third-party fragments — and several consumers read it. Without one shared
format and one shared validation implementation, each producer/consumer pair
re-invents both.

**System Role.** commons-phpmodels is the format library: it owns the model
types, the YAML decoding, and the load-time validation. It contains no
analysis — no lookups over graphs, no taint query, no layer mounting. Those
belong to consumers.

**Data Flow**
- **Inputs:** YAML documents — vocabulary declarations, policy rows, model
  entries (explicit models and generators).
- **Outputs:** validated typed values — `Vocabulary`, `TaintPolicy`, decoded
  model entries.
- **Connections:** producers (stub extraction, rule authors, third-party
  fragments) → this library (decode + validate) → consumers (analyzers,
  compilers).

**Scope Boundaries**
- **Owned:** the model format — subjects, ports, sections, signatures, guards,
  generators, vocabulary, policy; strict YAML decoding; every load-time
  validation rule of the format itself.
- **Not Owned:** layer ordering and override resolution, branch selection at a
  call, subject resolution from a program graph, the taint query, artifact
  compilation, and any file organization convention of a particular consumer.

## Concepts

```
vocabulary ──┐
policy ──────┤──► decode + validate ──► typed values ──► consumer
model files ─┘        (this library)
```

**Model** — The single declarative statement about one subject: what its calls
return, how values flow through them, its taint effects, and what the
declaration looks like. Composed of sections, at least one present.
- Scope: one model per entry; includes assertion sections and the descriptive
  signature section.
- Relationships: identifies exactly one Matching Subject; validated whole at
  decode.

**Section** — One aspect of a model: *returns*, *propagation*, *sources*,
*sinks*, *sanitizers*, or *signature*. An absent section asserts nothing about
that aspect, with one coupling: *returns* and *propagation* together form the
value-semantics statement — declaring *returns* asserts the flow set
exhaustively, so an absent propagation section then means no flow.

**Port** — An explicitly named location in a call: one argument position
(`argument(n)`) or the call result (`return`). Every positional reference in a
model is a port; the meaning is in the port name, never in a surrounding field
name.

**Propagation** — One declared flow from one port to another: an argument to
the result, or an argument to another argument. Written as a `from`/`to` port
pair; the Mariana Trench spellings `input`/`output` are accepted synonyms.

**Matching Subject** — The PHP declaration a model identifies. Seven kinds —
function, class, method, class constant, property, constant, predefined
variable — spelled with PHP's own static-reference grammar
(`mysqli::query`, `mysqli::$insert_id`). The kind is named by the entry key;
the spelling encodes identity only.

**Signature** — The descriptive section: what the declaration looks like,
never what an analysis believes about it. A signature-only entry is a complete
entry — it states "this declaration exists".

**Model Generator** — One entry declaring one model body over every subject
satisfying its constraints: a unique name, the subject kind to find, name
constraints, and the model to attach. The only entry form with a wrapper
level, separating what is matched from what is asserted.

**When Guard** — An optional condition on a model entry: an argument port
equals one scalar value. Entries for one subject form branches; the unguarded
entry is the default. Selection at a call is consumer behavior; the guard's
meaning is fixed here.

**Origin Color** — A named category of untrusted provenance. Travels with a
value along data flow.

**Danger Category** — A named category of vulnerability a sink is sensitive
to. Sink-side only.

**Vocabulary** — The closed, declared sets of origin colors and danger
categories. Any color or category named anywhere else must be declared here
first.

**Policy** — The global mapping from an origin color to the danger categories
it can trigger. One matrix per analysis.

## Contracts & Flow

**Data Contracts**
- **With producers:** producers emit YAML in this format; a malformed entry is
  a decode failure at the producer's build or the consumer's load — never a
  silent miss. One validation implementation serves every caller.
- **With consumers:** consumers receive only validated typed values. Interned
  identifiers (`VulnClassId`, `ProvenanceId`) replace raw strings past the
  load boundary, so a name mismatch cannot occur downstream.

**Internal Processing Flow**
1. Decode — strict YAML decoding: unknown keys, unknown discriminators,
   missing fields, and rejected values all fail.
2. Validate — construction-time invariants on every type: closed grammars,
   closed vocabularies, non-empty sections, coupling rules.
3. Intern — vocabulary names become identity tokens; references are checked
   against the declared sets.

## Scenarios

- **Typical — add a sink.** A rule author declares one model naming the
  function, with a sinks section listing the dangerous argument port under a
  declared danger category. No code change in any consumer.
- **Boundary — malformed spelling.** An entry spelling a method subject with
  two `::` separators, or a property without its `$`, fails the decode with
  the entry named. The closed grammar admits no guess.
- **Interaction — generated layer.** A stub-extraction producer emits
  signature-only entries for tens of thousands of builtin declarations. The
  same decode-and-validate path a consumer uses at runtime validates the
  generated files at the producer's build.

Domain semantics: [model.md](model.md),
[model-declarations.md](model-declarations.md),
[model-guards.md](model-guards.md). Software structure:
[design.md](design.md).
