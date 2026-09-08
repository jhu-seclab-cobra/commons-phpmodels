# Loaders

> Public decode surface: `DocumentSetLoader` over one document set, `CategoryMappingLoader` over a consumer's mapping document.

## Quick Start

```kotlin
val set = DocumentSetLoader.load(opener, context = Vocabulary.EMPTY)
val mapping = CategoryMappingLoader.load(mappingYaml.byteInputStream())
val mapped = DocumentSetLoader.load(otherOpener, context = set.vocabulary, mapping = mapping)
```

## API

- **`DocumentSetLoader.load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY, mapping: CategoryMapping? = null): DocumentSet`** — Loads one set: manifest, `provenance.yaml`, `vocabulary.yaml`, `policy.yaml`, listed model documents ([sets.md](sets.md)).
- **`CategoryMappingLoader.load(input: InputStream): CategoryMapping`** — Decodes a consumer's translation table.

The single-document loaders (`VocabularyLoader`, `PolicyLoader`, `ModelLoader`, `ProvenanceLoader`) are internal: a consumer loads a set, never a single document.

## Configuration

- No configuration. Document location — classpath resource, file, artifact — is the caller's choice; the set loader takes a `ResourceOpener`, the mapping loader a stream.
- Vocabulary document keys: `vulnClasses`, `provenances` — both required (`[]` when empty); each entry carries `name` and `description`.
- Policy row keys: `origin` (one origin-color name), `enables` (list of vuln-class names).

## Gotchas

- Decode is strict: an unknown or stray key anywhere raises `IllegalArgumentException`.
- `VocabularyException` and `DocumentSetException` extend `IllegalArgumentException`; one catch covers all.
- Every stream the opener yields is closed by the set load; the mapping loader consumes its stream without closing it.
- Load order inside a set: provenance, vocabulary, policy, then model documents in manifest order; every entry is verified against the accumulated vocabulary.
