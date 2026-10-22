# PHP Models — Tokenizer Language Design

The type a vulnerability type's vocabulary entry holds. Semantics:
[model-tokenizer.md](model-tokenizer.md). The entry types and the loader:
[design.md](design.md).

## Design Overview

- **Classes:** `TokenizerLanguage`, `TokenizerState` (value class) — public;
  `TokenizerDecl`, `StateDecl`, `RuleDecl` (declaration data classes) and
  `TokenizerAutomaton` — internal.
- **Relationships:** `VulnTypeDecl` holds an optional `TokenizerLanguage`;
  `TokenizerLanguage` wraps one validated `TokenizerDecl` and the
  `TokenizerAutomaton` compiled from it. Arrows one-way.
- **Exceptions:** `IllegalArgumentException` from the `init` of
  `TokenizerDecl` and from `step` and `run` on a byte or state outside the
  language; `VocabularyException` when a merge meets two different languages.
- **Dependency roles:** Data holder: `TokenizerDecl` and its parts. Compiler:
  `TokenizerAutomaton`. Value type: `TokenizerLanguage`, built only by the
  decoder.

## Component Design

### TokenizerLanguage

**Responsibility:** The language as a consumer sees it: a start state, target
states, every state, and the transition function over bytes and strings.

**State/Fields:** `start: TokenizerState`, `targets: Set<TokenizerState>`,
`states: Set<TokenizerState>` (every plain state and every tracked prefix).

**Methods:**
- `fun step(state, byte: Int): TokenizerState` — the next state on a byte in
  0..255; `IllegalArgumentException` on another byte or a state outside
  `states`.
- `fun run(state, text: String): TokenizerState` — folds `step` over the
  characters of `text`, one byte each; `IllegalArgumentException` on a
  character above U+00FF.

**Equality:** by declaration, so the merge of two vocabularies compares the
languages as written.

**Creation:** the decoder builds it from the `tokenizer` mapping of a
vulnerability type entry; no other public constructor exists.

### TokenizerState

**Responsibility:** The interned name of one state. A value class over
`String`; the constructor is internal, so a state comes only from a language.

### TokenizerDecl, StateDecl, RuleDecl

**Responsibility:** The declaration as written: `start`, `targets`, `words`
(word set name to words) and `states` (name to `StateDecl`); a `StateDecl`
holds ordered `rules`, an `otherwise` name and a `track` word set name; a
`RuleDecl` holds the `chars` class, the `next` state name and `ifWord`.

**Validation (`init` of `TokenizerDecl`):** every invariant of
[model-tokenizer.md](model-tokenizer.md) Invariants; the message names the
offending state, word set or word.

### TokenizerAutomaton

**Responsibility:** Compile a `TokenizerDecl` into a total transition table.
A tracked state expands into one position per prefix of its words, so the
table is a plain deterministic automaton with no word buffer.

**Algorithm:** for every position and every byte, the first rule whose class
holds the byte and whose word condition holds gives the destination, else the
`otherwise` state, else the same state. A destination that is a tracked state
starts a spelling with the byte when entered from another state, and extends
the spelling when it is the current state; a spelling that is no word prefix
drops to the plain state. ASCII letters fold to lowercase before they enter
a spelling.

**Cost:** time and space `positions × 256`, paid once at load.

## Vocabulary Entry Wire Shape

```yaml
vulnTypes:
  - name: schemei
    description: ...
    tokenizer:
      start: text
      targets: [url-before-value, url-bare-scheme, url-double-start]
      words:
        url-attribute: [href, src]
      states:
        text:
          rules:
            - {chars: '[<]', next: tag-open}
        attribute-name:
          track: url-attribute
          rules:
            - {chars: '[=]', next: url-before-value, ifWord: true}
            - {chars: '[=]', next: before-value}
```

`sourceKinds` entries carry no `tokenizer` key; the strict decoder rejects it.
