# PHP Models — Concept

## Context

**Problem Statement.** Static analyses of PHP need declarative knowledge about
declarations they cannot see the body of: what a call returns, how values flow
through it, which declarations produce untrusted data, consume it dangerously,
or neutralize it, and what the declaration itself looks like (parameters,
types, class structure). Several producers emit this knowledge — a Generated Layer extracted from upstream stub sources, hand-written rule files, and
third-party fragments — and several consumers read it. Without one shared
format and one shared validation implementation, each producer/consumer pair
re-invents both. Without one shared organization of the documents, each
side re-invents the loading convention, and a consumer mounting a
source that spells its Vulnerability type entries and Source kind entries differently has
nowhere to translate them.

**System Role.** commons-phpmodels is the format library — it owns the
model types, the YAML decoding, and the load-time validation of one
Document Set, while every use of the decoded sets (merging them, lookup by
name, graph resolution, taint queries) belongs to consumers.

**Data Flow**
- **Inputs:** YAML documents — vocabulary declarations, policy rows, model
  entries — grouped into Document Set instances under one root each; optionally a
  Vocabulary Mapping for one set; scalar values from commons-value where a
  document states a literal.
- **Outputs:** validated typed values — the vocabulary, the taint policy,
  decoded model entries, and one loaded Document Set per root.
- **Connections:** producers (stub extraction, rule authors, third-party
  fragments) → this library (decode + validate) → consumers (analyzers,
  compilers).

**Scope Boundaries**
- **Owned:** the model format — subjects, ports, sections, signatures,
  conditions, vocabulary, policy; strict YAML decoding; every load-time
  validation rule of the format itself; the document-set convention shared
  by every producer and consumer; the Vocabulary Mapping that translates one
  set's names into a consumer's; the Set Provenance each set declares and
  the precedence over verification kinds ([concept-provenance.md](concept-provenance.md));
  the one operation that matches a condition against a list of argument
  values.
- **Not Owned:** merging sets (which entry is in force per subject,
  condition, and section), deciding what a call's arguments are, subject
  resolution from a program graph, the taint query, artifact compilation,
  and where a Document Set is stored (classpath, file, artifact) — the
  caller opens the streams.

## Concepts

```
document set root ─┬─ index.txt ──► model files ─┐
                   ├─ vocabulary.yaml ───────────┤──► decode + validate ──► typed values ──► consumer
                   ├─ policy.yaml ───────────────┤        (this library)          ▲
                   └─ provenance.yaml ───────────┘                                │
vocabulary mapping (consumer-supplied) ── translates a set's names ──────────────────┘
```

