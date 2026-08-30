# PHP Models — Subject and Port Design

The declaration-identity types: subjects, ports, and the propagation pair.
Vocabulary, policy, body, and loaders: [design.md](design.md). Entry forms
and generators: [design-generators.md](design-generators.md).

## Design Overview

- **Sealed hierarchies:** `ModelSubject` (two sealed bases, `NamedSubject`
  and `MemberSubject`, carrying the shared identity mechanics; seven
  concrete subtypes: `FunctionSubject`, `ClassSubject`, `MethodSubject`,
  `ClassConstantSubject`, `PropertySubject`, `ConstantSubject`,
  `VariableSubject`); `Port` (`Argument`, `Receiver`, `Return`, with the
  sealed sub-interface `Port.Input` marking the ports a call supplies
  values through: `Argument` and `Receiver`)
- **Classes:** `Propagation` (data class, private constructor, companion
  factory)
- **Relationships:** `Propagation` contains one `Port.Input` and one
  `Port`. All arrows one-way into the data types.
- **Exceptions:** `IllegalArgumentException` from `init` blocks and creators
  on every format violation ([design.md](design.md)).
- **Dependency roles:** Data holders: all types in this file.

Package `edu.jhu.cobra.commons.phpmodels`, same module and visibility rules
as [design.md](design.md).

## Class / Type Specifications

### ModelSubject (sealed)

**Responsibility:** The PHP declaration a model identifies. One subtype per
kind, holding exactly that kind's identity fields, case-folded per the table
in [model-declarations.md](model-declarations.md). Equality over the folded
identity, so a subject is usable as a lookup key. The interface declares
`name: String` — every kind has an own name; that shared field is what a
generator's name constraint matches
([design-generators.md](design-generators.md)).

**Structure:** two sealed bases carry the identity mechanics once —
`NamedSubject(kind, name)` for the name-only kinds,
`MemberSubject(kind, owner, name)` for the `owner::name` kinds. Each base
owns blank-field validation, per-concrete-kind equality (same concrete
class, same identity fields), hash code, and the kind-prefixed spelling,
with an optional `$` prefix. `MemberSubject` folds `owner`; each subtype
passes `name` folded or as-spelled per its sensitivity.

**Decoding:** the YAML form is a one-key mapping — the key names the kind,
the value is the PHP-native spelling string (`function: strlen`,
`method: mysqli::query`). Each subtype's companion carries a creator taking
the raw spelling; the private splitters strip the leading namespace slash,
split at the first `::`, and require the `$` prefix where the kind's
spelling mandates it. The splitters are the single authority for member
spellings; no other code inspects subject strings. Violations throw
`IllegalArgumentException` inside the creator. Jackson mechanism verified
in [impl.md](impl.md).

**Subtypes, YAML kind keys, and identity:**
- `FunctionSubject(name)` — `function:`, folded.
- `ClassSubject(name)` — `class:`, folded.
- `MethodSubject(owner, name)` — `method:`, both folded; spelled
  `class::name`.
- `ClassConstantSubject(owner, name)` — `class_constant:`, owner folded,
  name sensitive.
- `PropertySubject(owner, name)` — `property:`, owner folded, name
  sensitive; spelled `class::$name`, stored without the `$`.
- `ConstantSubject(name)` — `constant:`, sensitive.
- `VariableSubject(name)` — `variable:`, folded; spelled `$name`, stored
  without the `$`.

**Validation:** blank identity fields are rejected in `init`, and so is any
spelling-grammar character in an identity field (`::`, `$`, a leading `\`).
The base-class invariant covers both construction paths, so a directly
constructed subject and a parsed spelling always agree on one identity.

**Folding:** case folding happens inside the subject creators, once; every
consumer downstream compares folded identities for folded kinds and exact
identities for sensitive kinds. Identity fields fold with Kotlin
`String.lowercase()` — Unicode full case mapping — while PHP folds
identifiers ASCII-only. The divergence
is confined to non-ASCII identifiers, which the PHP builtin namespace does
not contain; an entry naming one folds more aggressively than PHP would,
never less.

### Port (sealed)

**Responsibility:** One explicitly named location in a call, decoded from
the string spellings `argument(n)`, `this`, and `return`. No bare integer
and no sentinel value exists anywhere in the port vocabulary.

**Subtypes:** `Argument(position: Int)` — `position >= 0`; `Receiver`
(object, spelling `this`); `Return` (object). The sealed sub-interface
`Port.Input` marks the ports a call supplies values through (`Argument`,
`Receiver`), so a from-side field is input-typed instead of
runtime-checked. `Argument` and `Input` each carry their own narrowing
creator (a field typed as a subtype does not consult the supertype's
creator, [impl.md](impl.md)).

**Methods:** companion `parse(raw: String): Port` — the decode entry point;
`IllegalArgumentException` on any other spelling.

### Propagation

**Responsibility:** One declared flow between two ports — `from:
Port.Input`, `to: Port`: a `data class` over the resolved pair with a
private constructor. The companion `invoke` factory — also the Jackson
creator — takes the synonym spellings `from`/`input` and `to`/`output` as
four nullable parameters and resolves each side; a Jackson alias would let
a pair naming both spellings decode silently ([impl.md](impl.md)).

**Validation:** exactly one spelling per side (factory); `to != from`
(`init`).

## Exception / Error Types

- `IllegalArgumentException` — blank identity field, malformed `::` or port
  spelling, a propagation whose sides are equal or double-spelled. Raised
  in `init` blocks and creators at decode through the shared load-boundary
  contract ([design.md](design.md)).

Domain semantics: [model-declarations.md](model-declarations.md).
