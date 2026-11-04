# Glossary — Vocabulary

The taint vocabulary: the named kinds of untrusted input, the named categories of vulnerability, and the policy that relates them.

---

## Taint Vocabulary

### Source kind
- **Definition:** A declared kind of untrusted input, identified by its lowercased name, that travels with a value along data flow.
- **Source:** `Vocabulary.kt` `SourceKindId`, `SourceKindDecl`
- **Reference:** none

### Vulnerability type
- **Definition:** A declared category of vulnerability a sink is sensitive to, identified by its lowercased name.
- **Source:** `Vocabulary.kt` `VulnTypeId`, `VulnTypeDecl`
- **Reference:** none

### Vocabulary
- **Definition:** The two closed declared sets, of every source kind and of every vulnerability type, to which every name a model or policy row uses must belong.
- **Source:** `Vocabulary.kt` `Vocabulary`
- **Reference:** none

### Policy
- **Definition:** The global relation from each source kind to the vulnerability types it can trigger, accumulated by union over the rows that share a source kind.
- **Source:** `TaintPolicy.kt` `TaintPolicy`, `PolicyRow`
- **Reference:** none
