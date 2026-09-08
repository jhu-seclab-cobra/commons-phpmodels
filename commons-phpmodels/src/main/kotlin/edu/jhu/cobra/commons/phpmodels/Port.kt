package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * One explicitly named location in a call. Decoded from the string spellings
 * `argument(n)`, `this`, and `return` — no bare integer and no sentinel value
 * exists anywhere in the port vocabulary.
 */
public sealed interface Port {
    /**
     * The ports a call supplies values through: the arguments and the
     * receiver. A from-side field typed as this interface is input-typed
     * instead of runtime-checked.
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

    public companion object {
        // The three port spellings are fixed by the model format; each port's
        // string form and the parser read the same constant.
        private const val RETURN_SPELLING = "return"
        private const val RECEIVER_SPELLING = "this"
        private const val ARGUMENT_SPELLING = "argument"
        private val ARGUMENT_PATTERN = Regex("""$ARGUMENT_SPELLING\((\d+)\)""")

        /**
         * The port the spelling [raw] names.
         *
         * @throws IllegalArgumentException If [raw] is none of `return`, `this`,
         *   and `argument(n)` with n >= 0.
         */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): Port =
            when (raw) {
                RETURN_SPELLING -> Return
                RECEIVER_SPELLING -> Receiver
                else -> parseArgumentSpelling(raw)
            }

        private fun parseArgumentSpelling(raw: String): Argument {
            val match =
                ARGUMENT_PATTERN.matchEntire(raw)
                    ?: throw IllegalArgumentException("Port must be 'return', 'this', or 'argument(n)', got '$raw'")
            val position =
                match.groupValues[1].toIntOrNull()
                    ?: throw IllegalArgumentException("Argument position out of range: '$raw'")
            return Argument(position)
        }
    }
}
