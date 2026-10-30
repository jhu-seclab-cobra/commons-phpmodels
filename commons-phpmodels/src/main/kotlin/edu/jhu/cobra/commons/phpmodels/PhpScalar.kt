package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.databind.JsonNode
import edu.jhu.cobra.commons.value.BoolVal
import edu.jhu.cobra.commons.value.FloatVal
import edu.jhu.cobra.commons.value.IPrimitiveVal
import edu.jhu.cobra.commons.value.IntVal
import edu.jhu.cobra.commons.value.NullVal
import edu.jhu.cobra.commons.value.StrVal

/**
 * Reads one YAML scalar as the PHP scalar it spells: boolean, integer within
 * `Long`, other number as float, null, or text. The one reading shared by a
 * condition element and a check's accepting value (impl.md).
 *
 * @param role Names the declaring position in the failure message.
 * @throws IllegalArgumentException If the node is a mapping or a sequence.
 */
internal fun JsonNode.toPhpScalar(role: String): IPrimitiveVal =
    when {
        isBoolean -> BoolVal(booleanValue())
        isIntegralNumber && canConvertToLong() -> IntVal(longValue())
        isNumber -> FloatVal(doubleValue())
        isNull -> NullVal
        isTextual -> StrVal(textValue())
        else -> throw IllegalArgumentException("$role must be a scalar, got: $this")
    }

/**
 * Reads the PHP scalar literal a stub spells as a parameter default: `true`, `false` or `null` in any case, a decimal
 * integer within `Long`, a decimal float, or a quoted string without an escape.
 *
 * @param spelled The default as the stub spells it.
 * @return The scalar, or null when the spelling is none of those forms.
 */
internal fun parsePhpScalarLiteral(spelled: String): IPrimitiveVal? {
    val quoted = spelled.length >= 2 && spelled.first() in "'\"" && spelled.last() == spelled.first()
    return when {
        spelled.lowercase() == "true" -> BoolVal(true)
        spelled.lowercase() == "false" -> BoolVal(false)
        spelled.lowercase() == "null" -> NullVal
        quoted && '\\' !in spelled -> StrVal(spelled.substring(1, spelled.length - 1))
        INTEGER.matches(spelled) -> spelled.toLongOrNull()?.let(::IntVal)
        DECIMAL.matches(spelled) -> FloatVal(spelled.toDouble())
        else -> null
    }
}

private val INTEGER = Regex("-?\\d+")
private val DECIMAL = Regex("-?\\d+\\.\\d+")
