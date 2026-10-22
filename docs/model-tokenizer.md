# PHP Models — Tokenizer Language

Domain semantics of the lexical language a vulnerability type may declare.
Base entities — Vulnerability Type, Vocabulary: [model.md](model.md).

## Entities

- **Tokenizer Language** — The deterministic lexical language of the text a
  value of one vulnerability type is written into: for cross-site scripting,
  an HTML document; for link injection, the same document seen at the
  position of a URL. Existence condition: declared on the vocabulary entry of
  a vulnerability type. Identity is the declaration. A vulnerability type
  declares at most one, so every sink of the type reads the same language.
- **Tokenizer State** — One state of the language. A *tracked* state also
  carries the lowercased prefix of a word spelled since the state was
  entered; each prefix is its own state, written `name:prefix`. Identity is
  the state name.
- **Start State** — The state a text begins in.
- **Target States** — The states at which a value written into the text lands
  in the position the vulnerability type attacks. A language declares one or
  more; a language whose attacked position is reached through several quoting
  forms has one target state per form.
- **Rule** — A byte class and a next state, optionally conditioned on a
  complete word. A state holds an ordered list of rules and an optional
  *otherwise* state for a byte no rule matches; absent *otherwise*, an
  unmatched byte keeps the state.
- **Word Set** — A named list of words a tracked state matches, ignoring
  ASCII case. A rule marked *if-word* applies only while the spelling since
  entry is a whole word of the set.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Vulnerability Type | Tokenizer Language | declares | 1:0..1 | The language of the text a value of the type is written into |
| Source Kind | Tokenizer Language | never declares | 1:0 | A source kind has no written-into text |
| Tokenizer Language | Tokenizer State | has | 1:N | Every declared state, with a tracked state expanded to one state per word prefix |
| Tokenizer State | Tokenizer State | moves to, on a byte | N:1 | Total: every state has exactly one next state for each of the 256 byte values |
| Tokenizer Language | Target States | marks | 1:1..N | A non-empty subset of the untracked states |

## State Model

### Reading One Byte

| Step | Condition | Result |
|------|-----------|--------|
| 1 | a rule of the state matches the byte and, for an *if-word* rule, the spelling is a whole word | the rule's next state, in rule order |
| 2 | no rule applies and the state declares *otherwise* | the *otherwise* state |
| 3 | no rule applies and no *otherwise* | the same state |

- Entering a tracked state from another state starts the spelling with the
  entering byte. Staying in a tracked state extends the spelling by the byte.
  A spelling that is no prefix of any word of the set drops to the plain
  state name and stays there until the state is entered again.
- A text is read as a string of bytes, one per Latin-1 character.

## Invariants

- A state, word set and track name is lowercase words joined by hyphens. The
  colon is reserved for tracked prefixes, so no declared name collides with one.
- Every rule destination, *otherwise* state, track name, start and target
  names a declared item.
- The start state and every target state are untracked.
- A rule marked *if-word* sits in a tracked state.
- A word set is non-empty, and each of its words is non-empty and a Latin-1
  string.
- Two declarations of one vulnerability type across document sets are one
  declaration only when their languages are equal; a differing or missing
  language is a conflicting redeclaration.

## Boundaries

- What a consumer does with the language — which states a tainted value may
  land in, how a text with an unknown part is expanded — is the consumer's
  algorithm, not the format's. The format fixes only the transition function.
- The language is a recognizer for one attacked position. Nested content
  with its own lexical rules, such as script, style and raw text elements, is
  not modelled; a text inside such an element reads as the surrounding
  language reads it.
