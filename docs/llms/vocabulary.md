# Vocabulary and Policy

> The two closed declared sets — vulnerability types and source kinds — and the source kind → vulnerability types taint policy.

## Quick Start

```kotlin
val set = DocumentSetLoader.load(opener)
val vocabulary = set.vocabulary
val policy = TaintPolicy(set.policy)
policy.isDangerous(vocabulary.requireSourceKind("remote"), vocabulary.requireVulnType("sqli"))
```

## API

- **`Vocabulary(vulnTypes: Map<VulnTypeId, VulnTypeDecl>, sourceKinds: Map<SourceKindId, SourceKindDecl>)`** — Sole authority for what vulnerability type and source kind names exist.
- **`Vocabulary.requireVulnType(raw: String): VulnTypeId`** — Interns and validates a vulnerability type name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.requireSourceKind(raw: String): SourceKindId`** — Interns and validates a source kind name. Raises `VocabularyException` when undeclared.
- **`Vocabulary.merge(other: Vocabulary): Vocabulary`** — Union in declaration order; an identical redeclaration is one declaration. Raises `VocabularyException` when a name is declared in both with different descriptions or tokenizer languages.
- **`Vocabulary.verify(entry: ModelEntry)`** — Checks every vulnerability type and source kind the entry's sources, sinks, and sanitizers name is declared. Raises `VocabularyException` otherwise.
- **`Vocabulary.EMPTY`** — The vocabulary declaring nothing; the starting accumulator of a load.
- **`VulnTypeId(raw: String)`**, **`SourceKindId(raw: String)`** — Interned lowercase reference tokens (value classes; companion `invoke`, constructor private, `id` is the lowercased name). Use past the load boundary instead of raw strings.
- **`VulnTypeDecl(id: VulnTypeId, description: String, tokenizer: TokenizerLanguage? = null)`**, **`SourceKindDecl(id: SourceKindId, description: String)`** — One declared vocabulary entry.
- **`TokenizerLanguage`** — The deterministic lexical language of the text a value of the vulnerability type is written into. `start: TokenizerState`, `targets: Set<TokenizerState>`, `states: Set<TokenizerState>`; `step(state, byte: Int): TokenizerState` reads one byte in 0..255, `run(state, text: String): TokenizerState` reads one Latin-1 character per byte. Equal when the declarations are equal. Raises `IllegalArgumentException` on a byte, character or state outside the language.
- **`TokenizerState(name: String)`** — Value class over the state name; a tracked state is `name:prefix`. Obtained from a language, never constructed by a consumer.
- **`PolicyRow(sourceKind: SourceKindId, enables: Set<VulnTypeId>)`** — One policy statement.
- **`TaintPolicy(rows: List<PolicyRow>)`** — The folded source kind → vulnerability types matrix; rows sharing a source kind accumulate by union.
- **`TaintPolicy.isDangerous(sourceKind: SourceKindId, vulnType: VulnTypeId): Boolean`** — Whether the source kind enables the vulnerability type.

## Configuration

- Vocabulary document: two sections `vulnTypes` and `sourceKinds`, each a list of `{name, description}` entries; a `vulnTypes` entry may add `tokenizer: {start, targets, words, states}`, whose states hold ordered `rules` (`{chars, next, ifWord}`), an `otherwise` state and a `track` word set.
- Policy document: a list of `{sourceKind, enables}` rows.

## Gotchas

- Identifiers are lowercased at interning; declare and reference names in lowercase.
- A repeated name within a vocabulary section raises `VocabularyException` at load.
- `VocabularyException` extends `IllegalArgumentException`.
- `DocumentSetLoader` verifies every entry it returns; `verify(entry)` serves a consumer that builds entries by hand.
