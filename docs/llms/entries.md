# Entries and Bodies

> Decoded model entries: one subject, an optional argument-pattern condition, an optional signature, and the sectioned assertion body.

## Quick Start

```kotlin
import edu.jhu.cobra.commons.phpmodels.ArgPattern
import edu.jhu.cobra.commons.value.BoolVal
val pattern = ArgPattern(listOf(BoolVal(true)))
val result = pattern.matchesAlternatives(listOf(setOf(BoolVal(true))))
result
```

## API

- **`ModelEntry(subject: ModelSubject, condition: ArgPattern?, signature: SignatureInfo?, body: ModelBody)`** — One entry: a named subject with its assertions. Several entries for one subject are alternatives; `(subject, condition)` is unique within one document.
- **`ArgPattern(expected: List<IPrimitiveVal?>)`** — The `when:` condition: one expected commons-value scalar per argument position, `null` for the wildcard `_`. `matches(actual: List<IPrimitiveVal?>): Boolean?` — `false` when the pattern is longer than the call or a known argument differs; `true` when every listed position holds an equal known argument; `null` when an unknown (`null` or `Unsure`) argument decides. `matchesAlternatives(actual: List<Set<IPrimitiveVal?>?>, defaults: List<IPrimitiveVal?> = emptyList()): Boolean?` — the same outcome over a call whose arguments each hold a set of alternatives, a `null` entry or a position past the end marking an omitted argument, which reads as the scalar `defaults` gives at that position: `false` when a listed position is omitted and has no default, or every alternative of a listed position differs; `true` when every alternative of every listed position equals its scalar; `null` otherwise. The positions stay independent, so no combination of alternatives across positions is formed. `positions: List<Int>` lists the non-wildcard indexes.
- **`ModelBody`** — Nine optional sections: `returns: ReturnKind?`, `propagation: List<Propagation>?`, `characters: CharacterBound?`, `sources: List<SourceDecl>?`, `sinks: List<SinkDecl>?`, `sanitizers: List<SanitizerDecl>?`, `callbacks: List<CallbackInvocation>?`, `throws: Set<ClassSubject>?` (the exception classes the implementation may throw; the empty set states it throws none), `effects: List<Nothing>?` (null: no assertion; the empty list: the effects assertion `effects: []`). `isEmpty: Boolean`; `declaresOnlySources: Boolean`; `namesReceiverPort: Boolean`; `declaresExplicitSourceSite: Boolean`; `valueUnit(): ValueUnit?` returns null when `returns` is undeclared.
- **`ValueUnit(returns: Set<ReturnKind>, propagation: List<Propagation>, characters: CharacterBound?, callbacks: List<CallbackInvocation>, effects: List<Nothing>?)`** — The whole Value unit; `returns` is non-empty, and the constructor taking one `ReturnKind` builds the unit of a single declared kind. `join(other: ValueUnit): ValueUnit` unions the kinds, flows and callback declarations, keeps `characters` only when both are equal and keeps `effects` only when both state it. Absent propagation section decodes to the empty list (no flow, not unknown flow); absent characters section stays null (no bound); absent effects section stays null (no assertion).
- **`CharacterBound(of: Set<Port.Argument>?, within: CharacterClass?)`** — The bytes a call result may hold: only bytes of the `of` ports' values, only bytes of the `within` class; at least one field declared, a declared `of` non-empty.
- **`CharacterClass(pattern: String)`** — Regex matched against one byte read as one Latin-1 character; `contains(byte: Int): Boolean` over 0..255. Decoded from a bare string scalar; equality over the pattern text.
- **`Propagation(from: Port.Input, to: Port, unchanged: Boolean = false)`** — One declared Propagation; file spellings `from`/`input` and `to`/`output` (one per side). The from side is input-typed: `return` as a from side fails to decode. `unchanged: true` is valid only with `to: return` and states that the call returns the value at `from` itself.
- **`Port`** — Sealed call locations: Argument, Callee, Receiver, Return, CallbackParameter and CallbackReturn; spellings are `argument(n)`, `callee`, `this`, `return`, `callback(argument(n)).parameter(m)` and `callback(argument(n)).return`.
- **`Port.parse(raw: String): Port`** — Reads a port spelling; raises `IllegalArgumentException` on an invalid spelling.
- **`Port.Input`** — Argument, Callee, Receiver or CallbackReturn.
- **`Port.Sink`** — Argument or Callee; Models admit callee only as a sink of the dynamic invocation operation.
- **`Port.Argument.parseArgument(raw: String): Port.Argument`** — Narrows a spelling to an argument port.
- **`Port.Input.parseInput(raw: String): Port.Input`** — Narrows a spelling to an input port.

