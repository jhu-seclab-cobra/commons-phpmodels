# Vocabulary and Policy

> The two closed declared sets — danger categories and origin colors — and the origin → categories taint policy.

## Quick Start

```kotlin
val set = DocumentSetLoader.load(opener)
val vocabulary = set.vocabulary
val policy = TaintPolicy(set.policy)
policy.isDangerous(vocabulary.requireOrigin("remote"), vocabulary.requireVulnClass("sqli"))
```

## API

- **`Vocabulary(vulnClasses: Map<VulnClassId, VulnClassDecl>, origins: Map<OriginId, OriginDecl>)`** — Sole authority for what category and color names exist.
- **`Vocabulary.requireVulnClass(raw: String): VulnClassId`** — Interns and validates a category name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.requireOrigin(raw: String): OriginId`** — Interns and validates a color name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.merge(other: Vocabulary): Vocabulary`** — Union in declaration order; an identical redeclaration is one declaration. Raises `VocabularyException` when a name is declared in both with different descriptions.
- **`Vocabulary.verify(entry: ModelEntry)`** — Checks every category and color the entry's sources, sinks, and sanitizers name is declared. Raises `VocabularyException` otherwise.
- **`Vocabulary.EMPTY`** — The vocabulary declaring nothing; the starting accumulator of a load.
- **`VulnClassId(id: String)`**, **`OriginId(id: String)`** — Interned lowercase reference tokens (value classes). Use past the load boundary instead of raw strings.
- **`VulnClassDecl(id: VulnClassId, description: String)`**, **`OriginDecl(id: OriginId, description: String)`** — One declared vocabulary entry.
- **`PolicyRow(origin: OriginId, enables: Set<VulnClassId>)`** — One policy statement.
- **`TaintPolicy(rows: List<PolicyRow>)`** — The folded origin → categories matrix; rows sharing an origin accumulate by union.
- **`TaintPolicy.isDangerous(color: OriginId, category: VulnClassId): Boolean`** — Whether the origin enables the category.

## Configuration

- Vocabulary document: two sections `vulnClasses` and `provenances` (YAML keys unchanged; the second decodes to `origins`), each a list of `{name, description}` entries.
- Policy document: a list of `{origin, enables}` rows.

## Gotchas

- Identifiers are lowercased at interning; declare and reference names in lowercase.
- A repeated name within a vocabulary section raises `VocabularyException` at load.
- `VocabularyException` extends `IllegalArgumentException`.
- `DocumentSetLoader` verifies every entry it returns; `verify(entry)` serves a consumer that builds entries by hand.
