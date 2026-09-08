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
 * membership in the declared set is checked by [Vocabulary.requireOrigin],
 * never by construction.
 *
 * @property id Lowercased origin-color name, as declared in the vocabulary.
 */
@JvmInline
public value class OriginId private constructor(
    public val id: String,
) {
    public companion object {
        /** Interns [raw], folding it to its lowercased declared form. */
        @JvmStatic
        @JsonCreator
        public operator fun invoke(raw: String): OriginId = OriginId(raw.lowercase())
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
public data class OriginDecl(
    val id: OriginId,
    val description: String,
)

/**
 * Raised at load time when a color or category reference is not declared in
 * the [Vocabulary], or when a vocabulary entry repeats. Never raised past the
 * load boundary.
 */
public class VocabularyException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/**
 * The two closed declared sets — danger categories and origin colors. Sole
 * authority for what category and color names exist. Policy rows are
 * validated against it at load ([PolicyLoader]); model-entry references are
 * validated by [verify], called by [DocumentSetLoader] or by a caller that
 * decodes single documents.
 *
 * @property vulnClasses Declared danger categories, keyed by interned identity.
 * @property origins Declared origin colors, keyed by interned identity.
 */
public data class Vocabulary(
    val vulnClasses: Map<VulnClassId, VulnClassDecl>,
    val origins: Map<OriginId, OriginDecl>,
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
        throw undeclared("vulnerability class", raw, vulnClasses.values.map { "${it.id.id} (${it.description})" })
    }

    /**
     * Interns [raw] to its [OriginId], validating it is declared.
     *
     * @param raw Origin-color tag from a YAML document (case-insensitive).
     * @return The interned identity of the declared origin color.
     * @throws VocabularyException If [raw] names no declared origin color.
     */
    public fun requireOrigin(raw: String): OriginId {
        val id = OriginId(raw)
        if (id in origins) return id
        throw undeclared("origin", raw, origins.values.map { "${it.id.id} (${it.description})" })
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
            origins = origins.merged(other.origins, "origin") { it.description },
        )

    /**
     * Checks that every category and color [entry] references is declared.
     *
     * @param entry A model entry; its sources, sinks, and sanitizers are read.
     * @throws VocabularyException If a referenced name is undeclared.
     */
    public fun verify(entry: ModelEntry) {
        val body = entry.body
        body.sources?.forEach { source -> source.origin.forEach(::requireDeclared) }
        body.sinks?.forEach { sink -> requireDeclared(sink.vulnClass) }
        body.sanitizers?.forEach { sanitizer -> sanitizer.categories.forEach(::requireDeclared) }
    }

    private fun requireDeclared(id: VulnClassId) {
        requireVulnClass(id.id)
    }

    private fun requireDeclared(id: OriginId) {
        requireOrigin(id.id)
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
