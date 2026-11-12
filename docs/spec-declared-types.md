# PHP Models — Declared Type Parsing

## Problem

Input: the spelling of a [Declared Type](glossary/glossary-declarations.md#declared-type).
Output: its ordered union alternatives, each a set of normalized single types, or a format failure under [declaration invariants](model-declarations.md#invariants).
The raw spelling is retained independently of this derived representation.

## Steps

1. Reject an empty spelling or ASCII whitespace.
2. Split the spelling at union separators.
3. For a nullable prefix, require one single target other than null or a standalone-only keyword, then produce the target and null alternatives.
4. Without a union, read one single type or a bare intersection.
5. Within a union, require every intersection to be parenthesized and every single type to be ungrouped.
6. Split an intersection at its separators, validate each member as a class name, and reject keyword types.
7. For each single type, accept a keyword or a valid class-name spelling; fold ASCII case and remove a leading namespace separator from class names; reject a namespace-qualified keyword.
8. Reject a union containing mixed, void or never as a member.
9. To derive Return Classification, classify each alternative by the model table and join classifications: equal categories remain equal and differing categories give any.

## Invariants

- Each accepted alternative is nonempty.
- A keyword intersection is rejected.
- Nullable shorthand has exactly one non-null target and the null alternative.
- Validation never rewrites the retained raw spelling.

## Termination

Every split and validation scans finite substrings; no step recursively expands an alternative.

## Complexity

For n input characters, the fixed grammar requires O(n) scanning and O(n) output/storage, excluding library regex-engine overhead and hash collisions.
The class-name recognizer has no recursive domain expansion; its implementation is the regex in DeclaredType.kt.

## Examples

`?string` produces string and null alternatives and Return Classification any.
`(A&B)|null` produces the intersection of class names a and b plus null.
`int&string`, `?mixed` and `mixed|int` are invalid.
