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
- Model entries decoded by `ModelLoader` carry raw color/category strings — intern them through `requireProvenance` / `requireVulnClass` yourself.
