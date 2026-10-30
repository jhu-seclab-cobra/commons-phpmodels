# Subjects and Signatures

> The PHP declaration a model identifies, and the decoded signature section per declaration kind.

## Quick Start

```kotlin
val subject = MethodSubject.parse("mysqli::query")
val model = DocumentSetLoader.load(opener).entries.single()
model.subject == subject
```

## API

- **`ModelSubject`** — Sealed; equality over the folded identity, usable as a lookup key. One subtype per kind; each companion carries `parse(raw: String)` taking the PHP-native spelling.

| Subtype | YAML key | Spelling | Case |
|---------|----------|----------|------|
| `FunctionSubject(name)` | `function:` | `strlen` | folded |
| `ClassSubject(name)` | `class:` | `mysqli` | folded |
| `MethodSubject(owner, name)` | `method:` | `mysqli::query` | both folded |
| `ClassConstantSubject(owner, name)` | `class_constant:` | `Exception::SEVERITY_ERROR` | owner folded, name sensitive |
| `PropertySubject(owner, name)` | `property:` | `mysqli::$insert_id` | owner folded, name sensitive |
| `ConstantSubject(name)` | `constant:` | `PHP_EOL` | sensitive |
| `VariableSubject(name)` | `variable:` | `$_GET` | folded |

- **`SignatureInfo`** — Sealed; the subtype is selected from the entry's subject kind, never from a discriminator key.
- **`SignatureInfo.CallableSignature(params: List<ParameterInfo>, returnType: DeclaredType, framelessArities: Set<Int> = emptySet(), specialArities: Set<Int> = emptySet(), ropeFormat: String? = null)`** — function, method; `framelessArities` are the argument counts (at most 3) at which php-src selects a frameless function, `specialArities` the counts (2 or more) at which it always compiles a call bound when compiled to one opcode, `ropeFormat` the parameter php-src compiles into a rope; counts lie within the required..declared parameter count (any above it with a variadic tail), and a method declares none of the three.
- **`SignatureInfo.ClassSignature(classifier: Classifier, parent: String?, interfaces: List<String>)`** — class; parent and interfaces case-folded.
- **`SignatureInfo.TypedSignature(type: DeclaredType, value: String?)`** — constant, class constant; `value` is the spelled literal, null when the source states none.
- **`SignatureInfo.PropertySignature(type: DeclaredType, visibility: Visibility, static: Boolean)`** — property.
- **`ParameterInfo(name: String, type: DeclaredType, optional: Boolean, byRef: Boolean, variadic: Boolean, namedExtras: NamedExtras? = null, default: String? = null)`** — one declared parameter; position is list order. `default` spells the scalar literal the stub declares (`true`, `false`, `null`, a decimal number, a quoted string without an escape) and needs `optional` and not `variadic`; `defaultValue: IPrimitiveVal?` is the scalar it spells, null when absent.
- **`DeclaredType(raw: String)`** — Value class over a PHP type declaration as spelled, without whitespace: a single type, `?T`, a union, an intersection of class names, or a union of single types and parenthesized intersections; `alternatives: List<Set<String>>` is its union read (one set per alternative, names lowercased, a class name without its leading slash; `?T` reads as `[{t}, {null}]`); `toReturnKind(): ReturnKind` classifies each alternative (string → STR; int, float → NUM; bool, true, false → BOOL; else ANY) and joins them (`int|float` → NUM, `int|false` → ANY); `isVoid: Boolean` is true for the `void` keyword.
- **`Classifier`** — Enum `CLASS`, `INTERFACE`, `TRAIT`, `ENUM`.
- **`Visibility`** — Enum `PUBLIC`, `PROTECTED`, `PRIVATE`.

## Configuration

- Keyword type vocabulary: `string`, `int`, `float`, `bool`, `array`, `object`, `callable`, `resource`, `mixed`, `void`, `null`, `iterable`. Any other spelling must be a class name.
- Enum values decode case-insensitively from lowercase file vocabulary (`classifier: class`, `visibility: public`).

## Gotchas

- `::` grammar is closed: exactly one separator, both sides non-empty; `$` is mandatory for property names and forbidden elsewhere; leading namespace slashes are stripped.
- `PropertySubject` and `VariableSubject` store names without the `$`; `VariableSubject("_get")` matches the spelling `$_GET`.
- A signature subtype that does not match the entry's subject kind is rejected at load.
- `variable` entries never carry a signature — superglobals are hand-declared.
- The returns classification is derived from `returnType` only when the entry declares propagation; a signature-only entry asserts existence, not purity.
