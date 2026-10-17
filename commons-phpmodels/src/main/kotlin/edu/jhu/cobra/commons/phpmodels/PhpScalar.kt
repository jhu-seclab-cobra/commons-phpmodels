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
