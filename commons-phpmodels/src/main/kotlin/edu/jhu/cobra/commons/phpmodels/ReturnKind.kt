package edu.jhu.cobra.commons.phpmodels

/**
 * Closed classification of the result of a call with no interpretable body.
 * Mapping into a consumer's value lattice is the consumer's extension; this
 * library owns no lattice type.
 */
public enum class ReturnKind {
    STR,
    NUM,
    BOOL,
    ANY,
    ;

    /** The least classification covering both: itself when equal, [ANY] otherwise. */
    public fun join(other: ReturnKind): ReturnKind = if (this == other) this else ANY
}
