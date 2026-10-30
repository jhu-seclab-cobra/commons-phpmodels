package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import edu.jhu.cobra.commons.value.IPrimitiveVal
import edu.jhu.cobra.commons.value.Unsure

/**
 * One decoded `when:` condition of a model entry: the expected scalar per
 * argument position, or the wildcard `_` where the position does not
 * matter. Equality covers the whole list — two entries carrying equal
 * patterns state alternatives of the same key.
 *
 * @property expected Position `i` holds the scalar argument `i` must equal,
 *   or null for the wildcard.
 * @throws IllegalArgumentException If the pattern is empty, holds only
 *   wildcards, or holds an [Unsure].
 */
public data class ArgPattern(
    val expected: List<IPrimitiveVal?>,
) {
    init {
        require(expected.isNotEmpty()) { "Condition pattern is empty" }
        require(expected.any { it != null }) { "Condition pattern holds only wildcards" }
        require(expected.none { it is Unsure }) { "Condition pattern holds an unsure value" }
    }

    /** The non-wildcard positions, for the entry-level arity check. */
    public val positions: List<Int>
        get() = expected.indices.filter { expected[it] != null }

    /**
     * Matches this condition against the arguments of one call.
     *
     * @param actual The call's arguments by position; null or [Unsure] marks an unknown argument.
     * @return `false` when the pattern is longer than [actual] or a known argument differs from its
     *   scalar; `true` when every non-wildcard position holds an equal known argument; `null` when the
     *   outcome depends on an unknown argument.
     */
    public fun matches(actual: List<IPrimitiveVal?>): Boolean? {
        if (expected.size > actual.size) return false
        return combine(positions.map { outcomeAt(actual[it], expected[it]) })
    }

    /**
     * Matches this condition against a call whose arguments each hold a set of alternatives. A constrained position
     * holds when every alternative holds, fails when all fail, and is undecidable otherwise; the positions stay
     * independent, so no combination of alternatives across positions is formed and the work is the sum of the
     * alternatives over the constrained positions. An omitted position, including one past the end of [actual], is
     * read as the default the callable declares there, as PHP binds it.
     *
     * @param actual The call's alternatives by position; a null entry marks an omitted argument, an alternative that
     *   is null or [Unsure] an unknown one. A position this pattern leaves as a wildcard is not read.
     * @param defaults The scalar default the callable declares at each position, null where it declares none.
     * @return `false` when a constrained position is omitted and has no declared default, or every alternative of a
     *   constrained position differs from its scalar; `true` when every alternative of every constrained position
     *   equals its scalar; `null` otherwise.
     */
    public fun matchesAlternatives(
        actual: List<Set<IPrimitiveVal?>?>,
        defaults: List<IPrimitiveVal?> = emptyList(),
    ): Boolean? =
        combine(
            positions.map { position ->
                positionOutcome(actual.getOrNull(position), defaults.getOrNull(position), expected[position])
            },
        )

    // An omitted position answers as its declared default, or fails without one; a supplied one answers over its
    // alternatives.
    private fun positionOutcome(
        alternatives: Set<IPrimitiveVal?>?,
        default: IPrimitiveVal?,
        scalar: IPrimitiveVal?,
    ): Boolean? =
        when {
            alternatives != null -> alternativesOutcome(alternatives, scalar)
            default != null -> default == scalar
            else -> false
        }

    // A failing position decides over an unknown one.
    private fun combine(outcomes: List<Boolean?>): Boolean? =
        when {
            outcomes.any { it == false } -> false
            outcomes.any { it == null } -> null
            else -> true
        }

    // One position's outcome over its alternatives, each read once: all true → true, all false → false, else (an
    // unknown or mixed set, or none) undecidable.
    private fun alternativesOutcome(
        alternatives: Set<IPrimitiveVal?>,
        scalar: IPrimitiveVal?,
    ): Boolean? {
        var holds = false
        var fails = false
        for (alternative in alternatives) {
            when (outcomeAt(alternative, scalar)) {
                true -> holds = true
                false -> fails = true
                null -> return null
            }
        }
        return if (holds == fails) null else holds
    }

    // One position's outcome: unknown argument → undecidable, else equality.
    private fun outcomeAt(
        argument: IPrimitiveVal?,
        scalar: IPrimitiveVal?,
    ): Boolean? = if (argument == null || argument is Unsure) null else argument == scalar

    public companion object {
        private const val WILDCARD = "_"

        // The sequence arrives as raw tree nodes so each element narrows by
        // its YAML shape (impl.md); a mapping or sequence element is rejected.
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        internal fun decode(elements: List<JsonNode>): ArgPattern = ArgPattern(elements.map(::scalarOf))

        private fun scalarOf(node: JsonNode): IPrimitiveVal? =
            if (node.isTextual && node.textValue() == WILDCARD) null else node.toPhpScalar("Condition element")
    }
}
