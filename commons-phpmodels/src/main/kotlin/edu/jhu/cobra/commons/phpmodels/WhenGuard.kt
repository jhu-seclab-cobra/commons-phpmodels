package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * The compared scalar of a guard, closed over the three shapes a guard admits.
 * Owned by this library so the format depends on no external value-lattice
 * type; a consumer maps a [GuardValue] into its own value domain at its
 * boundary.
 */
public sealed interface GuardValue {
    /** A boolean compared value. */
    @JvmInline
    public value class BoolValue(
        public val value: Boolean,
    ) : GuardValue

    /** An integer compared value. */
    @JvmInline
    public value class IntValue(
        public val value: Long,
    ) : GuardValue

    /** A string compared value. */
    @JvmInline
    public value class StrValue(
        public val value: String,
    ) : GuardValue
}

/**
 * One decoded `when:` condition of a guarded model entry: the argument port
 * tested and the scalar it must equal. Equality is the only predicate, and the
 * (port, value) pair is the guard's identity — two entries carrying the same
 * guard address the same branch of their subject.
 *
 * @property port The argument port the guard tests.
 * @property value The scalar the argument must equal.
 */
public data class WhenGuard(
    val port: Port.Argument,
    val value: GuardValue,
) {
    public companion object {
        // The compared value is the one field where three scalar shapes share a
        // key, so it arrives as a tree node and narrows here; `is` is a Kotlin
        // keyword, so the config key binds through the rename (impl.md).
        @JvmStatic
        @JsonCreator
        internal fun decode(
            @JsonProperty("port") port: Port.Argument,
            @JsonProperty("is") value: JsonNode,
        ): WhenGuard = WhenGuard(port, scalarOf(value))

        private fun scalarOf(node: JsonNode): GuardValue =
            when {
                node.isBoolean -> GuardValue.BoolValue(node.booleanValue())
                node.isIntegralNumber -> GuardValue.IntValue(node.longValue())
                node.isTextual -> GuardValue.StrValue(node.textValue())
                else -> throw IllegalArgumentException(
                    "Guard 'is' must be a boolean, integer, or string scalar, got: $node",
                )
            }
    }
}