- **`SourceDecl(sourceKinds: Set<SourceKindId>, at: Port.Argument?, keys: List<KeyPattern>?)`** — Non-empty produced Source kind set; optional explicit production site (`at:`, a by-ref out-parameter) and non-empty key-pattern list (`keys:`).
- **`KeyPattern(pattern: String)`** — Regex over array keys, matched entire-key and case-sensitively; `matches(key: String): Boolean`. Decoded from a bare string scalar.
- **`SinkDecl(port: Port.Sink, vulnType: VulnTypeId)`** — One consumed argument or dynamic invocation selector under one Vulnerability type.
- **`SanitizerDecl(vulnTypes: Set<VulnTypeId> = emptySet(), check: ArgumentCheck? = null)`** — Neutralized Vulnerability type set of the result (non-empty without a check), or of the argument a check accepts.
- **`ArgumentCheck(argument: Port.Argument, accepts: IPrimitiveVal, within: CharacterClass? = null, types: DeclaredType? = null)`** — YAML `check: {argument: argument(0), accepts: true, within: "[0-9]"}`; on a call returning `accepts`, the argument is free of the element's Vulnerability type entries and its bytes lie in `within`; `types` holds exactly when the call returns `accepts`.
- **`ReturnKind`** — Enum `STR`, `NUM`, `BOOL`, `ANY`; `join(other: ReturnKind): ReturnKind` gives itself when equal, `ANY` otherwise.

## Configuration

- `when:` is a YAML sequence of scalars, one per argument position: `when: [_, true]`. `_` is the wildcard; a boolean becomes `BoolVal`, an integer within `Long` becomes `IntVal`, any other number becomes `FloatVal`, `null`/`~` becomes `NullVal`, any other text becomes `StrVal`. Quote a scalar to force the string shape.
- A pattern must be non-empty and name at least one non-wildcard position; a mapping or sequence element fails the decode.
- `throws:` is a YAML sequence of bare class names, without the `class:` key: `throws: [ValueError, \JsonException]`. Each folds as a class subject (leading `\` stripped, lowercased); a member spelling fails. `throws: []` and `effects: []` are complete statements, unlike any other empty section.
- `effects: []` is the only admitted effects value: it asserts that after parameter reception the native operation terminates normally apart from separately declared raises, depends only on the declared inputs and has no observable interaction besides its declared port flows, immediate invocations and separately modeled raises. `effects: null`, a nonempty sequence, a scalar or a mapping fails the decode.
- `characters:` is a mapping with `of:` (a sequence of `argument(n)` ports) and `within:` (a quoted regex): `characters: {of: [argument(0)], within: "[^/]"}`. The bytes of an argument are those of the strings the call converts it to: a scalar's string form, an object's `__toString` result, each element's conversion for an array read element by element, and `Array` for a nested array. `of` names `argument(n)` ports only. `within` uses `java.util.regex` syntax and must match at least one byte: write `\p{Alpha}`, not the POSIX `[[:alpha:]]`.

## Gotchas

- An entry asserting nothing — no signature, empty body — is rejected at load.
- `propagation` without `returns` is rejected; the Value unit is asserted whole. With a callable `signature`, `returns` is derived from the declared return type instead.
- `characters` without `returns` is rejected, like `propagation`; beside a callable `signature:` it needs a `propagation` section for `returns` to be derived. Each `of:` port must be the `from:` of a declared flow into `return`.
- Explicit `returns:` beside a callable `signature:` is rejected — one fact, one source.
- `effects:` requires a `function` or `method` subject, explicit `returns:` and no `signature:`; any other entry carrying it is rejected.
- A callable `signature:` fixes the arity: an argument port (condition position, propagation side, sink, source `at:` site) outside the declared parameter list is rejected, unless the last parameter is variadic. An entry without a signature is not arity-checked.
- With a callable `signature:`, a written-into argument port — a propagation `to:` side or a source `at:` site — must name a `byRef: true` parameter; a variadic-tail position resolves to the variadic parameter's flag.
- With a callable `signature:` whose `returnType` is `void`, a propagation into `return` is rejected.
- A `when:` condition on a non-callable subject is rejected.
- The receiver port `this` anywhere in the body requires a `method` subject.
- A source `at:` site requires a callable subject.
- Section admissibility follows subject kind: `returns`/`propagation`/`characters`/`callbacks`/`effects`, `throws`, and `when:` belong to `function` and `method` entries; `class` entries declare nothing besides their signature; other non-callable kinds declare sources only.
- Two entries of one document sharing subject and condition are rejected; an unconditional entry beside conditional ones is the subject's default. Which alternative applies to a call is the consumer's decision over `matches`.
- Equality in `matches` is commons-value equality: `IntVal(1)` and `FloatVal(1.0)` are unequal, as are `BoolVal(true)` and `StrVal("true")`.

- **`CallbackInvocation(callable: CallbackSelector, supply: CallbackSupply)`** — Declares an invoked operand and its input supply. Selectors use `argument`, `trailing` or `elements`; supplies use `array-elements`, `joined-inputs`, `array-value` or `unpacked`. Positions and starts are non-negative; trailing distances and joined parameter counts are positive.
