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
- **`ModelGenerator(name: String, find: SubjectKind, where: List<SubjectConstraint>, model: ModelBody)`** — Pattern entry; `matches(subject: ModelSubject): Boolean` tests one candidate. `find` is `FUNCTION`, `METHOD`, or `VARIABLE`; constraints are `name`/`class` regex patterns.
- **`ModelBody`** — Five optional sections: `returns: ReturnKind?`, `propagation: List<Propagation>?`, `sources: List<SourceDecl>?`, `sinks: List<SinkPoint>?`, `sanitizers: List<SanitizerDecl>?`. `isEmpty: Boolean`; `valueSemantics(): ValueSemantics?` returns null when `returns` is undeclared.
- **`ValueSemantics(returns: ReturnKind, propagation: List<Propagation>)`** — The whole value unit; absent propagation section decodes to the empty list (no flow, not unknown flow).
- **`Propagation(from: Port, to: Port)`** — One declared flow; file spellings `from`/`input` and `to`/`output` (one per side).
- **`Port`** — Sealed: `Port.Argument(position: Int)` (spelling `argument(n)`, `n >= 0`), `Port.Return` (spelling `return`). `Port.parse(raw: String): Port`.
- **`WhenGuard(port: Port.Argument, value: GuardValue)`** — Equality guard on one argument; bool, int, or string scalar.
- **`SourceDecl(provenance: Set<ProvenanceId>)`** — Non-empty produced color set.
- **`SinkPoint(port: Port.Argument, category: VulnClassId)`** — One dangerously consumed argument under one category.
- **`SanitizerDecl(categories: Set<VulnClassId>)`** — Non-empty neutralized category set.
- **`ReturnKind`** — Enum `STR`, `NUM`, `BOOL`, `ANY`; `join(other: ReturnKind): ReturnKind` gives itself when equal, `ANY` otherwise.

## Configuration

- Entry form is deduced from keys: `subject:` starts a flat model; `name:`/`find:`/`where:`/`model:` starts a generator. No `type:` discriminator.
- Generator `name` is load-unique per document.

## Gotchas

- An entry asserting nothing — no signature, empty body — is rejected at load.
- `propagation` without `returns` is rejected; the value unit is asserted whole. With a callable `signature`, `returns` is derived from the declared return type instead.
- Explicit `returns:` beside a callable `signature:` is rejected — one fact, one source.
- A `when:` guard on a non-callable subject is rejected.
- Section admissibility follows subject kind: `variable` entries declare sources only; `class` entries declare nothing besides their signature.
- Multiple entries per subject act as guard clauses in declaration order; an unguarded entry is the default branch. Branch selection is the consumer's job.
