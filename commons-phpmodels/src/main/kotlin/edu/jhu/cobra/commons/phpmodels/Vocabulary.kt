package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Interned reference token for a declared vulnerability type. Replaces raw
 * strings past the load boundary, so a name mismatch cannot occur downstream.
 * Construction folds any spelling to lowercase, so a mixed-case reference
 * cannot miss a vocabulary lookup.
 *
 * @property id Lowercased vulnerability type name, as declared in the vocabulary.
 */
@JvmInline
public value class VulnTypeId private constructor(
    public val id: String,
) {
    public companion object {
        /** Interns [raw], folding it to its lowercased declared form. */
        @JvmStatic
        @JsonCreator
        public operator fun invoke(raw: String): VulnTypeId = VulnTypeId(raw.lowercase())
    }
}

/**
 * Interned reference token for a declared source kind. Travels with a
 * tainted value from its source. Construction folds any spelling to
 * lowercase, so a mixed-case reference cannot miss a vocabulary lookup;
 * membership in the declared set is checked by [Vocabulary.requireSourceKind],
 * never by construction.
 *
 * @property id Lowercased source kind name, as declared in the vocabulary.
 */
@JvmInline
public value class SourceKindId private constructor(
    public val id: String,
) {
    public companion object {
        /** Interns [raw], folding it to its lowercased declared form. */
        @JvmStatic
        @JsonCreator
        public operator fun invoke(raw: String): SourceKindId = SourceKindId(raw.lowercase())
    }
}

/**
 * One declared vulnerability type entry.
 *
 * @property id Interned vulnerability type identity.
 * @property description Human-readable summary; self-documents the vocabulary
 *   file and enriches the undeclared-reference error message.
 * @property tokenizer The lexical language of the text a value of this type is written into,
 *   or null when the type declares none.
 */
public data class VulnTypeDecl(
    val id: VulnTypeId,
    val description: String,
    val tokenizer: TokenizerLanguage? = null,
)

/**
 * One declared source kind entry.
 *
 * @property id Interned source kind identity.
 * @property description Human-readable summary; self-documents the vocabulary
 *   file and enriches the undeclared-reference error message.
 */
public data class SourceKindDecl(
    val id: SourceKindId,
    val description: String,
)

/**
 * Raised at load time when a vulnerability type or source kind reference is
 * not declared in the [Vocabulary], or when a vocabulary entry repeats. Never
 * raised past the load boundary.
 */
public class VocabularyException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/**
 * The two closed declared sets — vulnerability types and source kinds. Sole
 * authority for what vulnerability type and source kind names exist. Policy rows are
 * validated against it at load ([PolicyLoader]); model-entry references are
 * validated by [verify], called by [DocumentSetLoader] or by a caller that
 * decodes single documents.
 *
 * @property vulnTypes Declared vulnerability types, keyed by interned identity.
 * @property sourceKinds Declared source kinds, keyed by interned identity.
 */
public data class Vocabulary(
    val vulnTypes: Map<VulnTypeId, VulnTypeDecl>,
    val sourceKinds: Map<SourceKindId, SourceKindDecl>,
) {
    /**
     * Interns [raw] to its [VulnTypeId], validating it is declared.
     *
     * @param raw Vulnerability type tag from a YAML document (case-insensitive).
     * @return The interned identity of the declared vulnerability type.
     * @throws VocabularyException If [raw] names no declared vulnerability type.
     */
    public fun requireVulnType(raw: String): VulnTypeId {
        val id = VulnTypeId(raw)
        if (id in vulnTypes) return id
        throw undeclared("vulnerability type", raw, vulnTypes.values.map { "${it.id.id} (${it.description})" })
    }

    /**
     * Interns [raw] to its [SourceKindId], validating it is declared.
     *
     * @param raw Source kind tag from a YAML document (case-insensitive).
     * @return The interned identity of the declared source kind.
     * @throws VocabularyException If [raw] names no declared source kind.
     */
    public fun requireSourceKind(raw: String): SourceKindId {
        val id = SourceKindId(raw)
        if (id in sourceKinds) return id
        throw undeclared("source kind", raw, sourceKinds.values.map { "${it.id.id} (${it.description})" })
    }

    /**
     * Unions this vocabulary with [other]. A name declared in both is one
     * declaration when the declarations are equal; a differing description or
     * tokenizer is a conflicting redeclaration, never a silent override.
     *
     * @param other The vocabulary a later document set contributes.
     * @return The union, this vocabulary's entries first.
     * @throws VocabularyException If a name is declared in both with different declarations.
     */
    public fun merge(other: Vocabulary): Vocabulary =
        Vocabulary(
            vulnTypes = vulnTypes.merged(other.vulnTypes, "vulnerability type"),
            sourceKinds = sourceKinds.merged(other.sourceKinds, "source kind"),
        )

    /**
     * Checks that every vulnerability type and source kind [entry] references is declared.
     *
     * @param entry A model entry; its sources, sinks, and sanitizers are read.
     * @throws VocabularyException If a referenced name is undeclared.
     */
    public fun verify(entry: ModelEntry) {
        val body = entry.body
        body.sources?.forEach { source -> source.sourceKinds.forEach(::requireDeclared) }
        body.sinks?.forEach { sink -> requireDeclared(sink.vulnType) }
        body.sanitizers?.forEach { sanitizer -> sanitizer.vulnTypes.forEach(::requireDeclared) }
    }

    private fun requireDeclared(id: VulnTypeId) {
        requireVulnType(id.id)
    }

    private fun requireDeclared(id: SourceKindId) {
        requireSourceKind(id.id)
    }

    private fun <K, V> Map<K, V>.merged(
        other: Map<K, V>,
        kind: String,
    ): Map<K, V> {
        val union = LinkedHashMap(this)
        for ((id, decl) in other) {
            val existing = union.put(id, decl) ?: continue
            if (existing != decl) {
                throw VocabularyException("Conflicting $kind redeclaration '$id': $existing vs $decl")
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
