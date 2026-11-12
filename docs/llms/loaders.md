# Loaders

> Public decode surface: `DocumentSetLoader` over one Document Set, `VocabularyMappingLoader` over a consumer's mapping document.

## Quick Start

```kotlin
import edu.jhu.cobra.commons.phpmodels.DocumentSetLoader
import edu.jhu.cobra.commons.phpmodels.ResourceOpener
val yaml = "- subject: {function: example}
  returns: str
"
val files = mapOf("index.txt" to "models.yaml
", "models.yaml" to yaml)
val set = DocumentSetLoader.load(ResourceOpener { files[it]?.byteInputStream() })
set.entries.single().body.returns
```

## API

- **`DocumentSetLoader.load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY, mapping: VocabularyMapping? = null): DocumentSet`** — Loads one set: manifest, `provenance.yaml`, `vocabulary.yaml`, `policy.yaml`, listed model documents ([sets.md](sets.md)).
- **`VocabularyMappingLoader.load(input: InputStream): VocabularyMapping`** — Decodes a consumer's translation table.

- Load one complete set rather than an individual document.

## Configuration

- No configuration. Document location — classpath resource, file, artifact — is the caller's choice; the set loader takes a `ResourceOpener`, the mapping loader a stream.
- Vocabulary document keys: `vulnTypes`, `sourceKinds` — both required (`[]` when empty); each entry carries `name` and `description`.
- Policy row keys: `sourceKind` (one Source kind name), `enables` (list of Vulnerability type names).

## Gotchas

- Decode is strict: an unknown or stray key anywhere raises `IllegalArgumentException`.
- `VocabularyException` and `DocumentSetException` extend `IllegalArgumentException`; one catch covers all.
- Every stream the opener yields is closed by the set load; the mapping loader consumes its stream without closing it.
- Load order inside a set: provenance, vocabulary, policy, then model documents in manifest order; every entry is verified against the accumulated vocabulary.
