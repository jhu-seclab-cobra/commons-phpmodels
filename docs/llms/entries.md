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
- **`ArgPattern(expected: List<IPrimitiveVal?>)`** — The `when:` condition: one expected commons-value scalar per argument position, `null` for the wildcard `_`. `matches(actual: List<IPrimitiveVal?>): Boolean?` — `false` when the pattern is longer than the call or a known argument differs; `true` when every listed position holds an equal known argument; `null` when an unknown (`null` or `Unsure`) argument decides. `matchesAlternatives(actual: List<Set<IPrimitiveVal?>?>): Boolean?` — the same outcome over a call whose arguments each hold a set of alternatives, a `null` entry marking an omitted argument: `false` when the pattern is longer than the call, a listed position is omitted, or every alternative of a listed position differs; `true` when every alternative of every listed position equals its scalar; `null` otherwise. The positions stay independent, so no combination of alternatives across positions is formed. `positions: List<Int>` lists the non-wildcard indexes.
- **`ModelBody`** — Seven optional sections: `returns: ReturnKind?`, `propagation: List<Propagation>?`, `characters: CharacterBound?`, `sources: List<SourceDecl>?`, `sinks: List<SinkDecl>?`, `sanitizers: List<SanitizerDecl>?`, `throws: Set<ClassSubject>?` (the exception classes the implementation may throw; the empty set states it throws none). `isEmpty: Boolean`; `declaresOnlySources: Boolean`; `namesReceiverPort: Boolean`; `declaresExplicitSourceSite: Boolean`; `valueUnit(): ValueUnit?` returns null when `returns` is undeclared.
- **`ValueUnit(returns: Set<ReturnKind>, propagation: List<Propagation>, characters: CharacterBound?)`** — The whole value unit; `returns` is non-empty, and the constructor taking one `ReturnKind` builds the unit of a single declared kind. `join(other)` unions the kinds and the flows and keeps `characters` only when both are equal. Absent propagation section decodes to the empty list (no flow, not unknown flow); absent characters section stays null (no bound).
- **`CharacterBound(of: Set<Port.Argument>?, within: CharacterClass?)`** — The bytes a call result may hold: only bytes of the `of` ports' values, only bytes of the `within` class; at least one field declared, a declared `of` non-empty.
- **`CharacterClass(pattern: String)`** — Regex matched against one byte read as one Latin-1 character; `contains(byte: Int): Boolean` over 0..255. Decoded from a bare string scalar; equality over the pattern text.
- **`Propagation(from: Port.Input, to: Port)`** — One declared flow; file spellings `from`/`input` and `to`/`output` (one per side). The from side is input-typed: `return` as a from side fails to decode.
- **`Port`** — Sealed: `Port.Argument(position: Int)` (spelling `argument(n)`, `n >= 0`), `Port.Receiver` (spelling `this`), `Port.Return` (spelling `return`). `Port.Input` is the sealed sub-interface of `Argument` and `Receiver`. `Port.parse(raw: String): Port`; `Port.Argument.parseArgument(raw: String): Port.Argument` and `Port.Input.parseInput(raw: String): Port.Input` narrow to the subtype.
- **`SourceDecl(sourceKinds: Set<SourceKindId>, at: Port.Argument?, keys: List<KeyPattern>?)`** — Non-empty produced source kind set; optional explicit production site (`at:`, a by-ref out-parameter) and non-empty key-pattern list (`keys:`).
- **`KeyPattern(pattern: String)`** — Regex over array keys, matched entire-key and case-sensitively; `matches(key: String): Boolean`. Decoded from a bare string scalar.
- **`SinkDecl(port: Port.Argument, vulnType: VulnTypeId)`** — One dangerously consumed argument under one vulnerability type.
- **`SanitizerDecl(vulnTypes: Set<VulnTypeId> = emptySet(), check: ArgumentCheck? = null)`** — Neutralized vulnerability type set of the result (non-empty without a check), or of the argument a check accepts.
- **`ArgumentCheck(argument: Port.Argument, accepts: IPrimitiveVal, within: CharacterClass? = null, types: DeclaredType? = null)`** — YAML `check: {argument: argument(0), accepts: true, within: "[0-9]"}`; on a call returning `accepts`, the argument is free of the element's vulnerability types and its bytes lie in `within`; `types` holds exactly when the call returns `accepts`.
- **`ReturnKind`** — Enum `STR`, `NUM`, `BOOL`, `ANY`; `join(other: ReturnKind): ReturnKind` gives itself when equal, `ANY` otherwise.

## Configuration

- `when:` is a YAML sequence of scalars, one per argument position: `when: [_, true]`. `_` is the wildcard; a boolean becomes `BoolVal`, an integer within `Long` becomes `IntVal`, any other number becomes `FloatVal`, `null`/`~` becomes `NullVal`, any other text becomes `StrVal`. Quote a scalar to force the string shape.
- A pattern must be non-empty and name at least one non-wildcard position; a mapping or sequence element fails the decode.
- `throws:` is a YAML sequence of bare class names, without the `class:` key: `throws: [ValueError, \JsonException]`. Each folds as a class subject (leading `\` stripped, lowercased); a member spelling fails. `throws: []` is a complete statement, unlike any other empty section.
- `characters:` is a mapping with `of:` (a sequence of `argument(n)` ports) and `within:` (a quoted regex): `characters: {of: [argument(0)], within: "[^/]"}`. The bytes of an argument are those of the strings the call converts it to: a scalar's string form, an object's `__toString` result, each element's conversion for an array read element by element, and `Array` for a nested array. `of` names `argument(n)` ports only. `within` uses `java.util.regex` syntax and must match at least one byte: write `\p{Alpha}`, not the POSIX `[[:alpha:]]`.

## Gotchas

- An entry asserting nothing — no signature, empty body — is rejected at load.
- `propagation` without `returns` is rejected; the value unit is asserted whole. With a callable `signature`, `returns` is derived from the declared return type instead.
- `characters` without `returns` is rejected, like `propagation`; beside a callable `signature:` it needs a `propagation` section for `returns` to be derived. Each `of:` port must be the `from:` of a declared flow into `return`.
- Explicit `returns:` beside a callable `signature:` is rejected — one fact, one source.
- A callable `signature:` fixes the arity: an argument port (condition position, propagation side, sink, source `at:` site) outside the declared parameter list is rejected, unless the last parameter is variadic. An entry without a signature is not arity-checked.
- With a callable `signature:`, a written-into argument port — a propagation `to:` side or a source `at:` site — must name a `byRef: true` parameter; a variadic-tail position resolves to the variadic parameter's flag.
- With a callable `signature:` whose `returnType` is `void`, a propagation into `return` is rejected.
- A `when:` condition on a non-callable subject is rejected.
- The receiver port `this` anywhere in the body requires a `method` subject.
- A source `at:` site requires a callable subject.
- Section admissibility follows subject kind: `returns`/`propagation`/`characters`, `throws`, and `when:` belong to `function` and `method` entries; `class` entries declare nothing besides their signature; other non-callable kinds declare sources only.
- Two entries of one document sharing subject and condition are rejected; an unconditional entry beside conditional ones is the subject's default. Which alternative applies to a call is the consumer's decision over `matches`.
- Equality in `matches` is commons-value equality: `IntVal(1)` and `FloatVal(1.0)` are unequal, as are `BoolVal(true)` and `StrVal("true")`.
