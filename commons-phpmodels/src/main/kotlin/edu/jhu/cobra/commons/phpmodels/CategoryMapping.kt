package edu.jhu.cobra.commons.phpmodels

/**
 * One translation table over both vocabulary axes, applied to a document
 * set whose names are not the consumer's. A `null` target discards the
 * source name: the element naming it is dropped rather than renamed.
 * Translation touches only category and color names; subject, ports,
 * conditions, signatures, and value semantics pass through unchanged.
 *
 * Whether each target is declared is checked at load against the
 * accumulated vocabulary ([DocumentSetLoader]); the table alone does not
 * know the consumer's vocabulary.
 *
 * @property categories Source category to target category, or null to discard.
 * @property origins Source color to target color, or null to discard.
 */
public data class CategoryMapping(
    val categories: Map<VulnClassId, VulnClassId?>,
    val origins: Map<OriginId, OriginId?>,
) {
    /**
     * Translates one category name.
     *
     * @param source A category name of the mapped set.
     * @return The consumer's name, or null when [source] is discarded.
     * @throws VocabularyException If [source] is unlisted.
     */
    public fun category(source: VulnClassId): VulnClassId? {
        if (source !in categories) throw unmapped("category", source.id)
        return categories[source]
    }

    /**
     * Translates one origin-color name.
     *
     * @param source A color name of the mapped set.
     * @return The consumer's name, or null when [source] is discarded.
     * @throws VocabularyException If [source] is unlisted.
     */
    public fun origin(source: OriginId): OriginId? {
        if (source !in origins) throw unmapped("origin", source.id)
        return origins[source]
    }

    /**
     * Translates the taint sections of [entry].
     *
     * @param entry A model entry in the mapped set's names.
     * @return The entry in the consumer's names, or null when translation
     *   removes every section of an entry that carries no signature.
     * @throws VocabularyException If a referenced name is unlisted.
     */
    public fun apply(entry: ModelEntry): ModelEntry? {
        val body = apply(entry.body)
        return if (body.isEmpty && entry.signature == null) null else entry.copy(body = body)
    }

    /**
     * Translates policy rows.
     *
     * @param rows Rows in the mapped set's names.
     * @return Rows in the consumer's names; a row whose origin is discarded
     *   or whose enabled set empties is dropped.
     * @throws VocabularyException If a referenced name is unlisted.
     */
    public fun apply(rows: List<PolicyRow>): List<PolicyRow> =
        rows.mapNotNull { row ->
            val origin = origin(row.origin) ?: return@mapNotNull null
            val enables = row.enables.mapNotNull(::category).toSet()
            if (enables.isEmpty()) null else PolicyRow(origin, enables)
        }

    private fun apply(body: ModelBody): ModelBody =
        ModelBody(
            returns = body.returns,
            propagation = body.propagation,
            sources = body.sources?.mapNotNull(::apply)?.ifEmpty { null },
            sinks = body.sinks?.mapNotNull(::apply)?.ifEmpty { null },
            sanitizers = body.sanitizers?.mapNotNull(::apply)?.ifEmpty { null },
        )

    private fun apply(source: SourceDecl): SourceDecl? {
        val colors = source.origin.mapNotNull(::origin).toSet()
        return if (colors.isEmpty()) null else source.copy(origin = colors)
    }

    private fun apply(sink: SinkDecl): SinkDecl? = category(sink.vulnClass)?.let { sink.copy(vulnClass = it) }

    private fun apply(sanitizer: SanitizerDecl): SanitizerDecl? {
        val categories = sanitizer.categories.mapNotNull(::category).toSet()
        return if (categories.isEmpty()) null else sanitizer.copy(categories = categories)
    }

    private fun unmapped(
        kind: String,
        name: String,
    ): VocabularyException = VocabularyException("Unmapped $kind: '$name' is neither mapped nor discarded")
}
