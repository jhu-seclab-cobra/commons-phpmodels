# Subjects and Signatures

> The PHP declaration a model identifies, and the decoded signature section per declaration kind.

## Quick Start

```kotlin
val subject = MethodSubject.parse("mysqli::query")
val model = ModelLoader.load(yaml.byteInputStream()).single() as SubjectModel
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
- **`SignatureInfo.CallableSignature(params: List<ParameterInfo>, returnType: DeclaredType)`** — function, method.
- **`SignatureInfo.ClassSignature(classifier: Classifier, parent: String?, interfaces: List<String>)`** — class; parent and interfaces case-folded.
- **`SignatureInfo.TypedSignature(type: DeclaredType, value: String?)`** — constant, class constant; `value` is the spelled literal, null when the source states none.
- **`SignatureInfo.PropertySignature(type: DeclaredType, visibility: Visibility, static: Boolean)`** — property.
- **`ParameterInfo(name: String, type: DeclaredType, optional: Boolean, byRef: Boolean, variadic: Boolean)`** — one declared parameter; position is list order.
- **`DeclaredType(raw: String)`** — Value class over a keyword type or class name; `toReturnKind(): ReturnKind` derives the classification (string → STR; int, float → NUM; bool → BOOL; else ANY); `isVoid: Boolean` is true for the `void` keyword.
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
