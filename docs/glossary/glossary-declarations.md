# Glossary — Declarations

What a declaration of PHP code looks like: its declared types, the classification derived from a declared return type, and the names of a class.

---

## Types

### Declared Type
- **Definition:** A PHP type declaration in the grammar of the PHP manual, stored losslessly and read as a union of alternatives, each an intersection of single types.
- **Example:** `?string` → the union `string|null` of two alternatives, each one single type.
- **Source:** `DeclaredType.kt` `DeclaredType`
- **Reference:** none (the PHP manual section "Type declarations" states the same concept)

### Return Classification
- **Definition:** The closed classification of a call result, one of str, num, bool and any, derived at load from the declared return type as the common classification of its alternatives, or any when they differ.
- **Example:** `int|float` → num; `int|false` → any.
- **Source:** `ReturnKind.kt` `ReturnKind`; `docs/model-declarations.md` Return Classification
- **Reference:** none

## Class Names

### Class name
- **Definition:** The name of a class in the letter case its declaration spells, namespace included and without a leading namespace separator.
- **Source:** `SignatureInfo.kt` `ClassSignature.name`; `docs/model-declarations-class-names.md`
- **Reference:** none

### Class alias
- **Definition:** A further name the engine registers for a class, stated by its class signature, stored case-folded and without a leading namespace separator, and distinct from the class name and from every other alias of that class.
- **Source:** `SignatureInfo.kt` `ClassSignature.aliases`; `docs/model-declarations-class-names.md`
- **Reference:** none
