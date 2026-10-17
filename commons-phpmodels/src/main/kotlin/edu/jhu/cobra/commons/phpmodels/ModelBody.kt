package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * One declared flow between two ports of a call.
 *
 * The YAML pair accepts the synonym spellings `from`/`input` and `to`/`output`;
 * all four arrive as nullable creator parameters because a Jackson alias would
 * let a pair naming both spellings of one side decode silently (impl.md).
 *
 * @property from The input port the data leaves: an argument or the receiver.
 * @property to The port the data reaches: the result, or a different port.
 * @throws IllegalArgumentException If a side is missing or doubled, or the flow
 *   targets its own source port.
 */
@ConsistentCopyVisibility
public data class Propagation private constructor(
    val from: Port.Input,
    val to: Port,
) {
    init {
        require(to != from) { "Propagation from $from targets its own source port" }
    }

    public companion object {
        /** Resolves each side's synonym pair into the stored, compared port. */
        @JvmStatic
        @JsonCreator
        public operator fun invoke(
            from: Port.Input? = null,
            input: Port.Input? = null,
            to: Port? = null,
            output: Port? = null,
        ): Propagation =
            Propagation(
                exactlyOne("from" to from, "input" to input),
                exactlyOne("to" to to, "output" to output),
            )
    }
}

// Each side arrives as its two spellings paired with their values; exactly
// one of the two must carry a value.
private fun <T : Any> exactlyOne(
    first: Pair<String, T?>,
    second: Pair<String, T?>,
): T {
    val (firstName, firstValue) = first
    val (secondName, secondValue) = second
    require(firstValue == null || secondValue == null) {
        "'$firstName' and '$secondName' are synonym spellings; declare one"
    }
    return firstValue ?: secondValue
        ?: throw IllegalArgumentException("Propagation is missing '$firstName' (or '$secondName')")
}

/**
 * One key pattern of a source element: restricts production to the array
 * elements whose entire key matches. Array keys are runtime data, not
 * identifiers, so the match is case-sensitive. Equality is over the pattern
 * text: the compiled [Regex] carries no value equality, so it stays out of
 * the primary constructor.
 *
 * @property pattern The declared pattern source.
 * @throws java.util.regex.PatternSyntaxException If [pattern] does not compile.
 */
