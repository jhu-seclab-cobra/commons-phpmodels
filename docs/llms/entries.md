# Entries and Bodies

> Decoded model entries: one subject, an optional argument-pattern condition, an optional signature, and the sectioned assertion body.

## Quick Start

```kotlin
val set = DocumentSetLoader.load(opener)
for (entry in set.entries) {
    entry.subject to entry.body
    entry.condition?.matches(callArguments) // true, false, or null (undecidable)
}
```

## API

- **`ModelEntry(subject: ModelSubject, condition: ArgPattern?, signature: SignatureInfo?, body: ModelBody)`** — One entry: a named subject with its assertions. Several entries for one subject are alternatives; `(subject, condition)` is unique within one document.
- **`ArgPattern(expected: List<IPrimitiveVal?>)`** — The `when:` condition: one expected commons-value scalar per argument position, `null` for the wildcard `_`. `matches(actual: List<IPrimitiveVal?>): Boolean?` — `false` when the pattern is longer than the call or a known argument differs; `true` when every listed position holds an equal known argument; `null` when an unknown (`null` or `Unsure`) argument decides. `positions: List<Int>` lists the non-wildcard indexes.
- **`ModelBody`** — Five optional sections: `returns: ReturnKind?`, `propagation: List<Propagation>?`, `sources: List<SourceDecl>?`, `sinks: List<SinkDecl>?`, `sanitizers: List<SanitizerDecl>?`. `isEmpty: Boolean`; `valueSemantics(): ValueSemantics?` returns null when `returns` is undeclared.
- **`ValueSemantics(returns: ReturnKind, propagation: List<Propagation>)`** — The whole value unit; absent propagation section decodes to the empty list (no flow, not unknown flow).
- **`Propagation(from: Port.Input, to: Port)`** — One declared flow; file spellings `from`/`input` and `to`/`output` (one per side). The from side is input-typed: `return` as a from side fails to decode.
- **`Port`** — Sealed: `Port.Argument(position: Int)` (spelling `argument(n)`, `n >= 0`), `Port.Receiver` (spelling `this`), `Port.Return` (spelling `return`). `Port.Input` is the sealed sub-interface of `Argument` and `Receiver`. `Port.parse(raw: String): Port`.
- **`SourceDecl(origin: Set<OriginId>, at: Port.Argument?, keys: List<KeyPattern>?)`** — Non-empty produced color set (YAML key `provenance`); optional explicit production site (`at:`, a by-ref out-parameter) and non-empty key-pattern list (`keys:`).
- **`KeyPattern(pattern: String)`** — Regex over array keys, matched entire-key and case-sensitively; `matches(key: String): Boolean`. Decoded from a bare string scalar.
- **`SinkDecl(port: Port.Argument, vulnClass: VulnClassId)`** — One dangerously consumed argument under one category (YAML key `category`).
- **`SanitizerDecl(categories: Set<VulnClassId>)`** — Non-empty neutralized category set.
- **`ReturnKind`** — Enum `STR`, `NUM`, `BOOL`, `ANY`; `join(other: ReturnKind): ReturnKind` gives itself when equal, `ANY` otherwise.

## Configuration

- `when:` is a YAML sequence of scalars, one per argument position: `when: [_, true]`. `_` is the wildcard; a boolean becomes `BoolVal`, an integer within `Long` becomes `IntVal`, any other number becomes `FloatVal`, `null`/`~` becomes `NullVal`, any other text becomes `StrVal`. Quote a scalar to force the string shape.
- A pattern must be non-empty and name at least one non-wildcard position; a mapping or sequence element fails the decode.

## Gotchas

- An entry asserting nothing — no signature, empty body — is rejected at load.
- `propagation` without `returns` is rejected; the value unit is asserted whole. With a callable `signature`, `returns` is derived from the declared return type instead.
- Explicit `returns:` beside a callable `signature:` is rejected — one fact, one source.
- A callable `signature:` fixes the arity: an argument port (condition position, propagation side, sink, source `at:` site) outside the declared parameter list is rejected, unless the last parameter is variadic. An entry without a signature is not arity-checked.
- With a callable `signature:`, a written-into argument port — a propagation `to:` side or a source `at:` site — must name a `byRef: true` parameter; a variadic-tail position resolves to the variadic parameter's flag.
- With a callable `signature:` whose `returnType` is `void`, a propagation into `return` is rejected.
- A `when:` condition on a non-callable subject is rejected.
- The receiver port `this` anywhere in the body requires a `method` subject.
- A source `at:` site requires a callable subject.
- Section admissibility follows subject kind: `returns`/`propagation` and `when:` belong to `function` and `method` entries; `class` entries declare nothing besides their signature; other non-callable kinds declare sources only.
- Two entries of one document sharing subject and condition are rejected; an unconditional entry beside conditional ones is the subject's default. Which alternative applies to a call is the consumer's decision over `matches`.
- Equality in `matches` is commons-value equality: `IntVal(1)` and `FloatVal(1.0)` are unequal, as are `BoolVal(true)` and `StrVal("true")`.
