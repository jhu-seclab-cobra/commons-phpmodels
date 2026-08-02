package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * One explicitly named location in a call. Decoded from the string spellings
 * `argument(n)` and `return` — no bare integer and no sentinel value exists
 * anywhere in the port vocabulary.
 */
public sealed interface Port {
    /**
     * The argument at zero-based [position].
     *
     * @throws IllegalArgumentException If [position] is negative.
     */
    public data class Argument(
        val position: Int,
    ) : Port {
        init {
            require(position >= 0) { "Argument position must be non-negative: $position" }
        }

        override fun toString(): String = "argument($position)"

        public companion object {
            // A field typed as this subtype does not consult the supertype's
            // creator (impl.md), so the narrowing creator lives here.
            @JvmStatic
            @JsonCreator
            public fun parseArgument(raw: String): Argument =
                requireNotNull(parse(raw) as? Argument) { "Expected an argument port, got '$raw'" }
        }
    }

    /** The call result. */
    public object Return : Port {
        override fun toString(): String = "return"
    }

    public companion object {
        private val ARGUMENT_SPELLING = Regex("""argument\((\d+)\)""")

        /**
         * The port the spelling [raw] names.
         *
         * @throws IllegalArgumentException If [raw] is neither `return` nor `argument(n)` with n >= 0.
         */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): Port {
            if (raw == "return") return Return
            val match =
                ARGUMENT_SPELLING.matchEntire(raw)
                    ?: throw IllegalArgumentException("Port must be 'return' or 'argument(n)', got '$raw'")
            return Argument(match.groupValues[1].toInt())
        }
    }
}
