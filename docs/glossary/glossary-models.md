# Glossary — Models

Models: the declarative statements about PHP declarations, the subjects they identify, and the parts that compose them.

---

## Models

### Matching Subject
- **Definition:** The PHP declaration or library operation a model identifies, of one of seven declaration kinds or the operation kind, spelled in PHP's own static-reference grammar.
- **Example:** `mysqli::query` → the method subject of class `mysqli` and method `query`, both case-folded.
- **Source:** `ModelSubject.kt` `ModelSubject`; `OperationSubject.kt` `OperationSubject`
- **Reference:** none

### Model
- **Definition:** The single declarative statement about one Matching Subject: what its calls return, how values flow through them, its taint effects, the exceptions it throws and what its declaration looks like, composed of at least one section.
- **Source:** `ModelEntry.kt` `ModelEntry`; `ModelBody.kt` `ModelBody`
- **Reference:** none

### Section
- **Definition:** One aspect of a Model: returns, propagation, characters, sources, sinks, sanitizers, throws or signature; an absent section asserts nothing about its aspect.
- **Source:** `ModelBody.kt` `ModelBody`; `docs/model.md` Entities
- **Reference:** none

### Port
- **Definition:** An explicitly named location in a call to a callable subject: an argument, the dynamic invocation selector, the receiver, the result, or a parameter or result of the subject's call to a callable it received as an argument.
- **Example:** `callback(argument(0)).parameter(1)` → the second argument the subject passes when it calls the callable given as its first argument.
- **Source:** `Port.kt` `Port`
- **Reference:** none

### Propagation
- **Definition:** One declared flow from an input port of a call, an argument, the selector, the receiver or a callback result, to another Port of the same call.
- **Example:** `{from: argument(0), to: return}` → the flow from the first argument to the call result.
- **Source:** `ModelBody.kt` `Propagation`
- **Reference:** none

### Signature
- **Definition:** The descriptive Section of a Model: what the declaration looks like, its parameters, declared types and class structure, never what an analysis believes about it.
- **Source:** `SignatureInfo.kt` `SignatureInfo`
- **Reference:** none

### Condition
- **Definition:** An optional argument pattern on a model entry, one expected Primitive Value or a wildcard per argument position, under which the entry holds when every listed position carries its value.
- **Source:** `ArgPattern.kt` `ArgPattern`; `docs/model-conditions.md`
- **Reference:** none

### Value unit
- **Definition:** The coupled statement of the returns, propagation and characters sections of a Model, which one declaration replaces whole: a non-empty set of returns classifications, the exhaustive set of propagations, and an optional character bound.
- **Example:** A model declaring returns `str` and no propagation → the value unit of classification str with no flow.
- **Source:** `ModelBody.kt` `ValueUnit`, `ModelBody.valueUnit`
- **Reference:** none

### `CharacterBound`
- **Definition:** The characters Section of a Model: the arguments whose bytes the call result copies, the class every result byte belongs to, or both, each narrowing the other.
- **Example:** `basename` with characters of `argument(0)` within `[^/]` → `basename("../../etc/passwd")` yields `passwd`.
- **Source:** `ModelBody.kt` `CharacterBound`, `CharacterClass`
- **Reference:** none
