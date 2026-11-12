# PHP Models — Conditional Models

## Entities

- **[Condition](glossary/glossary-models.md#condition)** constrains scalar argument positions of one Model; `_` leaves a position unconstrained.
- A Model with a Condition is a complete alternative for that pattern; its Sections are not deltas over another entry.
- A Model without a Condition supplies the consumer's unconditional alternative.
- The consumer supplies its knowledge of call arguments; the library compares that knowledge with the Condition.

## Relations

| From | To | Cardinality | Meaning |
|------|----|-------------|---------|
| Matching Subject | Model with a Condition | 1:N | Alternatives describing one subject |
| Condition | Constrained argument position | 1:N | Equality to one expected Primitive Value |
| Model | Condition | 1:0..1 | The entry's optional argument constraint |

## State Model

A constrained position has one of three outcomes.

| Outcome | Condition |
|---------|-----------|
| holds | Every supplied alternative is the expected scalar, or the position is omitted and its declared scalar default equals that scalar |
| fails | Every supplied alternative is a known unequal scalar, or an omitted position has no scalar default or a different default |
| undecidable | Supplied alternatives are mixed, empty, or contain an unknown value |

Any failing constrained position makes the whole Condition fail; otherwise an undecidable position makes it undecidable; otherwise it holds.
Wildcard positions do not participate in this reduction.
A scalar-list match has the additional precondition that the supplied list covers the entire pattern; it fails when the list is shorter, including an omitted trailing wildcard.
An alternatives match reads omitted constrained positions from the supplied scalar defaults and ignores omitted wildcard positions.
[Condition matching](spec-conditions.md) owns the procedure.

## Invariants

- Only function and method Models admit a Condition.
- A Condition has at least one position and at least one constrained position.
- An expected scalar is a Primitive Value other than Uncertain Value; nested lists and maps are invalid.
- Scalar equality preserves the concrete Value form: integer one and floating-point one are unequal.
- Each Model satisfies its section invariants independently.
- Two entries in one document with equal Matching Subject and Condition, including two unconditional entries, are invalid.
- The consumer determines the number and alternatives of supplied arguments; argument unpacking may leave that knowledge incomplete.
- A missing scalar default differs from the declared null scalar.

## Cross-Structure Contracts

- The library defines matching and [Value unit](glossary/glossary-models.md#value-unit) join; the consumer selects the participating entries and decides how to use an undecidable outcome.
- Joining two Value unit instances unions their Return Classification values and Propagation values, retaining the characters bound only when both operands state equal bounds.
- The join operation selects no Model and assigns no priority to a Condition.
- Combining Document Set entries uses the consumer's rule for Matching Subject, Condition and Override Unit, ranked by Set Provenance where that consumer requires it.

A declared scalar default participates in matching after PHP parameter reception:

```php
<?php
function flag($value, $binary = false) { return $binary; }
var_dump(flag('x'));
var_dump(strlen(md5('x')), strlen(md5('x', true)));
```

```text
bool(false)
int(32)
int(16)
```

A Condition requiring the second argument to be false holds for the omitted default of either call with that declaration; an unknown supplied second argument remains undecidable.
Software mapping: [ArgPattern](design-conditions.md).
