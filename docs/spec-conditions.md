# PHP Models — Condition Matching

## Problem

Input: a [Condition](glossary/glossary-models.md#condition), supplied scalar arguments or per-position alternatives, and optional declared scalar defaults.
Output: holds, fails or undecidable under [the condition model](model-conditions.md).
Equality is equality of the corresponding commons-value Primitive Value, without numeric coercion.

## Steps

1. For scalar-list matching, fail when the supplied list is shorter than the entire Condition.
2. Enumerate constrained positions; ignore wildcard positions.
3. For a scalar-list position, an unknown argument is undecidable; a known scalar holds exactly when equal to the expected scalar.
4. For an alternatives position that is omitted, compare its scalar default with the expectation; fail when no scalar default exists.
5. For supplied alternatives, return undecidable for an empty set or an unknown member; otherwise the position holds when all members hold, fails when all fail, and is undecidable when both outcomes occur.
6. Reduce positions: any failure gives fails, otherwise any undecidable position gives undecidable, otherwise return holds.

## Invariants

- Each constrained position contributes one outcome.
- Positions are independent; the algorithm forms no Cartesian product.
- An unknown supplied argument is not an omitted argument.
- The declared null scalar is known and differs from the absence of a default.

## Termination

Every scan visits a finite pattern or finite supplied set; the number of unvisited constrained positions and alternatives decreases at each iteration.

## Complexity

Let p be the pattern length, c its constrained-position count, and a the sum of alternative counts at those positions.
Ignoring scalar-content comparison costs, scalar matching takes O(p) time and O(c) temporary space; alternatives matching takes O(p + a) time and O(c) temporary space.
String equality additionally reads the compared scalar contents.

## Examples

The complete PHP default-binding example and its output are in [the model](model-conditions.md).
For expected `[true, false]`, an unknown first argument and a known true second argument produce fails; the second position decides even when the first is undecidable.
Expected `[true, _]` over one supplied true scalar fails the scalar-list length precondition, but alternatives matching holds because it never reads the trailing wildcard.
