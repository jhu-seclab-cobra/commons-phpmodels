package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import edu.jhu.cobra.commons.value.IPrimitiveVal
import edu.jhu.cobra.commons.value.Unsure

/**
 * One element of a sanitizers section (agent decision CK-1): the danger
 * categories neutralized in the call result, or, with a [check], the
 * statement a call returning the accepting value makes about one argument.
 *
 * @property categories Danger categories neutralized: in the call result
 *   without a check, in the accepted argument with one.
 * @property check The checked argument and its accepting value, or null for
 *   a result element.
 * @throws IllegalArgumentException If a result element declares no category,
 *   or a check element states nothing about the accepted argument.
 */
public data class SanitizerDecl(
    val categories: Set<VulnTypeId> = emptySet(),
    val check: ArgumentCheck? = null,
) {
    init {
        require(categories.isNotEmpty() || check?.statesBound == true) {
            "Sanitizers element declares no category, and no check 'within' class or 'types' set"
        }
    }
}

/**
 * The check of one sanitizers element: a call returning [accepts] accepted
 * the argument at [argument].
 *
 * @property argument The checked argument port.
 * @property accepts The result value that accepts the argument.
 * @property within The class every byte of the accepted argument belongs to,
 *   or null when no class bounds it.
 * @property types The type set the call accepts exactly: the call returns
 *   [accepts] when and only when the argument's type is in the set; null when
 *   the check states no type.
 * @throws IllegalArgumentException If [accepts] is an [Unsure], or [types] is
 *   `void`.
 */
public data class ArgumentCheck(
    val argument: Port.Argument,
    val accepts: IPrimitiveVal,
    val within: CharacterClass? = null,
    val types: DeclaredType? = null,
) {
    init {
        require(accepts !is Unsure) { "Check accepting value is an unsure value" }
        require(types?.isVoid != true) { "Check type set is 'void'" }
    }

    /** True when the check bounds the accepted argument by a class or a type set. */
    internal val statesBound: Boolean
        get() = within != null || types != null

    internal companion object {
        // The accepting value arrives as a raw tree node and narrows by its
        // YAML shape, as a condition element does (impl.md).
        @JvmStatic
        @JsonCreator
        fun decode(
            @JsonProperty("argument") argument: Port.Argument,
            @JsonProperty("accepts") accepts: JsonNode,
            @JsonProperty("within") within: CharacterClass?,
            @JsonProperty("types") types: String?,
        ): ArgumentCheck =
            ArgumentCheck(argument, accepts.toPhpScalar("Check accepting value"), within, types?.let(::DeclaredType))
    }
}
