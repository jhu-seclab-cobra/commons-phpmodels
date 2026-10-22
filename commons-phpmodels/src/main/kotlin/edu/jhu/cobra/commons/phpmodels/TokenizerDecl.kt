package edu.jhu.cobra.commons.phpmodels

// A declared state, word-set and track name: lowercase words joined by hyphens. The colon is reserved
// for the state names of a tracked word prefix ("name:href"), so no declared name can collide with one.
private val NAME_PATTERN = Regex("[a-z][a-z0-9-]*")

/**
 * One rule of a state. The first rule whose class holds the byte, and whose word condition holds, decides the
 * next state.
 *
 * @property chars The byte class the rule matches.
 * @property next The state the rule moves to.
 * @property ifWord True when the rule applies only while the tracked state has spelled a complete word of its word set.
 */
internal data class RuleDecl(
    val chars: CharacterClass,
    val next: String,
    val ifWord: Boolean = false,
)

/**
 * One declared state of a tokenizer language.
 *
 * @property rules Ordered rules; the first match decides.
 * @property otherwise The state an unmatched byte moves to, or null to stay.
 * @property track The word set whose prefix the state spells, or null for an untracked state.
 */
internal data class StateDecl(
    val rules: List<RuleDecl> = emptyList(),
    val otherwise: String? = null,
    val track: String? = null,
)

/**
 * The declaration of a tokenizer language as written in a vocabulary entry, validated at construction.
 *
 * @property start The state a text begins in.
 * @property targets The states that count as the interesting position (for URL start, the three states
 *   after a URL attribute's `=` and its opening quote).
 * @property words Named word sets a tracked state matches against, case-insensitively.
 * @property states The declared states by name.
 * @throws IllegalArgumentException If a name is malformed or undeclared, a word is empty or not a Latin-1
 *   string, a rule's `ifWord` sits in an untracked state, or the start or a target is a tracked state.
 */
internal data class TokenizerDecl(
    val start: String,
    val targets: Set<String>,
    val words: Map<String, List<String>> = emptyMap(),
    val states: Map<String, StateDecl>,
) {
    init {
        states.keys.forEach { requireName("state", it) }
        words.keys.forEach { requireName("word set", it) }
        words.forEach { (set, members) -> requireWords(set, members) }
        states.forEach { (name, state) -> requireState(name, state) }
        require(targets.isNotEmpty()) { "Tokenizer declares no target state" }
        (targets + start).forEach { requireUntrackedState(it) }
    }

    private fun requireName(
        kind: String,
        name: String,
    ) = require(NAME_PATTERN.matches(name)) { "Tokenizer $kind name '$name' is not lowercase words joined by hyphens" }

    private fun requireWords(
        set: String,
        members: List<String>,
    ) {
        require(members.isNotEmpty()) { "Tokenizer word set '$set' is empty" }
        for (word in members) {
            require(word.isNotEmpty() && word.all { it.code < BYTE_VALUES }) {
                "Tokenizer word set '$set' holds '$word', which is empty or not a Latin-1 string"
            }
        }
    }

    private fun requireState(
        name: String,
        state: StateDecl,
    ) {
        state.track?.let { require(it in words) { "Tokenizer state '$name' tracks undeclared word set '$it'" } }
        val destinations = state.rules.map { it.next } + listOfNotNull(state.otherwise)
        destinations.forEach { require(it in states) { "Tokenizer state '$name' moves to undeclared state '$it'" } }
        require(state.track != null || state.rules.none { it.ifWord }) {
            "Tokenizer state '$name' has an ifWord rule but tracks no word set"
        }
    }

    private fun requireUntrackedState(name: String) {
        val state = states[name]
        require(state != null) { "Tokenizer start or target '$name' is not a declared state" }
        require(state.track == null) { "Tokenizer start or target '$name' is a tracked state" }
    }
}
