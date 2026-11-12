# Vocabulary and Policy

> The two closed declared sets — Vulnerability type entries and Source kind entries — and the Source kind → Vulnerability type entries taint policy.

## Quick Start

```kotlin
import edu.jhu.cobra.commons.phpmodels.SourceKindDecl
import edu.jhu.cobra.commons.phpmodels.SourceKindId
import edu.jhu.cobra.commons.phpmodels.Vocabulary
import edu.jhu.cobra.commons.phpmodels.VulnTypeDecl
import edu.jhu.cobra.commons.phpmodels.VulnTypeId
val source = SourceKindDecl(SourceKindId("remote"), "Remote input")
val vuln = VulnTypeDecl(VulnTypeId("sqli"), "SQL injection")
val vocabulary = Vocabulary(mapOf(vuln.id to vuln), mapOf(source.id to source))
vocabulary.requireSourceKind("remote")
```

## API

- **`Vocabulary(vulnTypes: Map<VulnTypeId, VulnTypeDecl>, sourceKinds: Map<SourceKindId, SourceKindDecl>)`** — Sole authority for what Vulnerability type and Source kind names exist.
- **`Vocabulary.requireVulnType(raw: String): VulnTypeId`** — Normalizes and validates a Vulnerability type name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.requireSourceKind(raw: String): SourceKindId`** — Normalizes and validates a Source kind name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.merge(other: Vocabulary): Vocabulary`** — Union in declaration order; an identical redeclaration is one declaration. Raises `VocabularyException` when a name is declared in both with different descriptions.
- **`Vocabulary.verify(entry: ModelEntry): Unit`** — Checks every Vulnerability type and Source kind the entry's sources, sinks, and sanitizers name is declared. Raises `VocabularyException` otherwise.
- **`Vocabulary.EMPTY`** — The vocabulary declaring nothing; the starting accumulator of a load.
- **`VulnTypeId(raw: String)`**, **`SourceKindId(raw: String)`** — Lowercased reference tokens (value classes; companion `invoke`, constructor private, `id` is the lowercased name). Use past the load boundary instead of raw strings.
- **`VulnTypeDecl(id: VulnTypeId, description: String)`**, **`SourceKindDecl(id: SourceKindId, description: String)`** — One declared vocabulary entry.
- **`PolicyRow(sourceKind: SourceKindId, enables: Set<VulnTypeId>)`** — One policy statement.
- **`TaintPolicy(rows: List<PolicyRow>)`** — The folded Source kind → Vulnerability type entries matrix; rows sharing a Source kind accumulate by union.
- **`TaintPolicy.isDangerous(sourceKind: SourceKindId, vulnType: VulnTypeId): Boolean`** — Whether the Source kind enables the Vulnerability type.

## Configuration

- Vocabulary document: two sections `vulnTypes` and `sourceKinds`, each a list of `{name, description}` entries.
- Policy document: a list of `{sourceKind, enables}` rows.

## Gotchas

- Identifiers are lowercased at construction; declare and reference names in lowercase.
- A repeated name within a vocabulary section raises `VocabularyException` at load.
- `VocabularyException` extends `IllegalArgumentException`.
- `DocumentSetLoader` verifies every entry it returns; `verify(entry)` serves a consumer that builds entries by hand.
