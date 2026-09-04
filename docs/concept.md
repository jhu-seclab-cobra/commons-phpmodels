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
re-invents both. Without one shared organization of the documents, each
side also re-invents the loading convention, and a consumer mounting a
source that names categories on another axis has nowhere to translate them.

**System Role.** commons-phpmodels is the format library — it owns the
model types, the YAML decoding, and the load-time validation, while every
analysis (graph lookups, taint queries, layer mounting) belongs to
consumers.

**Data Flow**
- **Inputs:** YAML documents — vocabulary declarations, policy rows, model
  entries (explicit models and generators) — grouped into document sets
  under one root each; optionally a category mapping for one set.
- **Outputs:** validated typed values — the vocabulary, the taint policy,
  decoded model entries, and one loaded document set per root.
- **Connections:** producers (stub extraction, rule authors, third-party
  fragments) → this library (decode + validate) → consumers (analyzers,
  compilers).

**Scope Boundaries**
- **Owned:** the model format — subjects, ports, sections, signatures, guards,
  generators, vocabulary, policy; strict YAML decoding; every load-time
  validation rule of the format itself; the document-set convention shared
  by every producer and consumer; the category mapping that translates one
  set's categories and colors into a consumer's vocabulary.
- **Not Owned:** layer ordering and override resolution, branch selection at a
  call, subject resolution from a program graph, the taint query, artifact
  compilation, and where a document set is stored (classpath, file,
  artifact) — the caller opens the streams.

## Concepts

```
document set root ─┬─ index.txt ──► model files ─┐
                   ├─ vocabulary.yaml ───────────┤──► decode + validate ──► typed values ──► consumer
                   └─ policy.yaml ───────────────┘        (this library)          ▲
category mapping (consumer-supplied) ── translates a set's names ──────────────────┘
```

**Model** — The single declarative statement about one subject: what its calls
return, how values flow through them, its taint effects, and what the
declaration looks like. Composed of sections, at least one present.
- Scope: one model per entry; includes assertion sections and the descriptive
  signature section.
- Relationships: identifies exactly one Matching Subject; validated whole at
  decode.

**Section** — One aspect of a model: *returns*, *propagation*, *sources*,
*sinks*, *sanitizers*, or *signature*.
- Scope: an absent section asserts nothing about that aspect; *returns* and
  *propagation* form one coupled value-semantics statement
  ([model.md](model.md)).
- Relationships: composes a Model; its positional references are Ports.

**Port** — An explicitly named location in a call: one argument position
(`argument(n)`), the receiver of a method call (`this`), or the call result
(`return`).
- Scope: every positional reference in a model; the meaning is in the port
  name, never in a surrounding field name.
- Relationships: named by Propagations, source elements, sink elements, and
  When Guards.

**Propagation** — One declared flow from one input port — an argument or the
method receiver — to another port of the same call.
- Scope: written as a `from`/`to` port pair.
- Relationships: an element of a Model's propagation section; connects two
  Ports.

**Matching Subject** — The PHP declaration a model identifies.
- Scope: seven kinds — function, class, method, class constant, property,
  constant, predefined variable — spelled with PHP's own static-reference
  grammar (`mysqli::query`, `mysqli::$insert_id`); the entry key names the
  kind, the spelling encodes identity only.
- Relationships: identified by exactly one Model; found by Model Generators.

**Signature** — The descriptive section: what the declaration looks like,
never what an analysis believes about it.
- Scope: a signature-only entry is a complete entry — it states "this
  declaration exists".
- Relationships: one Section of a Model; describes the Matching Subject.

**Model Generator** — One entry declaring one model body over every subject
satisfying its constraints.
- Scope: a unique name, the subject kind to find, name constraints, and the
  model to attach; the only entry form with a wrapper level, separating what
  is matched from what is asserted.
- Relationships: denotes one Model per satisfying Matching Subject.

