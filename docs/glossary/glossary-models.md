# Glossary — Models

## Model Structure

### Model
- **Definition:** A declarative statement about a PHP declaration or library operation, composed of at least one aspect of behavior or declaration structure.
- **Example:** `subject: {function: f}, returns: str` → a Model asserting a string result.
- **Source:** `ModelEntry.kt` `ModelEntry`; `ModelBody.kt` `ModelBody`

### Matching Subject
- **Definition:** The PHP declaration or library operation a Model identifies, of one of seven declaration kinds or the operation kind, spelled in PHP's own static-reference grammar.
- **Example:** `mysqli::query` → the method subject of class `mysqli` and method `query`, both case-folded.
- **Source:** `ModelSubject.kt` `ModelSubject`; `OperationSubject.kt` `OperationSubject`

### Section
- **Definition:** One aspect of a Model: returns, propagation, characters, callbacks, effects, sources, sinks, sanitizers, throws or signature, with omission leaving that aspect unstated unless declared returns fixes omitted propagation to the empty flow set.
- **Example:** `returns: str` → the returns Section of a Model.
- **Source:** `ModelBody.kt` `ModelBody`; `docs/model.md` Entities

### Port
- **Definition:** An explicitly named location in a call to a callable subject: an argument, the dynamic invocation selector, the receiver, the result, or a parameter or result of the subject's call to a callable it received as an argument.
- **Example:** `callback(argument(0)).parameter(1)` → the second argument the subject passes when it calls the callable given as its first argument.
- **Source:** `Port.kt` `Port`

### Propagation
- **Definition:** One declared flow from an argument, receiver or callback result of a call to a different Port of that call, excluding callee. A flow into the result marked `unchanged` states that the call returns the value at its from-port itself, not a new value.
- **Example:** `{from: argument(0), to: return}` → the flow from the first argument to the call result; `{from: this, to: return, unchanged: true}` → the call returns its receiver.
- **Source:** `ModelBody.kt` `Propagation`

### Signature
- **Definition:** The descriptive Section of a Model: what the declaration looks like, its parameters, declared types and class structure, never what an analysis believes about it.
- **Example:** `signature: {params: [], returnType: int}` → the descriptive Signature of a function Model.
- **Source:** `SignatureInfo.kt` `SignatureInfo`

### Condition
- **Definition:** An optional scalar-equality pattern on a Model, with one expected Primitive Value or wildcard per argument position.
- **Example:** `when: [_, false]` → a Condition constraining only the second argument.
- **Source:** `ArgPattern.kt` `ArgPattern`; `docs/model-conditions.md`

### Value unit
- **Definition:** The coupled statement of the returns, propagation, characters, callbacks and effects sections of a Model, which one declaration replaces whole: a non-empty set of Return Classification values, the exhaustive set of propagations, an optional character bound, the immediate callback declarations with their input supplies, and the optional effects assertion `effects: []`, which states that after parameter reception the native operation terminates normally apart from separately declared raises, depends only on the declared inputs, and has no observable interaction besides its declared port flows, immediate invocations and separately modeled raises.
- **Example:** A model declaring returns `str` and no propagation → the Value unit of classification str with no flow.
- **Source:** `ModelBody.kt` `ValueUnit`, `ModelBody` method `valueUnit`

### `CharacterBound`
- **Definition:** The characters Section of a Model: the arguments whose bytes the call result copies, the class every result byte belongs to, or both, each narrowing the other.
- **Example:** `basename` with characters of `argument(0)` within `[^/]` → `basename("../../etc/passwd")` yields `passwd`.
- **Source:** `ModelBody.kt` `CharacterBound`, `CharacterClass`


### Key Pattern
- **Definition:** A regular expression matching the entire string form of an array key at a source production site.
- **Example:** `id` → the pattern matches key `id` and rejects `userid`.
- **Source:** `ModelBody.kt` `KeyPattern`

### Character Class
- **Definition:** A set of bytes selected by a regular expression matched against each byte as one Latin-1 character.
- **Example:** `[0-9]` → byte values 48 through 57.
- **Source:** `ModelBody.kt` `CharacterClass`

### Override Unit
- **Definition:** A group of Sections replaced together when a consumer combines Models for the same Matching Subject and Condition.
- **Example:** `returns: str` → the Value unit is one Override Unit even with no Propagation.
- **Source:** `docs/model.md` Entities
