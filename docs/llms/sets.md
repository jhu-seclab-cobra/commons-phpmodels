# Document Set instances

> One stored source of models — manifest, optional vocabulary, policy, and provenance, listed documents — loaded as a unit under the consumer's accumulated vocabulary, optionally translated through a Vocabulary Mapping.

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

- **`ResourceOpener.open(path: String): InputStream?`** — `fun interface`; the caller's storage. Path is relative to the set root; null when absent.
- **`DocumentSetLoader.load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY, mapping: VocabularyMapping? = null): DocumentSet`** — Reads `index.txt`, then `vocabulary.yaml` and `policy.yaml` when present, then every listed document in manifest order. Raises `DocumentSetException` (manifest or listed document absent, path listed twice, listed document malformed — the decode failure is the cause), `VocabularyException` (conflicting redeclaration, undeclared reference naming the document, undeclared mapping target, unlisted mapped name), `IllegalArgumentException` (malformed vocabulary, policy, or mapping document).
- **`DocumentSetLoader.MANIFEST`**, **`VOCABULARY`**, **`POLICY`**, **`PROVENANCE`** — The four fixed file names, directly under the root.
- **`DocumentSet(vocabulary: Vocabulary, policy: List<PolicyRow>, documents: List<Document>, provenance: SetProvenance? = null)`** — `vocabulary` is what this set contributed (empty for a mapped set); `entries` flattens the documents in order; `provenance` is null when `provenance.yaml` is absent.
- `provenance.yaml` decodes `producer:` and `verification:` inside the set load. Raises `IllegalArgumentException` on a stray key, a missing field, a blank producer, or a kind other than `generated`/`manual`.
- **`SetProvenance(producer: String, verification: Verification)`** — One set's declared provenance; `producer` non-blank.
- **`Verification`** — `GENERATED`, `MANUAL`.
- **`Precedence(order: List<Verification>)`** — `Comparator<Verification>`, highest first; `rank(kind: Verification): Int` is the position, `0` highest; `DEFAULT` is manual above generated. Raises `IllegalArgumentException` when a kind is missing or repeated.
- **`Document(path: String, entries: List<ModelEntry>)`** — One listed document.
- **`VocabularyMappingLoader.load(input: InputStream): VocabularyMapping`** — Decodes two maps, `vulnTypes:` and `sourceKinds:`, source name to target name or the literal `ignore` (`VocabularyMappingLoader.IGNORE`).
- **`VocabularyMapping.vulnType(source: VulnTypeId): VulnTypeId?`**, **`sourceKind(source: SourceKindId): SourceKindId?`** — Target name, or null when discarded. Raises `VocabularyException` when unlisted.
- **`VocabularyMapping.apply(entry: ModelEntry): ModelEntry?`** — Translates sources, sinks, and sanitizers; null when an entry without a signature loses its last section.
- **`VocabularyMapping.apply(rows: List<PolicyRow>): List<PolicyRow>`** — Translates rows; a discarded Source kind or emptied row drops.
- **`DocumentSetException(path: String, detail: String, cause: Throwable? = null)`** — Extends `IllegalArgumentException`; `path` names the manifest or document; `cause` carries a malformed document's decode failure.

## Configuration

- Manifest `index.txt`: one document path per line, relative to the root; blank lines and lines starting with `#` are not entries; a path listed twice fails.
- Mapping document: `vulnTypes:` and `sourceKinds:` maps; both sections required (`{}` when empty); every target is declared in `context` at load; a null target or a source spelled `ignore` fails.
- Provenance document `provenance.yaml`: `producer: <identifier>` and `verification: generated|manual`; read for declared and mapped loads alike.

## Gotchas

- Without a mapping, `vocabulary.yaml` merges into `context`: a redeclaration is admitted only when its description is identical.
- With a mapping, `vocabulary.yaml` is ignored, the mapping must list every name the set uses, and the returned `vocabulary` is empty — a mapped set contributes no names.
- Translation never touches subjects, ports, conditions, signatures, Value unit instances, or the Set Provenance.
- `SetProvenance` is where a set's statements come from; `SourceKindId` is the Source kind of a tainted value. Neither substitutes for the other.
- The library ranks verification kinds only; folding two sets' entries for one subject is the consumer's, with mount order as the tie-break.
- Every stream the opener yields is closed by the load, whether or not the decode succeeds.
