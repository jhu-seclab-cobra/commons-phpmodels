package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * One state of a [TokenizerLanguage]: a declared state name, or for a state that tracks a word set the
 * declared name, a colon and the lowercased prefix spelled since entry.
 *
 * @property name The state name; two states are the same state exactly when their names are equal.
 */
@JvmInline
public value class TokenizerState internal constructor(
    public val name: String,
)

/**
 * The deterministic lexical language of the text a value is written into, held once on the vocabulary entry
 * of a vulnerability type. A text is read as bytes; [run] gives the state it ends in, and [targets] are the
 * states at which a tainted value lands in the position the vulnerability type attacks.
 *
 * Declared as ordered rules per state: the first rule whose byte class holds the byte decides, an unmatched
 * byte takes the state's `otherwise` or stays. A state may track a word set: it then spells the bytes read
 * since entry, case-insensitively, and a rule marked `ifWord` applies only when the spelling is a whole word.
 * Two languages are equal when their declarations are equal.
 *
 * @property start The state a text begins in.
 * @property targets The states at which a tainted value lands in the attacked position.
 * @property states Every state, with a tracked state expanded to one state per word prefix; an unknown text
 *   may end in any of them.
 */
public class TokenizerLanguage
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    internal constructor(
        private val declaration: TokenizerDecl,
    ) {
        private val automaton = TokenizerAutomaton(declaration)

        public val start: TokenizerState = TokenizerState(declaration.start)

        public val targets: Set<TokenizerState> = declaration.targets.map(::TokenizerState).toSet()

        public val states: Set<TokenizerState> get() = automaton.states

        /**
         * The state reached from [state] on the byte [byte].
         *
         * @throws IllegalArgumentException If [byte] lies outside 0..255 or [state] is not a state of this language.
         */
        public fun step(
            state: TokenizerState,
            byte: Int,
        ): TokenizerState = automaton.step(state, byte)

        /**
         * The state reached from [state] after reading [text], one byte per character.
         *
         * @throws IllegalArgumentException If a character lies above U+00FF or [state] is not a state of this language.
         */
        public fun run(
            state: TokenizerState,
            text: String,
        ): TokenizerState {
            require(text.all { it.code < BYTE_VALUES }) { "Text holds a character above U+00FF; not a byte string" }
            return text.fold(state) { current, char -> step(current, char.code) }
        }

        override fun equals(other: Any?): Boolean = other is TokenizerLanguage && declaration == other.declaration

        override fun hashCode(): Int = declaration.hashCode()

        override fun toString(): String = "TokenizerLanguage($declaration)"
    }
