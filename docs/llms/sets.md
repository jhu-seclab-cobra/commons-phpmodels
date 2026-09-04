# Document Sets

> One stored source of models — manifest, optional vocabulary and policy, listed documents — loaded as a unit under the consumer's accumulated vocabulary, optionally translated through a category mapping.

## Quick Start

```kotlin
val opener = ResourceOpener { path -> javaClass.getResourceAsStream("/models/$path") }
var vocabulary = Vocabulary.EMPTY
val stubs = DocumentSetLoader.load(opener, vocabulary)
vocabulary = vocabulary.merge(stubs.vocabulary)

val mapping = CategoryMappingLoader.load(mappingYaml.byteInputStream())
val upstream = DocumentSetLoader.load(upstreamOpener, vocabulary, mapping)
val entries = stubs.entries + upstream.entries
```

## API

- **`ResourceOpener.open(path: String): InputStream?`** — `fun interface`; the caller's storage. Path is relative to the set root; null when absent.
- **`DocumentSetLoader.load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY, mapping: CategoryMapping? = null): DocumentSet`** — Reads `index.txt`, then `vocabulary.yaml` and `policy.yaml` when present, then every listed document in manifest order. Raises `DocumentSetException` (manifest or listed document absent, path listed twice, listed document malformed — the decode failure is the cause), `VocabularyException` (conflicting redeclaration, undeclared reference naming the document, undeclared mapping target, unlisted mapped name), `IllegalArgumentException` (malformed vocabulary, policy, or mapping document).
- **`DocumentSetLoader.MANIFEST`**, **`VOCABULARY`**, **`POLICY`** — The three fixed file names, directly under the root.
- **`DocumentSet(vocabulary: Vocabulary, policy: List<PolicyRow>, documents: List<Document>)`** — `vocabulary` is what this set contributed (empty for a mapped set); `entries` flattens the documents in order.
- **`Document(path: String, entries: List<ModelEntry>)`** — One listed document.
- **`CategoryMappingLoader.load(input: InputStream): CategoryMapping`** — Decodes two maps, `categories:` and `provenances:`, source name to target name or the literal `ignore`.
- **`CategoryMapping.category(source)`**, **`provenance(source)`** — Target name, or null when discarded. Raises `VocabularyException` when unlisted.
- **`CategoryMapping.apply(entry: ModelEntry): ModelEntry?`** — Translates sources, sinks, and sanitizers; null when an entry without a signature loses its last section.
- **`CategoryMapping.apply(rows: List<PolicyRow>): List<PolicyRow>`** — Translates rows; a discarded origin or emptied row drops.
- **`DocumentSetException(path, detail, cause?)`** — Extends `IllegalArgumentException`; `path` names the manifest or document; `cause` carries a malformed document's decode failure.

## Configuration

- Manifest `index.txt`: one document path per line, relative to the root; blank lines and lines starting with `#` are not entries; a path listed twice fails.
- Mapping document: `categories:` and `provenances:` maps; both sections required (`{}` when empty); every target is declared in `context` at load; a null target or a source spelled `ignore` fails.

## Gotchas

- Without a mapping, `vocabulary.yaml` merges into `context`: a redeclaration is admitted only when its description is identical.
- With a mapping, `vocabulary.yaml` is ignored, the mapping must list every name the set uses, and the returned `vocabulary` is empty — a mapped set contributes no names.
- Translation never touches subjects, ports, guards, signatures, or value semantics.
- Every stream the opener yields is closed by the load, whether or not the decode succeeds.