**[Model](glossary/glossary-models.md#model)** — Role: Groups the statements a consumer reads for one Matching Subject.
- Scope: one model per entry; includes assertion sections and the descriptive
  signature section.
- Relationships: identifies exactly one Matching Subject; validated whole at
  decode.

**[Section](glossary/glossary-models.md#section)** — Role: Separates independently declared aspects of a Model.
- Scope: omission follows the coupling invariants of [the model](model.md#invariants); *returns*,
  *propagation*, and *characters* form one coupled statement, the Value unit
  ([model.md](model.md)).
- Relationships: composes a Model; its positional references are Ports.

**[Port](glossary/glossary-models.md#port)** — Role: Names the call locations referenced by behavior Sections.
- Scope: every positional reference in a model; the meaning is in the port
  name, never in a surrounding field name.
- Relationships: named by Propagations, source elements, and sink elements.

**[Propagation](glossary/glossary-models.md#propagation)** — Role: Connects the call locations whose values flow together.
- Scope: written as a `from`/`to` port pair.
- Relationships: an element of a Model's propagation section; connects two
  Ports.

**[Matching Subject](glossary/glossary-models.md#matching-subject)** — Role: Identifies one PHP declaration or the dynamic invocation operation.
- Scope: seven declaration kinds — function, class, method, class constant, property,
  constant, predefined variable — spelled with PHP's own static-reference
  grammar; the additional dynamic invocation operation admits only unconditional callee sinks. The entry key names the
  kind, the spelling encodes identity only.
- Relationships: identified by one Model per Condition.

**[Signature](glossary/glossary-models.md#signature)** — Role: Supplies declaration information without inferring analysis behavior.
- Scope: a signature-only entry is a complete entry — it states "this
  declaration exists".
- Relationships: one Section of a Model; describes the Matching Subject.

**[Condition](glossary/glossary-models.md#condition)** — Role: Selects the calls to which a Model applies.
- Scope: scalar equality at constrained argument positions; omitted positions use declared scalar defaults in alternatives matching. The consumer decides how to use undecidable outcomes.
- Relationships: attaches to a Model entry; an entry without one is the
  subject's unconditional statement; several entries for one subject are
  alternatives.

**[Source kind](glossary/glossary-vocabulary.md#source-kind)** — Role: Distinguishes categories of untrusted input.
- Scope: travels with a value along data flow.
- Relationships: declared in the Vocabulary; produced by sources; mapped by
  the Policy.

**[Vulnerability type](glossary/glossary-vocabulary.md#vulnerability-type)** — Role: Distinguishes sink sensitivities.
- Scope: sink-side only.
- Relationships: declared in the Vocabulary; named by sinks and sanitizers;
  enabled by the Policy.

**[Vocabulary](glossary/glossary-vocabulary.md#vocabulary)** — Role: Checks that referenced names are declared.
- Scope: any Source kind or Vulnerability type named anywhere else must be declared here
  first. A second declaration of a name is admitted only when identical to
  the first: two sets can restate a shared name, never silently disagree.
- Relationships: referenced by every Model and by the Policy; accumulated
  across Document Set instances by the consumer that loads them in sequence.

**[Policy](glossary/glossary-vocabulary.md#policy)** — Role: Relates source categories to sink sensitivities.
- Scope: one matrix per analysis.
- Relationships: relates Source kind entries to Vulnerability type entries; validated
  against the Vocabulary.

**[Document Set](glossary/glossary-sets.md#document-set)** — Role: Groups documents loaded from one producer root.
- Scope: the unit a producer publishes and a consumer mounts; one root, one
  manifest. Its position among sets is the consumer's; its rank against
  another set follows its Set Provenance ([concept-provenance.md](concept-provenance.md)).
- Relationships: contains Models, at most one Vocabulary, one Policy, and
  one Set Provenance; loaded whole; the target of at most one Vocabulary Mapping.

**[Vocabulary Mapping](glossary/glossary-sets.md#vocabulary-mapping)** — Role: Translates a producer vocabulary at the load boundary.
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
  Document Set instances; a malformed entry is a decode failure at the producer's
  build or the consumer's load — never a silent miss. One validation
  implementation and one loading convention serve every caller.
- **With consumers:** consumers receive only validated typed values.
  Normalized identifiers replace raw Source kind and Vulnerability type names past the load
  boundary, so a name mismatch cannot occur downstream
  ([design.md](design.md)).

**Internal Processing Flow**

Producers supply a Document Set to the loader and consumers receive validated entries; [DocumentSetLoader](design-sets.md#documentsetloader) owns the load sequence.

## Scenarios

- **Typical — add a sink.** A rule author declares one model naming the
  function, with a sinks section listing the dangerous argument port under a
  declared Vulnerability type. No code change in any consumer.
- **Boundary — malformed spelling.** An entry spelling a method subject with
  two `::` separators, or a property without its `$`, fails the decode with
  the entry named. The closed grammar admits no guess.
- **Interaction — Generated Layer.** A stub-extraction producer emits
  signature-only entries for tens of thousands of builtin declarations; the
  decode-and-validate path a consumer uses at runtime validates them at the producer's build.
- **Interaction — mapped upstream taint set.** A producer publishes a
  Document Set whose Vulnerability type entries name the medium a sink writes to
  (`sql`, `html`, `shell`). A consumer whose Vulnerability type entries name the
  attack (`sqli`, `xss`, `cmdi`) mounts the set with a Vocabulary Mapping.
  The set's entries arrive in the consumer's names; a new upstream medium
  absent from the mapping fails the load instead of vanishing.
- **Boundary — conflicting redeclaration.** Two sets both declare `sqli`;
  one describes it differently. The load fails naming the set and the name.

Domain semantics: [model.md](model.md), [model-declarations.md](model-declarations.md),
[model-conditions.md](model-conditions.md), [model-sets.md](model-sets.md). Software structure: [design.md](design.md).
