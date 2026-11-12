# Glossary — Document Set instances

Document Set instances: how models are stored and published, how a consumer translates their names, and where their entries come from.

---

## Sets

### Document Set
- **Definition:** One source of models as it is stored: a root holding a manifest that lists the model documents in order, and optionally a Vocabulary, a Policy and a provenance document under fixed file names.
- **Example:** a root with `index.txt` listing `models.yaml` → one Document Set.
- **Source:** `DocumentSet.kt` `DocumentSet`; `DocumentSetLoader.kt` `DocumentSetLoader`

### Vocabulary Mapping
- **Definition:** A consumer-supplied translation, total over one Document Set, from each Vulnerability type and Source kind the set names to a name of the consumer's Vocabulary or to an explicit discard.
- **Example:** `vulnTypes: {html: xss}` → a Vocabulary Mapping from `html` to `xss`.
- **Source:** `VocabularyMapping.kt` `VocabularyMapping`

## Provenance

### Verification
- **Definition:** The closed kind stating how the entries of a Document Set were checked: generated, emitted by a program from an upstream source and never reviewed entry by entry, or manual, written or reviewed by hand.
- **Example:** `verification: manual` → the manual Verification value.
- **Source:** `Provenance.kt` `Verification`

| Member | Definition | Reference | Literature | Tools |
|--------|------------|-----------|------------|-------|
| `generated` | Entries were emitted by a program and were not reviewed individually. | none | none | none |
| `manual` | Entries were written or reviewed by hand. | none | none | none |

### Set Provenance
- **Definition:** The statement of where the entries of one Document Set come from, the identifier of the producer and the Verification, which travels with the set and applies to every entry it lists.
- **Example:** `producer: rules` with `verification: manual` → one Set Provenance value.
- **Source:** `Provenance.kt` `SetProvenance`; `docs/concept-provenance.md`

### Generated Layer
- **Definition:** The lowest configuration layer of a consumer: documents an extraction producer emits from upstream sources, marked by their provenance and never edited by hand, so that a correction is an entry of a higher layer.
- **Example:** extracted declaration Models marked generated → the Generated Layer of a consumer.
- **Source:** `docs/model-declarations.md` Entities; `Provenance.kt` `Verification` member `GENERATED`

### Precedence
- **Definition:** A total order over Verification values used by a consumer to rank competing Document Set entries.
- **Example:** the default order → manual ranks above generated.
- **Source:** `Provenance.kt` `Precedence`
