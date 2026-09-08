# Document Sets

> One stored source of models — manifest, optional vocabulary, policy, and provenance, listed documents — loaded as a unit under the consumer's accumulated vocabulary, optionally translated through a category mapping.

## Quick Start

```kotlin
val opener = ResourceOpener { path -> javaClass.getResourceAsStream("/models/$path") }
var vocabulary = Vocabulary.EMPTY
val stubs = DocumentSetLoader.load(opener, vocabulary)
vocabulary = vocabulary.merge(stubs.vocabulary)

val mapping = CategoryMappingLoader.load(mappingYaml.byteInputStream())
val upstream = DocumentSetLoader.load(upstreamOpener, vocabulary, mapping)
val entries = stubs.entries + upstream.entries
val stubsWin = Precedence.DEFAULT.compare(stubs.provenance!!.verification, upstream.provenance!!.verification) > 0
```

## API

- **`ResourceOpener.open(path: String): InputStream?`** — `fun interface`; the caller's storage. Path is relative to the set root; null when absent.
- **`DocumentSetLoader.load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY, mapping: CategoryMapping? = null): DocumentSet`** — Reads `index.txt`, then `vocabulary.yaml` and `policy.yaml` when present, then every listed document in manifest order. Raises `DocumentSetException` (manifest or listed document absent, path listed twice, listed document malformed — the decode failure is the cause), `VocabularyException` (conflicting redeclaration, undeclared reference naming the document, undeclared mapping target, unlisted mapped name), `IllegalArgumentException` (malformed vocabulary, policy, or mapping document).
- **`DocumentSetLoader.MANIFEST`**, **`VOCABULARY`**, **`POLICY`**, **`PROVENANCE`** — The four fixed file names, directly under the root.
- **`DocumentSet(vocabulary: Vocabulary, policy: List<PolicyRow>, documents: List<Document>, provenance: SetProvenance? = null)`** — `vocabulary` is what this set contributed (empty for a mapped set); `entries` flattens the documents in order; `provenance` is null when `provenance.yaml` is absent.
- `provenance.yaml` decodes `producer:` and `verification:` inside the set load. Raises `IllegalArgumentException` on a stray key, a missing field, a blank producer, or a kind other than `generated`/`manual`.
- **`SetProvenance(producer: String, verification: Verification)`** — One set's declared provenance; `producer` non-blank.
- **`Verification`** — `GENERATED`, `MANUAL`.
- **`Precedence(order: List<Verification>)`** — `Comparator<Verification>`, highest first; `rank(kind)` is the position, `0` highest; `DEFAULT` is manual above generated. Raises `IllegalArgumentException` when a kind is missing or repeated.
- **`Document(path: String, entries: List<ModelEntry>)`** — One listed document.
- **`CategoryMappingLoader.load(input: InputStream): CategoryMapping`** — Decodes two maps, `categories:` and `provenances:`, source name to target name or the literal `ignore` (`CategoryMappingLoader.IGNORE`).
- **`CategoryMapping.category(source)`**, **`origin(source)`** — Target name, or null when discarded. Raises `VocabularyException` when unlisted.
- **`CategoryMapping.apply(entry: ModelEntry): ModelEntry?`** — Translates sources, sinks, and sanitizers; null when an entry without a signature loses its last section.
- **`CategoryMapping.apply(rows: List<PolicyRow>): List<PolicyRow>`** — Translates rows; a discarded origin or emptied row drops.
- **`DocumentSetException(path, detail, cause?)`** — Extends `IllegalArgumentException`; `path` names the manifest or document; `cause` carries a malformed document's decode failure.

## Configuration

- Manifest `index.txt`: one document path per line, relative to the root; blank lines and lines starting with `#` are not entries; a path listed twice fails.
- Mapping document: `categories:` and `provenances:` maps; both sections required (`{}` when empty); every target is declared in `context` at load; a null target or a source spelled `ignore` fails.
- Provenance document `provenance.yaml`: `producer: <identifier>` and `verification: generated|manual`; read for declared and mapped loads alike.

## Gotchas

- Without a mapping, `vocabulary.yaml` merges into `context`: a redeclaration is admitted only when its description is identical.
- With a mapping, `vocabulary.yaml` is ignored, the mapping must list every name the set uses, and the returned `vocabulary` is empty — a mapped set contributes no names.
- Translation never touches subjects, ports, conditions, signatures, value semantics, or the set provenance.
- `SetProvenance` is where a set's statements come from; `OriginId` is the origin color of a tainted value. Neither substitutes for the other.
- The library ranks verification kinds only; folding two sets' entries for one subject is the consumer's, with mount order as the tie-break.
- Every stream the opener yields is closed by the load, whether or not the decode succeeds.
