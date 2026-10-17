package edu.jhu.cobra.commons.phpmodels

/**
 * One policy statement: a source kind and the vulnerability types it enables.
 * A value carrying [sourceKind] that reaches a sink of a vulnerability type in
 * [enables] is a vulnerability.
 *
 * @property sourceKind Interned source kind the row governs.
 * @property enables Vulnerability types this source kind makes dangerous.
 * @throws IllegalArgumentException If no vulnerability type is enabled.
 */
public data class PolicyRow(
    val sourceKind: SourceKindId,
    val enables: Set<VulnTypeId>,
) {
    init {
        require(enables.isNotEmpty()) { "Policy row for '${sourceKind.id}' enables no vulnerability type" }
    }
}

/**
 * The global source kind → vulnerability types matrix. Answers whether a value
 * of a given source kind reaching a sink of a given vulnerability type is a
 * vulnerability. Rows sharing a source kind accumulate (union of enabled
 * vulnerability types).
 *
 * @param rows Policy statements, validated against the vocabulary at load time.
 */
public class TaintPolicy(
    rows: List<PolicyRow>,
) {
    private val matrix: Map<SourceKindId, Set<VulnTypeId>> =
        rows.groupingBy { it.sourceKind }.fold(emptySet()) { acc, row -> acc + row.enables }

    /**
     * Reports whether [sourceKind] reaching a sink of [vulnType] is a vulnerability.
     *
     * @param sourceKind Source kind arriving at the sink.
     * @param vulnType Vulnerability type of the sink.
     * @return True when the matrix enables [vulnType] for [sourceKind].
     */
    public fun isDangerous(
        sourceKind: SourceKindId,
        vulnType: VulnTypeId,
    ): Boolean = matrix[sourceKind]?.contains(vulnType) == true
}