public data class KeyPattern(
    public val pattern: String,
) {
    /** The compiled pattern; construction fails on an invalid pattern. */
    public val regex: Regex = Regex(pattern)

    /** True when the entire [key] matches the pattern. */
    public fun matches(key: String): Boolean = regex.matches(key)

    public companion object {
        /** Decodes the bare pattern scalar. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): KeyPattern = KeyPattern(raw)
    }
}

/**
 * The bytes a character bound admits: every byte the declared regular
 * expression matches, each byte read as one Latin-1 character. Equality is
 * over the pattern text, as for [KeyPattern].
 *
 * @property pattern The declared pattern source: `[^/]`, `[0-9a-f]`.
 * @throws java.util.regex.PatternSyntaxException If [pattern] does not compile.
 * @throws IllegalArgumentException If [pattern] matches no byte.
 */
public data class CharacterClass(
    public val pattern: String,
) {
    // One membership flag per byte value, evaluated once at construction.
    private val members: BooleanArray =
        Regex(pattern).let { regex -> BooleanArray(BYTE_VALUES) { regex.matches(it.toChar().toString()) } }

    init {
        require(members.any { it }) { "Character class '$pattern' matches no byte" }
    }

    /**
     * True when the byte value [byte] belongs to the class.
     *
     * @throws IllegalArgumentException If [byte] lies outside 0..255.
     */
    public fun contains(byte: Int): Boolean {
        require(byte in 0..<BYTE_VALUES) { "Not a byte value: $byte" }
        return members[byte]
    }

    public companion object {
        private const val BYTE_VALUES = 256

        /** Decodes the bare pattern scalar. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): CharacterClass = CharacterClass(raw)
    }
}

/**
 * The characters section: the bytes a call result may hold. Each declared
 * field bounds the result; both together narrow each other.
 *
 * @property of The argument ports whose bytes the result copies, or null
 *   when the result is not bounded by its arguments.
 * @property within The class every result byte belongs to, or null when no
 *   class bounds it.
 * @throws IllegalArgumentException If neither field is declared, or a
 *   declared port set is empty.
 */
public data class CharacterBound(
    val of: Set<Port.Argument>? = null,
    val within: CharacterClass? = null,
) {
    init {
        require(of != null || within != null) { "Characters section declares neither 'of' nor 'within'" }
        require(of == null || of.isNotEmpty()) { "Characters section declares an empty 'of' set" }
    }
}

/**
 * One produced color set of a sources section.
 *
 * @property origin Origin colors the produced value carries; YAML key `provenance`.
 * @property at The argument port produced into (a by-reference out-parameter),
 *   or null for the subject kind's default production site.
 * @property keys Key patterns restricting production to matching array keys,
 *   or null when the whole value is produced.
 * @throws IllegalArgumentException If no color is declared, or a declared
 *   key-pattern set is empty.
 */
public data class SourceDecl(
    @param:JsonProperty("provenance") @get:JsonProperty("provenance") val origin: Set<OriginId>,
    val at: Port.Argument? = null,
    val keys: List<KeyPattern>? = null,
) {
    init {
        require(origin.isNotEmpty()) { "Sources element declares no origin color" }
        require(keys == null || keys.isNotEmpty()) { "Sources element declares an empty key-pattern set" }
    }
}

/**
 * One dangerously consumed port of a sinks section.
 *
 * @property port The argument port consumed dangerously.
 * @property vulnClass The danger category a color reaching [port] enables; YAML key `category`.
 */
public data class SinkDecl(
    val port: Port.Argument,
    @param:JsonProperty("category") @get:JsonProperty("category") val vulnClass: VulnClassId,
)

/**
 * The sectioned statement of one model: seven optional assertion sections.
 *
 * `returns`, `propagation`, and `characters` form one value unit:
 * declaring `returns` asserts the flow set exhaustively, so an absent
 * propagation section means the result is unrelated to the arguments.
 * Declaring `propagation` or `characters` alone is a load failure — the unit
 * is asserted whole or not at all. A byte copied into the result is a flow
 * into it, so each `characters.of` port is the source of a declared flow into
 * the result.
 *
 * An all-absent body is constructible — a signature-only entry has one — and
 * the at-least-one-section rule therefore lives at the entry level, where the
 * signature is visible: `ModelEntry` requires a signature or a non-empty
 * body.
 *
 * `raises` is the one section whose declared empty value is a statement: the
 * implementation throws no class. Each element is an exact class, spelled and
 * folded as a class subject.
 *
 * @throws IllegalArgumentException If propagation or characters comes without
 *   returns, a `characters.of` port has no declared flow into the result, or a
 *   declared section other than `raises` is empty.
 */
public data class ModelBody(
    val returns: ReturnKind? = null,
    val propagation: List<Propagation>? = null,
    val characters: CharacterBound? = null,
    val sources: List<SourceDecl>? = null,
    val sinks: List<SinkDecl>? = null,
    val sanitizers: List<SanitizerDecl>? = null,
    val raises: Set<ClassSubject>? = null,
) {
    init {
        require(propagation == null || returns != null) {
            "Propagation without returns: the value unit is asserted whole or not at all"
        }
        require(characters == null || returns != null) {
            "Characters without returns: the value unit is asserted whole or not at all"
        }
        val copied = characters?.of.orEmpty() - flowSourcesIntoResult()
        require(copied.isEmpty()) { "Characters copied from $copied, which has no declared flow into the result" }
        requireDeclaredNonEmpty(propagation, "propagation")
        requireDeclaredNonEmpty(sources, "sources")
        requireDeclaredNonEmpty(sinks, "sinks")
        requireDeclaredNonEmpty(sanitizers, "sanitizers")
    }

    // An absent section is null; a declared section must list at least one element.
    private fun requireDeclaredNonEmpty(
        section: List<*>?,
        name: String,
    ) {
        require(section == null || section.isNotEmpty()) { "Declared $name section is empty" }
    }

    private fun flowSourcesIntoResult(): Set<Port.Input> =
        propagation.orEmpty().filter { it.to == Port.Return }.mapTo(mutableSetOf()) { it.from }

    // Every section other than sources is absent.
    private val declaresNothingBesidesSources: Boolean
        get() =
            returns == null && propagation == null && characters == null &&
                sinks == null && sanitizers == null && raises == null

    /** True when no section is declared. */
    public val isEmpty: Boolean
        get() = sources == null && declaresNothingBesidesSources

    /** True when the body declares nothing besides its sources section. */
    public val declaresOnlySources: Boolean
        get() = sources != null && declaresNothingBesidesSources

    // The two port-admissibility predicates below are the one authority the
    // entry validation reads; the subject-kind requirement lives there.

    /** True when a propagation side names the receiver port. */
    public val namesReceiverPort: Boolean
        get() = propagation.orEmpty().any { it.from == Port.Receiver || it.to == Port.Receiver }

    /** True when a source element declares an explicit production site. */
    public val declaresExplicitSourceSite: Boolean
        get() = sources.orEmpty().any { it.at != null }

    /** The value unit this body asserts, or null when returns is undeclared. */
    public fun valueUnit(): ValueUnit? = returns?.let { ValueUnit(it, propagation.orEmpty(), characters) }
}

/**
 * The value unit a lookup serves: the returns classification together
 * with the exhaustive propagation set and the optional character bound. An
 * absent propagation section in the declaring body becomes the empty list here
 * — no flow, not unknown flow; an absent characters section stays null — no
 * bound.
 *
 * @property returns Classification of the call's result.
 * @property propagation Every declared port-to-port flow.
 * @property characters The bytes the result may hold, or null when unbounded.
 */
public data class ValueUnit(
    val returns: ReturnKind,
    val propagation: List<Propagation>,
    val characters: CharacterBound? = null,
)
