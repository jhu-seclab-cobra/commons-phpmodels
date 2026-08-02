package edu.jhu.cobra.commons.phpmodels

/**
 * One policy statement: an origin color and the danger categories it enables.
 * A value carrying [origin] that reaches a sink of a category in [enables] is
 * a vulnerability.
 *
 * @property origin Interned origin color the row governs.
 * @property enables Danger categories this origin makes dangerous.
 */
public data class PolicyRow(
    val origin: ProvenanceId,
    val enables: Set<VulnClassId>,
)

/**
 * The global origin→categories matrix. Answers whether a value of a given
 * origin color reaching a sink of a given danger category is a vulnerability.
 * Rows sharing an origin accumulate (union of enabled categories).
 *
 * @param rows Policy statements, validated against the vocabulary at load time.
 */
public class TaintPolicy(
    rows: List<PolicyRow>,
) {
    private val matrix: Map<ProvenanceId, Set<VulnClassId>> =
        rows.groupingBy { it.origin }.fold(emptySet()) { acc, row -> acc + row.enables }

    /**
     * Reports whether [color] reaching a sink of [category] is a vulnerability.
     *
     * @param color Origin color arriving at the sink.
     * @param category Danger category of the sink.
     * @return True when the matrix enables [category] for [color].
     */
    public fun isDangerous(
        color: ProvenanceId,
        category: VulnClassId,
    ): Boolean = matrix[color]?.contains(category) == true
}
