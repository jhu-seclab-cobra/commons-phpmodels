# Glossary — Document Sets

Document sets: how models are stored and published, how a consumer translates their names, and where their entries come from.

---

## Sets

### Document Set
- **Definition:** One source of models as it is stored: a root holding a manifest that lists the model documents in order, and optionally a Vocabulary, a Policy and a provenance document under fixed file names.
- **Source:** `DocumentSet.kt` `DocumentSet`; `DocumentSetLoader.kt` `DocumentSetLoader`
- **Reference:** none

### Vocabulary Mapping
- **Definition:** A consumer-supplied translation, total over one Document Set, from each vulnerability type and source kind the set names to a name of the consumer's Vocabulary or to an explicit discard.
- **Source:** `VocabularyMapping.kt` `VocabularyMapping`
- **Reference:** none

## Provenance

### Verification
- **Definition:** The closed kind stating how the entries of a Document Set were checked: generated, emitted by a program from an upstream source and never reviewed entry by entry, or manual, written or reviewed by hand.
- **Source:** `Provenance.kt` `Verification`
- **Reference:** none

### Set Provenance
- **Definition:** The statement of where the entries of one Document Set come from, the identifier of the producer and the Verification, which travels with the set and applies to every entry it lists.
- **Source:** `Provenance.kt` `SetProvenance`; `docs/concept-provenance.md`
- **Reference:** none

### Generated Layer
- **Definition:** The lowest configuration layer of a consumer: documents an extraction producer emits from upstream sources, marked by their provenance and never edited by hand, so that a correction is an entry of a higher layer.
- **Source:** `docs/model-declarations.md` Entities; `Provenance.kt` `Verification.GENERATED`
- **Reference:** none
