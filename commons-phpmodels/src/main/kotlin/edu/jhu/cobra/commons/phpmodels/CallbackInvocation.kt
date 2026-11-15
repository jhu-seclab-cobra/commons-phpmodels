package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.Nulls

/** The callable operand and input supply of an invocation declared by a value unit. */
public data class CallbackInvocation(val callable: CallbackSelector, val supply: CallbackSupply)

/** A callable operand selected from the enclosing invocation's inputs. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(
    JsonSubTypes.Type(value = CallbackSelector.Argument::class, name = "argument"),
    JsonSubTypes.Type(value = CallbackSelector.Trailing::class, name = "trailing"),
    JsonSubTypes.Type(value = CallbackSelector.Elements::class, name = "elements"),
)
public sealed interface CallbackSelector {
    /** An input at a non-negative, zero-based position. */
    public data class Argument(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) val position: Int,
    ) : CallbackSelector {
        init {
            require(position >= 0) { "Callable position must be non-negative: $position" }
        }
    }
    /** An input counted backwards from the end, with distance 1 selecting the last. */
    public data class Trailing(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) val distance: Int,
    ) : CallbackSelector {
        init {
            require(distance > 0) { "Trailing distance must be positive: $distance" }
        }
    }

    /** Every callable held by the array input at [position]. */
    public data class Elements(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) val position: Int,
    ) : CallbackSelector {
        init {
            require(position >= 0) { "Callable array position must be non-negative: $position" }
        }
    }
}

/** The input values passed to a selected callable. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(
    JsonSubTypes.Type(value = CallbackSupply.ArrayElements::class, name = "array-elements"),
    JsonSubTypes.Type(value = CallbackSupply.JoinedInputs::class, name = "joined-inputs"),
    JsonSubTypes.Type(value = CallbackSupply.ArrayValue::class, name = "array-value"),
    JsonSubTypes.Type(value = CallbackSupply.Unpacked::class, name = "unpacked"),
)
public sealed interface CallbackSupply {
    /** One element from each array input in the tail starting at [start]. */
    public data class ArrayElements(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) val start: Int,
    ) : CallbackSupply {
        init {
            require(start >= 0) { "Array input start must be non-negative: $start" }
        }
    }

    /** The non-callable inputs, reachable values and receiver joined for each of [count] parameters. */
    public data class JoinedInputs(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) val count: Int,
    ) : CallbackSupply {
        init {
            require(count > 0) { "Parameter count must be positive: $count" }
        }
    }

    /** One array whose members depend on the input at [position]. */
    public data class ArrayValue(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) val position: Int,
    ) : CallbackSupply {
        init {
            require(position >= 0) { "Array value position must be non-negative: $position" }
        }
    }

    /** The array input at [position] unpacked into possible argument positions. */
    public data class Unpacked(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) val position: Int,
    ) : CallbackSupply {
        init {
            require(position >= 0) { "Unpacked input position must be non-negative: $position" }
        }
    }
}
