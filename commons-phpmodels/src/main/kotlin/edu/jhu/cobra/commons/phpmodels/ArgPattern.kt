package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import edu.jhu.cobra.commons.value.BoolVal
import edu.jhu.cobra.commons.value.FloatVal
import edu.jhu.cobra.commons.value.IPrimitiveVal
import edu.jhu.cobra.commons.value.IntVal
import edu.jhu.cobra.commons.value.NullVal
import edu.jhu.cobra.commons.value.StrVal
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
        val outcomes = positions.map { outcomeAt(actual[it], expected[it]) }
        return when {
            outcomes.any { it == false } -> false
            outcomes.any { it == null } -> null
            else -> true
        }
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
            when {
                node.isTextual && node.textValue() == WILDCARD -> null
                node.isBoolean -> BoolVal(node.booleanValue())
                node.isIntegralNumber && node.canConvertToLong() -> IntVal(node.longValue())
                node.isNumber -> FloatVal(node.doubleValue())
                node.isNull -> NullVal
                node.isTextual -> StrVal(node.textValue())
                else -> throw IllegalArgumentException("Condition element must be a scalar or '_', got: $node")
            }
    }
}
