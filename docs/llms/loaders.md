# Loaders

> Public decode surface: one loader object per document kind, reading a caller-supplied `InputStream`.

## Quick Start

```kotlin
val vocabulary = VocabularyLoader.load(vocabYaml.byteInputStream())
val policy = PolicyLoader.load(policyYaml.byteInputStream(), vocabulary)
val entries = ModelLoader.load(modelsYaml.byteInputStream())
```

## API

- **`ModelLoader.load(input: InputStream): List<ModelEntry>`** — Decodes one model document's entries in file order. Raises `IllegalArgumentException` on any format violation.
- **`VocabularyLoader.load(input: InputStream): Vocabulary`** — Decodes the two-axis vocabulary. Raises `VocabularyException` when a name repeats within a section.
- **`PolicyLoader.load(input: InputStream, vocabulary: Vocabulary): List<PolicyRow>`** — Decodes policy rows, interning every tag against `vocabulary`. Raises `VocabularyException` on an undeclared color or category.

## Configuration

- No configuration. Document location — classpath resource, file, artifact — is the caller's choice; loaders take streams only.
- Vocabulary document keys: `vulnClasses`, `provenances`; each entry carries `name` and `description`.
- Policy row keys: `origin` (one provenance name), `enables` (list of vuln-class names).

## Gotchas

- Decode is strict: an unknown or stray key anywhere raises `IllegalArgumentException`.
- The input stream is consumed and closed by the load call.
- `VocabularyException` extends `IllegalArgumentException`; one catch covers both.
- `ModelLoader` does not intern the color and category strings inside decoded entries — pass them through `Vocabulary.requireProvenance` / `requireVulnClass` after loading.
- Load order: vocabulary first, then policy; model documents load independently.