**When Guard** — An optional condition on a model entry: an argument port
equals one scalar value.
- Scope: entries for one subject form branches, the unguarded entry being
  the default; the guard's meaning is fixed here, selection at a call is
  consumer behavior.
- Relationships: attaches to a Model entry; names one Port.

**Origin Color** — A named category of untrusted provenance.
- Scope: travels with a value along data flow.
- Relationships: declared in the Vocabulary; produced by sources; mapped by
  the Policy.

**Danger Category** — A named category of vulnerability a sink is sensitive
to.
- Scope: sink-side only.
- Relationships: declared in the Vocabulary; named by sinks and sanitizers;
  enabled by the Policy.

**Vocabulary** — The closed, declared sets of origin colors and danger
categories.
- Scope: any color or category named anywhere else must be declared here
  first. A second declaration of a name is admitted only when identical to
  the first: two sets can restate a shared name, never silently disagree.
- Relationships: referenced by every Model and by the Policy; accumulated
  across Document Sets by the consumer.

**Policy** — The global mapping from an origin color to the danger categories
it can trigger.
- Scope: one matrix per analysis.
- Relationships: relates Origin Colors to Danger Categories; validated
  against the Vocabulary.

**Document Set** — One source of models as it is stored: a root holding a
manifest that lists the model documents in order, an optional vocabulary,
and an optional policy, under fixed file names.
- Scope: the unit a producer publishes and a consumer mounts; one root, one
  manifest. Its layer position and precedence are the consumer's.
- Relationships: contains Models, at most one Vocabulary and one Policy;
  loaded whole; the target of at most one Category Mapping.

**Category Mapping** — A consumer-supplied translation from the danger
categories and origin colors a Document Set names to the names in the
consumer's own vocabulary.
- Scope: total over the set — every name the set uses is either mapped to
  a declared name or explicitly discarded; an unlisted name fails the load.
  A mapped set contributes no vocabulary of its own: its entries reach the
  consumer already spelled in the consumer's names.
- Relationships: applied to one Document Set at load; targets the
  consumer's Vocabulary. Naming rule: names are never renamed to match a
  source; a source naming sinks by the medium written to rather than the
  vulnerability enabled keeps its names, and the consumer translates.

## Contracts & Flow

**Data Contracts**
- **With producers:** producers emit YAML in this format, organized as
  document sets; a malformed entry is a decode failure at the producer's
  build or the consumer's load — never a silent miss. One validation
  implementation and one loading convention serve every caller.
- **With consumers:** consumers receive only validated typed values.
  Interned identifiers replace raw color and category names past the load
  boundary, so a name mismatch cannot occur downstream
  ([design.md](design.md)).

**Internal Processing Flow**
1. Decode — strict YAML decoding: unknown keys, unknown discriminators,
   missing fields, and rejected values all fail.
2. Validate — construction-time invariants on every type: closed grammars,
   closed vocabularies, non-empty sections, coupling rules.
3. Intern — vocabulary names become identity tokens; references are checked
   against the declared sets.
4. Resolve a set — read the manifest, load the set's vocabulary against the
   caller's accumulated vocabulary, then its policy, then its documents in
   manifest order; apply the category mapping when one is supplied.

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
- **Interaction — mapped upstream taint set.** A producer publishes a
  document set whose sinks are categorized by the medium they write to
  (`sql`, `html`, `shell`). A consumer whose vocabulary names vulnerability
  classes (`sqli`, `xss`, `cmdi`) mounts the set with a category mapping.
  The set's entries arrive in the consumer's names; a new upstream medium
  absent from the mapping fails the load instead of vanishing.
- **Boundary — conflicting redeclaration.** Two sets both declare `sqli`;
  one describes it differently. The load fails naming the set and the name.

Domain semantics: [model.md](model.md),
[model-declarations.md](model-declarations.md),
[model-guards.md](model-guards.md). Software structure:
[design.md](design.md).
