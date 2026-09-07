package edu.jhu.cobra.commons.phpmodels

/**
 * Closed kind of checking a document set's entries received. Decoded from
 * the lowercase scalars `generated` and `manual`; any other spelling fails
 * the load.
 */
public enum class Verification {
    /** Emitted by a program from an upstream source; never reviewed entry by entry. */
    GENERATED,

    /** Written or reviewed by hand. */
    MANUAL,
}

/**
 * One document set's declared provenance: where its entries come from. Stated
 * once per set in `provenance.yaml` and attached to the loaded [DocumentSet];
 * it applies to every entry the set lists. Distinct from the origin color a
 * source gives a tainted value ([ProvenanceId]).
 *
 * @property producer Non-blank identifier of the process or party that emitted the set; documentary, never ranked.
 * @property verification How the set's entries were checked; the input to a consumer's [Precedence].
 */
public data class SetProvenance(
    val producer: String,
    val verification: Verification,
) {
    init {
        require(producer.isNotBlank()) { "Set provenance producer must not be blank" }
    }
}

/**
 * A consumer's total order over [Verification] kinds, highest first. When two
 * mounted sets state the same subject and unit, the consumer's fold keeps the
 * entry whose set ranks higher here and falls back to its own mount order on
 * a tie.
 *
 * @property order Every kind exactly once, highest first.
 */
public data class Precedence(
    val order: List<Verification>,
) : Comparator<Verification> {
    init {
        require(order.toSet() == Verification.entries.toSet() && order.size == Verification.entries.size) {
            "Precedence must rank every verification kind exactly once, was $order"
        }
    }

    /** The position of [kind] in [order]; `0` is the highest rank. */
    public fun rank(kind: Verification): Int = order.indexOf(kind)

    /** Positive when [a] ranks higher than [b], zero when equal, negative otherwise. */
    override fun compare(
        a: Verification,
        b: Verification,
    ): Int = rank(b).compareTo(rank(a))

    public companion object {
        /** Manual above generated. */
        public val DEFAULT: Precedence = Precedence(listOf(Verification.MANUAL, Verification.GENERATED))
    }
}
