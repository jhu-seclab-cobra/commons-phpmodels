# Glossary — Declarations

What a declaration of PHP code looks like: its declared types, the classification derived from a declared return type, and the names of a class.

---

## Declared Types

### Declared Type
- **Definition:** A PHP type declaration in the grammar of the PHP manual, stored losslessly and read as a union of alternatives, each an intersection of single types.
- **Example:** `?string` → the union `string|null` of two alternatives, each one single type.
- **Source:** `DeclaredType.kt` `DeclaredType`
- **Reference:** [@php-manual, "Type declarations"] — same

## Return Classification

### Return Classification
- **Definition:** The closed classification of a call result, one of str, num, bool and any, derived at load from the declared return type as the common classification of its alternatives, or any when they differ.
- **Example:** `int|float` → num; `int|false` → any.
- **Source:** `ReturnKind.kt` `ReturnKind`; `docs/model-declarations.md` Return Classification

| Member | Definition | Reference | Literature | Tools |
|--------|------------|-----------|------------|-------|
| `str` | The result is a string. | none | none | none |
| `num` | The result is an integer or floating-point number. | none | none | none |
| `bool` | The result is a boolean. | none | none | none |
| `any` | The result is not restricted to one scalar classification. | none | none | none |

## Class Names

### Class name
- **Definition:** The name of a class in the letter case its declaration spells, namespace included and without a leading namespace separator.
- **Example:** `class Example {}` → Class name `Example`.
- **Source:** `SignatureInfo.kt` `ClassSignature` property `name`; `docs/model-declarations-class-names.md`

### Class alias
- **Definition:** A further name the engine registers for a class, stated by its class signature, stored case-folded and without a leading namespace separator, and distinct from the class name and from every other alias of that class.
- **Example:** a declaration registering `ExampleAlias` for `Example` → Class alias `examplealias`.
- **Source:** `SignatureInfo.kt` `ClassSignature` property `aliases`; `docs/model-declarations-class-names.md`
