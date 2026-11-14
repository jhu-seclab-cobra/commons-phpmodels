# Glossary — Vocabulary

The taint vocabulary: the named kinds of untrusted input, the named categories of vulnerability, and the policy that relates them.

---

## Taint Vocabulary

### Source kind
- **Definition:** A declared kind of untrusted input, identified by its lowercased name, that travels with a value along data flow.
- **Example:** `name: remote` → Source kind `remote`.
- **Source:** `Vocabulary.kt` `SourceKindId`, `SourceKindDecl`

### Vulnerability type
- **Definition:** A declared category of vulnerability a sink is sensitive to, identified by its lowercased name.
- **Example:** `name: sqli` → Vulnerability type `sqli`.
- **Source:** `Vocabulary.kt` `VulnTypeId`, `VulnTypeDecl`

### Vocabulary
- **Definition:** The two closed declared sets, of every Source kind and of every Vulnerability type, to which every name a model or policy row uses must belong.
- **Example:** a document declaring `remote` and `sqli` → a Vocabulary containing those two declarations.
- **Source:** `Vocabulary.kt` `Vocabulary`

### Policy
- **Definition:** The global relation from each Source kind to the Vulnerability type entries it can trigger, accumulated by union over the rows that share a Source kind.
- **Example:** `sourceKind: remote, enables: [sqli]` → a Policy relating remote input to SQL injection.
- **Source:** `TaintPolicy.kt` `TaintPolicy`, `PolicyRow`

## Tokenizer

### Tokenizer language
- **Definition:** The deterministic byte transition function declared by a Vulnerability type to recognize the position that its sinks attack in their output text.
- **Example:** A language whose text state moves to a tag state on `<` → the state after reading `<` is the tag state.
- **Source:** `TokenizerLanguage.kt` `TokenizerLanguage`; `TokenizerDecl.kt` `TokenizerDecl`; `TokenizerAutomaton.kt` `TokenizerAutomaton`

### Tokenizer state
- **Definition:** One named position of a Tokenizer language, including each ASCII-case-folded word prefix when a declared state tracks a word set.
- **Example:** A state `attribute-name` tracking `href` → `attribute-name:hre` after reading `hre` since entry.
- **Source:** `TokenizerLanguage.kt` `TokenizerState`
