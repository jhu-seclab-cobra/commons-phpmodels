package edu.jhu.cobra.commons.phpmodels

/**
 * One translation table over both vocabulary axes, applied to a document
 * set whose names are not the consumer's. A `null` target discards the
 * source name: the element naming it is dropped rather than renamed.
 * Translation touches only vulnerability type and source kind names;
 * subject, ports, conditions, signatures, and value units pass through
 * unchanged.
 *
 * Whether each target is declared is checked at load against the
 * accumulated vocabulary ([DocumentSetLoader]); the table alone does not
 * know the consumer's vocabulary.
 *
 * @property vulnTypes Mapped-set vulnerability type to the consumer's, or null to discard.
 * @property sourceKinds Mapped-set source kind to the consumer's, or null to discard.
 */
public data class CategoryMapping(
    val vulnTypes: Map<VulnTypeId, VulnTypeId?>,
    val sourceKinds: Map<SourceKindId, SourceKindId?>,
) {
    /**
     * Translates one vulnerability type name.
     *
     * @param source A vulnerability type name of the mapped set.
     * @return The consumer's name, or null when [source] is discarded.
     * @throws VocabularyException If [source] is unlisted.
     */
    public fun vulnType(source: VulnTypeId): VulnTypeId? {
        if (source !in vulnTypes) throw unmapped("vulnerability type", source.id)
        return vulnTypes[source]
    }

    /**
     * Translates one source kind name.
     *
     * @param source A source kind name of the mapped set.
     * @return The consumer's name, or null when [source] is discarded.
     * @throws VocabularyException If [source] is unlisted.
     */
    public fun sourceKind(source: SourceKindId): SourceKindId? {
        if (source !in sourceKinds) throw unmapped("source kind", source.id)
        return sourceKinds[source]
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
     * @return Rows in the consumer's names; a row whose source kind is discarded
     *   or whose enabled set empties is dropped.
     * @throws VocabularyException If a referenced name is unlisted.
     */
    public fun apply(rows: List<PolicyRow>): List<PolicyRow> =
        rows.mapNotNull { row ->
            val sourceKind = sourceKind(row.sourceKind) ?: return@mapNotNull null
            val enables = row.enables.mapNotNull(::vulnType).toSet()
            if (enables.isEmpty()) null else PolicyRow(sourceKind, enables)
        }

    private fun apply(body: ModelBody): ModelBody =
        ModelBody(
            returns = body.returns,
            propagation = body.propagation,
            characters = body.characters,
            sources = body.sources?.mapNotNull(::apply)?.ifEmpty { null },
            sinks = body.sinks?.mapNotNull(::apply)?.ifEmpty { null },
            sanitizers = body.sanitizers?.mapNotNull(::apply)?.ifEmpty { null },
            raises = body.raises,
        )

    private fun apply(source: SourceDecl): SourceDecl? {
        val sourceKinds = source.sourceKinds.mapNotNull(::sourceKind).toSet()
        return if (sourceKinds.isEmpty()) null else source.copy(sourceKinds = sourceKinds)
    }

    private fun apply(sink: SinkDecl): SinkDecl? = vulnType(sink.vulnType)?.let { sink.copy(vulnType = it) }

    // A check element bounding its argument by a class or a type set outlives its vulnerability types.
    private fun apply(sanitizer: SanitizerDecl): SanitizerDecl? {
        val vulnTypes = sanitizer.vulnTypes.mapNotNull(::vulnType).toSet()
        val emptied = vulnTypes.isEmpty() && sanitizer.check?.statesBound != true
        return if (emptied) null else sanitizer.copy(vulnTypes = vulnTypes)
    }

    private fun unmapped(
        kind: String,
        name: String,
    ): VocabularyException = VocabularyException("Unmapped $kind: '$name' is neither mapped nor discarded")
}
