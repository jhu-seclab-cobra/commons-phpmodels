package edu.jhu.cobra.commons.phpmodels

/**
 * One translation table over both vocabulary axes, applied to a document
 * set whose names are not the consumer's. A `null` target discards the
 * source name: the element naming it is dropped rather than renamed.
 * Translation touches only category and color names; subject, ports,
 * guards, signatures, and value semantics pass through unchanged.
 *
 * Whether each target is declared is checked at load against the
 * accumulated vocabulary ([DocumentSetLoader]); the table alone does not
 * know the consumer's vocabulary.
 *
 * @property categories Source category to target category, or null to discard.
 * @property provenances Source color to target color, or null to discard.
 */
public data class CategoryMapping(
    val categories: Map<VulnClassId, VulnClassId?>,
    val provenances: Map<ProvenanceId, ProvenanceId?>,
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
    public fun provenance(source: ProvenanceId): ProvenanceId? {
        if (source !in provenances) throw unmapped("provenance", source.id)
        return provenances[source]
    }

    /**
     * Translates the taint sections of [entry].
     *
     * @param entry A flat model or generator in the mapped set's names.
     * @return The entry in the consumer's names, or null when translation
     *   removes every section of an entry that carries no signature.
     * @throws VocabularyException If a referenced name is unlisted.
     */
    public fun apply(entry: ModelEntry): ModelEntry? =
        when (entry) {
            is SubjectModel -> {
                val body = apply(entry.body)
                if (body.isEmpty && entry.signature == null) null else entry.copy(body = body)
            }
            is ModelGenerator -> {
                val model = apply(entry.model)
                if (model.isEmpty) null else entry.copy(model = model)
            }
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
            val origin = provenance(row.origin) ?: return@mapNotNull null
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
        val colors = source.provenance.mapNotNull(::provenance).toSet()
        return if (colors.isEmpty()) null else source.copy(provenance = colors)
    }

    private fun apply(sink: SinkPoint): SinkPoint? = category(sink.category)?.let { sink.copy(category = it) }

    private fun apply(sanitizer: SanitizerDecl): SanitizerDecl? {
        val categories = sanitizer.categories.mapNotNull(::category).toSet()
        return if (categories.isEmpty()) null else sanitizer.copy(categories = categories)
    }

    private fun unmapped(
        kind: String,
        name: String,
    ): VocabularyException = VocabularyException("Unmapped $kind: '$name' is neither mapped nor discarded")
}
