# Entries and Bodies

> Decoded model entries: flat subject models, generators, and the sectioned assertion body.

## Quick Start

```kotlin
val entries = ModelLoader.load(yaml.byteInputStream())
for (entry in entries) {
    when (entry) {
        is SubjectModel -> entry.subject to entry.body
        is ModelGenerator -> entry.name to entry.model
    }
}
```

## API

- **`ModelEntry`** — Sealed supertype of the two entry forms.
- **`SubjectModel(subject: ModelSubject, guard: WhenGuard?, signature: SignatureInfo?, body: ModelBody)`** — One flat entry: a named subject with its assertions.
- **`ModelGenerator(name: String, find: SubjectKind, where: List<SubjectConstraint>, model: ModelBody)`** — Pattern entry; `matches(subject: ModelSubject): Boolean` tests one candidate. `find` is `FUNCTION`, `METHOD`, or `VARIABLE`; constraints are `name`/`class` regex patterns, matched entire-field and case-insensitively against the folded identity.
- **`ModelBody`** — Five optional sections: `returns: ReturnKind?`, `propagation: List<Propagation>?`, `sources: List<SourceDecl>?`, `sinks: List<SinkPoint>?`, `sanitizers: List<SanitizerDecl>?`. `isEmpty: Boolean`; `valueSemantics(): ValueSemantics?` returns null when `returns` is undeclared.
- **`ValueSemantics(returns: ReturnKind, propagation: List<Propagation>)`** — The whole value unit; absent propagation section decodes to the empty list (no flow, not unknown flow).
- **`Propagation(from: Port.Input, to: Port)`** — One declared flow; file spellings `from`/`input` and `to`/`output` (one per side). The from side is input-typed: `return` as a from side fails to decode.
- **`Port`** — Sealed: `Port.Argument(position: Int)` (spelling `argument(n)`, `n >= 0`), `Port.Receiver` (spelling `this`), `Port.Return` (spelling `return`). `Port.Input` is the sealed sub-interface of `Argument` and `Receiver`. `Port.parse(raw: String): Port`.
- **`WhenGuard(port: Port.Argument, value: GuardValue)`** — Equality guard on one argument; bool, int, or string scalar.
- **`SourceDecl(provenance: Set<ProvenanceId>, at: Port.Argument?, keys: List<KeyPattern>?)`** — Non-empty produced color set; optional explicit production site (`at:`, a by-ref out-parameter) and non-empty key-pattern list (`keys:`).
- **`KeyPattern(pattern: String)`** — Regex over array keys, matched entire-key and case-sensitively; `matches(key: String): Boolean`. Decoded from a bare string scalar.
- **`SinkPoint(port: Port.Argument, category: VulnClassId)`** — One dangerously consumed argument under one category.
- **`SanitizerDecl(categories: Set<VulnClassId>)`** — Non-empty neutralized category set.
- **`ReturnKind`** — Enum `STR`, `NUM`, `BOOL`, `ANY`; `join(other: ReturnKind): ReturnKind` gives itself when equal, `ANY` otherwise.

## Configuration

- Entry form is deduced from keys: `subject:` starts a flat model; `name:`/`find:`/`where:`/`model:` starts a generator. No `type:` discriminator.
- Generator `name` uniqueness spans one whole load across layers; `ModelLoader` does not enforce it — check it in the caller.

## Gotchas

- An entry asserting nothing — no signature, empty body — is rejected at load.
- `propagation` without `returns` is rejected; the value unit is asserted whole. With a callable `signature`, `returns` is derived from the declared return type instead.
- Explicit `returns:` beside a callable `signature:` is rejected — one fact, one source.
- A callable `signature:` fixes the arity: an argument port (guard, propagation side, sink, source `at:` site) at a position outside the declared parameter list is rejected, unless the last parameter is variadic. An entry without a signature is not arity-checked.
- With a callable `signature:`, a written-into argument port — a propagation `to:` side or a source `at:` site — must name a `byRef: true` parameter; a variadic-tail position resolves to the variadic parameter's flag.
- With a callable `signature:` whose `returnType` is `void`, a propagation into `return` is rejected.
- A `when:` guard on a non-callable subject is rejected.
- The receiver port `this` anywhere in the body requires a `method` subject (or a generator with `find: method`).
- A source `at:` site requires a callable subject; a variable entry or `find: variable` generator declaring one is rejected.
- Section admissibility follows subject kind: `returns`/`propagation` and `when:` belong to `function` and `method` entries; `class` entries declare nothing besides their signature; other non-callable kinds declare sources only.
- Multiple entries per subject are branches in declaration order; an unguarded entry is the default branch. Branch selection is the consumer's job.
