# PHP Models — Condition Design

The type carrying the optional `when:` condition and its one match
operation. Semantics: [model-conditions.md](model-conditions.md). The
entry that carries it: [design-entries.md](design-entries.md).

## Design Overview

- **Classes:** `ArgPattern` (data class)
- **External types (commons-value):** `IPrimitiveVal` and its scalar
  subtypes `StrVal`, `IntVal`, `FloatVal`, `BoolVal`, `NullVal`; `Unsure`
- **Relationships:** `ModelEntry` contains at most one `ArgPattern`;
  `ArgPattern` contains a list of nullable `IPrimitiveVal`. All arrows
  one-way into the pattern type.
- **Exceptions:** `IllegalArgumentException` from decode and `init` — an
  empty pattern, a pattern of wildcards only, a non-scalar element.
- **Dependency roles:** Data holder: `ArgPattern`. Operation: `matches`.
  Consumers: the fact filter of the consuming analyzer.

Package `edu.jhu.cobra.commons.phpmodels`. Public — a condition is part of
the decoded entry a consumer reads beside every fact. commons-value is an
`api` dependency: the pattern's element type and the match operation's
parameter type are its primitives, so the format library owns no scalar
type of its own.

What a consumer does with each match outcome — over-approximate, drop, or
fall back to the unconditional entry — is consumer structure. This library
fixes what a condition *is* and what one match *answers*.

## Class / Type Specifications

### ArgPattern

**Responsibility:** One decoded `when:` condition: the expected value per
argument position. Value identity: two entries with equal patterns state
alternatives of the same key, so equality and hash cover the list.

**State/Fields:** `val expected: List<IPrimitiveVal?>` — position `i` holds
the scalar argument `i` must equal, or `null` for the wildcard `_`.

**Decoding:** the YAML form is a sequence of scalars, `when: [_, 257]`.
The creator receives the raw sequence and narrows each element by shape
([impl.md](impl.md)): the bare text `_` becomes the wildcard; a boolean
becomes `BoolVal`; an integral within `Long` becomes `IntVal`; any other
number becomes `FloatVal`; a YAML null becomes `NullVal`; any other text
becomes `StrVal`. A mapping or sequence element fails the decode. The
text `_` is always the wildcard: a PHP argument whose value is the string
`_` cannot be a condition.

**Validation (`init`):** `expected` is non-empty and holds at least one
non-wildcard element; no element is an `Unsure`.

**Methods:**
- `fun matches(actual: List<IPrimitiveVal?>): Boolean?` — the match
  outcome of [model-conditions.md](model-conditions.md) over the call's
  arguments: `false` when `expected` is longer than `actual` or some
  non-wildcard position holds a known argument unequal to its scalar;
  `true` when every non-wildcard position holds an equal known argument;
  `null` (undecidable) otherwise. An argument is *known* when it is a
  non-null value other than `Unsure`; equality is commons-value equality,
  so `IntVal(1)` and `FloatVal(1.0)` are unequal. A failing position
  decides the outcome even when another position is unknown.
- `val positions: List<Int>` — the non-wildcard positions, for the
  entry-level arity check ([design-entries.md](design-entries.md)).

## Exception / Error Types

- `IllegalArgumentException` — the decode and validation failures listed
  above, raised while Jackson instantiates, surfacing at the caller's load
  boundary with every other malformed-entry failure.

Domain semantics: [model-conditions.md](model-conditions.md).
