package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * One explicitly named location in a call. Decoded from the string spellings
 * `argument(n)`, `this`, `return`, `callback(argument(n)).parameter(m)` and
 * `callback(argument(n)).return` — no bare integer and no sentinel value
 * exists anywhere in the port vocabulary.
 */
public sealed interface Port {
    /**
     * The ports a call reads values from: the arguments, the receiver and the
     * results of the callables the call invokes. A from-side field typed as
     * this interface is input-typed instead of runtime-checked.
     */
    public sealed interface Input : Port {
        public companion object {
            // A field typed as this sub-interface does not consult the
            // supertype's creator (impl.md), so the narrowing creator lives
            // here.
            @JvmStatic
            @JsonCreator
            public fun parseInput(raw: String): Input =
                requireNotNull(parse(raw) as? Input) { "Expected an input port, got '$raw'" }
        }
    }

    /**
     * The argument at zero-based [position].
     *
     * @throws IllegalArgumentException If [position] is negative.
     */
    public data class Argument(
        val position: Int,
    ) : Input {
        init {
            require(position >= 0) { "Argument position must be non-negative: $position" }
        }

        override fun toString(): String = "$ARGUMENT_SPELLING($position)"

        public companion object {
            // A field typed as this subtype does not consult the supertype's
            // creator (impl.md), so the narrowing creator lives here.
            @JvmStatic
            @JsonCreator
            public fun parseArgument(raw: String): Argument =
                requireNotNull(parse(raw) as? Argument) { "Expected an argument port, got '$raw'" }
        }
    }

    /** The receiver of a call to a method, spelled `this`. */
    public object Receiver : Input {
        override fun toString(): String = RECEIVER_SPELLING
    }

    /** The call result. */
    public object Return : Port {
        override fun toString(): String = RETURN_SPELLING
    }

    /**
     * The value the callable passed at argument [callable] returns to the
     * builtin that calls it, spelled `callback(argument(n)).return`. It is an
     * input: the call result or a later callback parameter reads it.
     *
     * @throws IllegalArgumentException If [callable] is negative.
     */
    public data class CallbackReturn(
        val callable: Int,
    ) : Input {
        init {
            require(callable >= 0) { "Callback argument position must be non-negative: $callable" }
        }

        override fun toString(): String = "$CALLBACK_SPELLING($ARGUMENT_SPELLING($callable)).$RETURN_SPELLING"
    }

    /**
     * The parameter at zero-based [position] of the callable passed at
     * argument [callable], which the builtin fills when it calls the callable,
     * spelled `callback(argument(n)).parameter(m)`. Only a `to` accepts it.
     *
     * @throws IllegalArgumentException If [callable] or [position] is negative.
     */
    public data class CallbackParameter(
        val callable: Int,
        val position: Int,
    ) : Port {
        init {
            require(callable >= 0) { "Callback argument position must be non-negative: $callable" }
            require(position >= 0) { "Callback parameter position must be non-negative: $position" }
        }

        override fun toString(): String =
            "$CALLBACK_SPELLING($ARGUMENT_SPELLING($callable)).$PARAMETER_SPELLING($position)"
    }

    public companion object {
        // The port spellings are fixed by the model format; each port's
        // string form and the parser read the same constant.
        private const val RETURN_SPELLING = "return"
        private const val RECEIVER_SPELLING = "this"
        private const val ARGUMENT_SPELLING = "argument"
        private const val CALLBACK_SPELLING = "callback"
        private const val PARAMETER_SPELLING = "parameter"
        private val ARGUMENT_PATTERN = Regex("""$ARGUMENT_SPELLING\((\d+)\)""")
        private val CALLBACK_PARAMETER_PATTERN =
            Regex("""$CALLBACK_SPELLING\($ARGUMENT_SPELLING\((\d+)\)\)\.$PARAMETER_SPELLING\((\d+)\)""")
        private val CALLBACK_RETURN_PATTERN =
            Regex("""$CALLBACK_SPELLING\($ARGUMENT_SPELLING\((\d+)\)\)\.$RETURN_SPELLING""")

        /**
         * The port the spelling [raw] names.
         *
         * @throws IllegalArgumentException If [raw] is none of `return`, `this`,
         *   `argument(n)`, `callback(argument(n)).parameter(m)` and
         *   `callback(argument(n)).return` with n, m >= 0.
         */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): Port =
            when (raw) {
                RETURN_SPELLING -> Return
                RECEIVER_SPELLING -> Receiver
                else -> parseIndexed(raw)
            }

        private fun parseIndexed(raw: String): Port {
            ARGUMENT_PATTERN.matchEntire(raw)?.let { return Argument(position(raw, it, 1)) }
            CALLBACK_RETURN_PATTERN.matchEntire(raw)?.let { return CallbackReturn(position(raw, it, 1)) }
            CALLBACK_PARAMETER_PATTERN.matchEntire(raw)?.let {
                return CallbackParameter(position(raw, it, 1), position(raw, it, 2))
            }
            throw IllegalArgumentException(
                "Port must be 'return', 'this', 'argument(n)', 'callback(argument(n)).parameter(m)' " +
                    "or 'callback(argument(n)).return', got '$raw'",
            )
        }

        private fun position(
            raw: String,
            match: MatchResult,
            group: Int,
        ): Int =
            match.groupValues[group].toIntOrNull()
                ?: throw IllegalArgumentException("Port position out of range: '$raw'")
    }
}
