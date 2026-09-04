# Vocabulary and Policy

> The two closed declared sets — danger categories and origin colors — and the origin → categories taint policy.

## Quick Start

```kotlin
val vocabulary = VocabularyLoader.load(vocabYaml.byteInputStream())
val policy = TaintPolicy(PolicyLoader.load(policyYaml.byteInputStream(), vocabulary))
policy.isDangerous(vocabulary.requireProvenance("remote"), vocabulary.requireVulnClass("sqli"))
```

## API

- **`Vocabulary(vulnClasses: Map<VulnClassId, VulnClassDecl>, provenances: Map<ProvenanceId, ProvenanceDecl>)`** — Sole authority for what category and color names exist.
- **`Vocabulary.requireVulnClass(raw: String): VulnClassId`** — Interns and validates a category name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.requireProvenance(raw: String): ProvenanceId`** — Interns and validates a color name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.merge(other: Vocabulary): Vocabulary`** — Union in declaration order; an identical redeclaration is one declaration. Raises `VocabularyException` when a name is declared in both with different descriptions.
- **`Vocabulary.verify(entry: ModelEntry)`** — Checks every category and color the entry's sources, sinks, and sanitizers name is declared. Raises `VocabularyException` otherwise.
- **`Vocabulary.EMPTY`** — The vocabulary declaring nothing; the starting accumulator of a load.
- **`VulnClassId(id: String)`**, **`ProvenanceId(id: String)`** — Interned lowercase reference tokens (value classes). Use past the load boundary instead of raw strings.
- **`VulnClassDecl(id: VulnClassId, description: String)`**, **`ProvenanceDecl(id: ProvenanceId, description: String)`** — One declared vocabulary entry.
- **`PolicyRow(origin: ProvenanceId, enables: Set<VulnClassId>)`** — One policy statement.
- **`TaintPolicy(rows: List<PolicyRow>)`** — The folded origin → categories matrix; rows sharing an origin accumulate by union.
- **`TaintPolicy.isDangerous(color: ProvenanceId, category: VulnClassId): Boolean`** — Whether the origin enables the category.

## Configuration

- Vocabulary document: two sections `vulnClasses` and `provenances`, each a list of `{name, description}` entries.
- Policy document: a list of `{origin, enables}` rows.

## Gotchas

- Identifiers are lowercased at interning; declare and reference names in lowercase.
- A repeated name within a vocabulary section raises `VocabularyException` at load.
- `VocabularyException` extends `IllegalArgumentException`.
- Model entries decoded by `ModelLoader` are not checked against any vocabulary — call `verify(entry)` yourself, or load through `DocumentSetLoader`.
