package edu.jhu.cobra.commons.phpmodels

// A byte is a value in 0..255; a text is read one Latin-1 character per byte.
internal const val BYTE_VALUES = 256

// ASCII letters fold to lowercase; every other byte is its own spelling.
private const val ASCII_CASE_OFFSET = 'a'.code - 'A'.code

/**
 * One position of the automaton: a declared state, and for a tracked state the word prefix spelled since entry.
 *
 * @property state The declared state name.
 * @property spelled The prefix of a tracked word spelled since entry, or null when the state is untracked or
 *   the spelling left every word of its set.
 * @property complete True when [spelled] is a whole word of the set.
 */
private data class Position(
    val state: String,
    val spelled: String? = null,
    val complete: Boolean = false,
) {
    val name: TokenizerState = TokenizerState(if (spelled == null) state else "$state:$spelled")
}

/**
 * The deterministic transition table a [TokenizerDecl] compiles to. A tracked state expands into one position
 * per prefix of its words, so the table is a plain DFA and a set of states can be pushed through a segment.
 */
internal class TokenizerAutomaton(
    private val declaration: TokenizerDecl,
) {
    // Per word set: every non-empty prefix of its lowercased words, mapped to whether it is a whole word.
    private val prefixes: Map<String, Map<String, Boolean>> =
        declaration.words.mapValues { (_, words) ->
            val folded = words.map { word -> word.map(::foldCase).joinToString("") }
            folded.flatMap { word -> (1..word.length).map(word::take) }.associateWith { it in folded }
        }

    private val table: Map<TokenizerState, List<TokenizerState>>

    /** Every position name, including each tracked prefix. */
    val states: Set<TokenizerState>

    init {
        val positions = declaration.states.flatMap { (name, state) -> positionsOf(name, state) }
        states = positions.map { it.name }.toSet()
        table = positions.associate { position -> position.name to List(BYTE_VALUES) { next(position, it).name } }
    }

    /** The state reached from [state] on [byte], a value in 0..255. */
    fun step(
        state: TokenizerState,
        byte: Int,
    ): TokenizerState {
        require(byte in 0..<BYTE_VALUES) { "Not a byte value: $byte" }
        val row = requireNotNull(table[state]) { "Not a state of this tokenizer: '${state.name}'" }
        return row[byte]
    }

    private fun positionsOf(
        name: String,
        state: StateDecl,
    ): List<Position> {
        val spellings = state.track?.let { prefixes.getValue(it) }.orEmpty()
        return listOf(Position(name)) + spellings.map { (prefix, complete) -> Position(name, prefix, complete) }
    }

    private fun next(
        from: Position,
        byte: Int,
    ): Position {
        val state = declaration.states.getValue(from.state)
        val rule = state.rules.firstOrNull { it.chars.contains(byte) && (!it.ifWord || from.complete) }
        return enter(rule?.next ?: state.otherwise ?: from.state, from, foldCase(byte.toChar()))
    }

    // Entering a tracked state starts a new spelling with the entering byte; staying in it extends the old one.
    private fun enter(
        destination: String,
        from: Position,
        folded: Char,
    ): Position {
        val track = declaration.states.getValue(destination).track
        val spelled = if (destination == from.state) from.spelled?.plus(folded) else folded.toString()
        val complete = if (track == null || spelled == null) null else prefixes.getValue(track)[spelled]
        return complete?.let { Position(destination, spelled, it) } ?: Position(destination)
    }

    private fun foldCase(char: Char): Char = if (char in 'A'..'Z') char + ASCII_CASE_OFFSET else char
}
