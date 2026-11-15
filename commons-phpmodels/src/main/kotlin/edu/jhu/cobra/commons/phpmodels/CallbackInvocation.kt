package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/** The callable operand and input supply of an invocation declared by a value unit. */
public data class CallbackInvocation(val callable: CallbackSelector, val supply: CallbackSupply)

/** A callable operand selected from the enclosing invocation's inputs. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(JsonSubTypes.Type(value = CallbackSelector.Argument::class, name = "argument"))
public sealed interface CallbackSelector {
    /** An input at a non-negative, zero-based position. */
    public data class Argument(@JsonProperty(required = true) val position: Int) : CallbackSelector {
        init {
            require(position >= 0) { "Callable position must be non-negative: $position" }
        }
    }
}

/** The input values passed to a selected callable. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(JsonSubTypes.Type(value = CallbackSupply.ArrayElements::class, name = "array-elements"))
public sealed interface CallbackSupply {
    /** One element from each array input in the tail starting at [start]. */
    public data class ArrayElements(@JsonProperty(required = true) val start: Int) : CallbackSupply {
        init {
            require(start >= 0) { "Array input start must be non-negative: $start" }
        }
    }
}
