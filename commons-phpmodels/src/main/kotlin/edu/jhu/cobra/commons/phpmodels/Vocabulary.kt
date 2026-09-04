package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Interned reference token for a declared danger category. Replaces raw
 * strings past the load boundary, so a name mismatch cannot occur downstream.
 * Construction folds any spelling to lowercase, so a mixed-case reference
 * cannot miss a vocabulary lookup.
 *
 * @property id Lowercased category name, as declared in the vocabulary.
 */
@JvmInline
public value class VulnClassId private constructor(
    public val id: String,
) {
    public companion object {
        /** Interns [raw], folding it to its lowercased declared form. */
        @JvmStatic
        @JsonCreator
        public operator fun invoke(raw: String): VulnClassId = VulnClassId(raw.lowercase())
    }
}

/**
 * Interned reference token for a declared origin color (provenance). Travels
 * with a tainted value from its source. Construction folds any spelling to
 * lowercase, so a mixed-case reference cannot miss a vocabulary lookup;
 * membership in the declared set is checked by [Vocabulary.requireProvenance],
 * never by construction.
 *
 * @property id Lowercased origin-color name, as declared in the vocabulary.
 */
@JvmInline
public value class ProvenanceId private constructor(
    public val id: String,
) {
    public companion object {
        /** Interns [raw], folding it to its lowercased declared form. */
        @JvmStatic
        @JsonCreator
        public operator fun invoke(raw: String): ProvenanceId = ProvenanceId(raw.lowercase())
    }
}

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
 * authority for what category and color names exist. Policy rows are
 * validated against it at load ([PolicyLoader]); model-entry references are
 * validated by [verify], called by [DocumentSetLoader] or by a caller that
 * decodes single documents.
 *
 * @property vulnClasses Declared danger categories, keyed by interned identity.
 * @property provenances Declared origin colors, keyed by interned identity.
 */
public data class Vocabulary(
    val vulnClasses: Map<VulnClassId, VulnClassDecl>,
    val provenances: Map<ProvenanceId, ProvenanceDecl>,
) {
    /**
     * Interns [raw] to its [VulnClassId], validating it is declared.
     *
     * @param raw Category tag from a YAML document (case-insensitive).
     * @return The interned identity of the declared category.
     * @throws VocabularyException If [raw] names no declared category.
     */
    public fun requireVulnClass(raw: String): VulnClassId {
        val id = VulnClassId(raw)
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
        val id = ProvenanceId(raw)
        if (id in provenances) return id
        throw undeclared("provenance", raw, provenances.keys.map { it.id })
    }

    /**
     * Unions this vocabulary with [other]. A name declared in both is one
     * declaration when the descriptions match; a differing description is a
     * conflicting redeclaration, never a silent override.
     *
     * @param other The vocabulary a later document set contributes.
     * @return The union, this vocabulary's entries first.
     * @throws VocabularyException If a name is declared in both with different descriptions.
     */
    public fun merge(other: Vocabulary): Vocabulary =
        Vocabulary(
            vulnClasses = vulnClasses.merged(other.vulnClasses, "vulnerability class") { it.description },
            provenances = provenances.merged(other.provenances, "provenance") { it.description },
        )

    /**
     * Checks that every category and color [entry] references is declared.
     *
     * @param entry A flat model or generator; its sources, sinks, and sanitizers are read.
     * @throws VocabularyException If a referenced name is undeclared.
     */
    public fun verify(entry: ModelEntry) {
        val body =
            when (entry) {
                is SubjectModel -> entry.body
                is ModelGenerator -> entry.model
            }
        body.sources?.forEach { source -> source.provenance.forEach(::requireDeclared) }
        body.sinks?.forEach { sink -> requireDeclared(sink.category) }
        body.sanitizers?.forEach { sanitizer -> sanitizer.categories.forEach(::requireDeclared) }
    }

    private fun requireDeclared(id: VulnClassId) {
        requireVulnClass(id.id)
    }

    private fun requireDeclared(id: ProvenanceId) {
        requireProvenance(id.id)
    }

    private fun <K, V> Map<K, V>.merged(
        other: Map<K, V>,
        kind: String,
        description: (V) -> String,
    ): Map<K, V> {
        val union = LinkedHashMap(this)
        for ((id, decl) in other) {
            val existing = union.put(id, decl) ?: continue
            if (description(existing) != description(decl)) {
                throw VocabularyException(
                    "Conflicting $kind redeclaration '$id': '${description(existing)}' vs '${description(decl)}'",
                )
            }
        }
        return union
    }

    private fun undeclared(
        kind: String,
        raw: String,
        declared: List<String>,
    ): VocabularyException = VocabularyException("Unknown $kind: '$raw' (declared: $declared)")

    public companion object {
        /** The vocabulary declaring nothing; the starting accumulator of a load. */
        public val EMPTY: Vocabulary = Vocabulary(emptyMap(), emptyMap())
    }
}
