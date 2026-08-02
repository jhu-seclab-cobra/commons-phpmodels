package edu.jhu.cobra.commons.phpmodels

/**
 * One declared flow between two ports of a call.
 *
 * The YAML pair accepts the synonym spellings `from`/`input` and `to`/`output`;
 * all four arrive as nullable creator parameters because a Jackson alias would
 * let a pair naming both spellings of one side decode silently (impl.md).
 *
 * @property from The argument port the data leaves.
 * @property to The port the data reaches: the result, or a different argument.
 * @throws IllegalArgumentException If a side is missing or doubled, or the flow
 *   targets its own source port.
 */
public class Propagation(
    from: Port.Argument? = null,
    input: Port.Argument? = null,
    to: Port? = null,
    output: Port? = null,
) {
    public val from: Port.Argument = exactlyOne("from", from, "input", input)
    public val to: Port = exactlyOne("to", to, "output", output)

    init {
        require(this.to != this.from) { "Propagation from ${this.from} targets its own source port" }
    }

    override fun equals(other: Any?): Boolean = other is Propagation && from == other.from && to == other.to

    override fun hashCode(): Int = 31 * from.hashCode() + to.hashCode()

    override fun toString(): String = "Propagation(from=$from, to=$to)"
}

private fun <T : Any> exactlyOne(
    aName: String,
    a: T?,
    bName: String,
    b: T?,
): T {
    require(a == null || b == null) { "'$aName' and '$bName' are synonym spellings; declare one" }
    return a ?: b ?: throw IllegalArgumentException("Propagation is missing '$aName' (or '$bName')")
}

/**
 * One produced color set of a sources section.
 *
 * @property provenance Origin colors the produced value carries.
 * @throws IllegalArgumentException If no color is declared.
 */
public data class SourceDecl(
    val provenance: Set<ProvenanceId>,
) {
    init {
        require(provenance.isNotEmpty()) { "Sources element declares no origin color" }
    }
}

/**
 * One dangerously consumed port of a sinks section.
 *
 * @property port The argument port consumed dangerously.
 * @property category The danger category a color reaching [port] enables.
 */
public data class SinkPoint(
    val port: Port.Argument,
    val category: VulnClassId,
)

/**
 * One neutralized category set of a sanitizers section.
 *
 * @property categories Danger categories neutralized for values passing through.
 * @throws IllegalArgumentException If no category is declared.
 */
public data class SanitizerDecl(
    val categories: Set<VulnClassId>,
) {
    init {
        require(categories.isNotEmpty()) { "Sanitizers element declares no category" }
    }
}

/**
 * The sectioned statement of one model: five optional assertion sections. One
 * shape shared by the flat entry and the generator body, so a body written in
 * either form carries the same validation.
 *
 * `returns` and `propagation` form one value-semantics unit: declaring `returns`
 * asserts the flow set exhaustively, so an absent propagation section means the
 * result is unrelated to the arguments. Declaring `propagation` alone is a load
 * failure — the unit is asserted whole or not at all.
 *
 * An all-absent body is constructible — a signature-only entry has one — and
 * the at-least-one-section rule therefore lives at the entry level, where the
 * signature is visible: `SubjectModel` requires a signature or a non-empty
 * body, `ModelGenerator` requires a non-empty body.
 *
 * @throws IllegalArgumentException If propagation comes without returns, or a
 *   declared section is empty.
 */
public data class ModelBody(
    val returns: ReturnKind? = null,
    val propagation: List<Propagation>? = null,
    val sources: List<SourceDecl>? = null,
    val sinks: List<SinkPoint>? = null,
    val sanitizers: List<SanitizerDecl>? = null,
) {
    init {
        require(propagation == null || returns != null) {
            "Propagation without returns: the value-semantics unit is asserted whole or not at all"
        }
        require(propagation == null || propagation.isNotEmpty()) { "Declared propagation section is empty" }
        require(sources == null || sources.isNotEmpty()) { "Declared sources section is empty" }
        require(sinks == null || sinks.isNotEmpty()) { "Declared sinks section is empty" }
        require(sanitizers == null || sanitizers.isNotEmpty()) { "Declared sanitizers section is empty" }
    }

    /** True when no section is declared. */
    public val isEmpty: Boolean
        get() = returns == null && propagation == null && sources == null && sinks == null && sanitizers == null

    /** True when the body declares nothing besides its sources section. */
    public val declaresOnlySources: Boolean
        get() = sources != null && returns == null && propagation == null && sinks == null && sanitizers == null

    /** The value-semantics unit this body asserts, or null when returns is undeclared. */
    public fun valueSemantics(): ValueSemantics? = returns?.let { ValueSemantics(it, propagation.orEmpty()) }
}

/**
 * The value-semantics unit a lookup serves: the returns classification together
 * with the exhaustive propagation set. An absent propagation section in the
 * declaring body becomes the empty list here — no flow, not unknown flow.
 *
 * @property returns Classification of the call's result.
 * @property propagation Every declared port-to-port flow.
 */
public data class ValueSemantics(
    val returns: ReturnKind,
    val propagation: List<Propagation>,
)
