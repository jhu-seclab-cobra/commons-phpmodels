package edu.jhu.cobra.commons.phpmodels

/**
 * Interned reference token for a declared danger category. Replaces raw
 * strings past the load boundary, so a name mismatch cannot occur downstream.
 *
 * @property id Lowercased category name, as declared in the vocabulary.
 */
@JvmInline
public value class VulnClassId(
    public val id: String,
)

/**
 * Interned reference token for a declared origin color (provenance). Travels
 * with a tainted value from its source; validated against the vocabulary at
 * load time.
 *
 * @property id Lowercased origin-color name, as declared in the vocabulary.
 */
@JvmInline
public value class ProvenanceId(
    public val id: String,
)

/**
 * One declared danger-category entry.
 *
 * @property id Interned category identity.
 * @property description Human-readable summary; self-documents the vocabulary
 *   file and enriches the undeclared-reference error message.
 */
public data class VulnClassDecl(
    val id: VulnClassId,
    val description: String,
)

/**
 * One declared origin-color entry.
 *
 * @property id Interned origin-color identity.
 * @property description Human-readable summary; self-documents the vocabulary
 *   file and enriches the undeclared-reference error message.
 */
public data class ProvenanceDecl(
    val id: ProvenanceId,
    val description: String,
)

/**
 * Raised at load time when a color or category reference is not declared in
 * the [Vocabulary], or when a vocabulary entry repeats. Never raised past the
 * load boundary.
 */
public class VocabularyException(
    message: String,
) : IllegalArgumentException(message)

/**
 * The two closed declared sets — danger categories and origin colors. Sole
 * authority for what category and color names exist; every reference by a
 * policy row or model entry is validated against it.
 *
 * @property vulnClasses Declared danger categories, keyed by interned identity.
 * @property provenances Declared origin colors, keyed by interned identity.
 */
public data class Vocabulary(
    val vulnClasses: Map<VulnClassId, VulnClassDecl>,
    val provenances: Map<ProvenanceId, ProvenanceDecl> = emptyMap(),
) {
    /**
     * Interns [raw] to its [VulnClassId], validating it is declared.
     *
     * @param raw Category tag from a YAML document (case-insensitive).
     * @return The interned identity of the declared category.
     * @throws VocabularyException If [raw] names no declared category.
     */
    public fun requireVulnClass(raw: String): VulnClassId {
        val id = VulnClassId(raw.lowercase())
        if (id in vulnClasses) return id
        throw undeclared("vulnerability class", raw, vulnClasses.keys.map { it.id })
    }

    /**
     * Interns [raw] to its [ProvenanceId], validating it is declared.
     *
     * @param raw Origin-color tag from a YAML document (case-insensitive).
     * @return The interned identity of the declared origin color.
     * @throws VocabularyException If [raw] names no declared origin color.
     */
    public fun requireProvenance(raw: String): ProvenanceId {
        val id = ProvenanceId(raw.lowercase())
        if (id in provenances) return id
        throw undeclared("provenance", raw, provenances.keys.map { it.id })
    }

    private fun undeclared(
        kind: String,
        raw: String,
        declared: List<String>,
    ): VocabularyException = VocabularyException("Unknown $kind: '$raw' (declared: $declared)")
}
