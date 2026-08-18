package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.JsonNode
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.CallableSignature
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.ClassSignature
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.PropertySignature
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.TypedSignature
import kotlin.reflect.KClass

/**
 * One decoded configuration entry: a [SubjectModel] naming its subject
 * explicitly, or a [ModelGenerator] denoting one model per subject satisfying
 * its constraints. The forms carry no `type` tag — they are deduced from their
 * disjoint required fields (`subject` versus `name`/`find`/`where`/`model`),
 * and an entry matching neither or both fails the decode (impl.md).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(
    JsonSubTypes.Type(value = SubjectModel::class),
    JsonSubTypes.Type(value = ModelGenerator::class),
)
public sealed interface ModelEntry

/**
 * One explicit model: the subject it identifies and the sectioned statement
 * asserted for it. The subject is the entry's identity — the form carries no
 * name. The five section fields decode flat beside `subject`, with no wrapper
 * key.
 *
 * An entry carrying a `when:` guard is one branch of its subject's model,
 * selected per call; an unguarded entry is the default branch. An entry
 * carrying a `signature:` describes the declaration; the signature subtype is
 * selected by the subject kind at decode.
 *
 * @property subject The PHP declaration the model identifies.
 * @property guard The branch condition, or null for the default branch.
 * @property signature The declaration description, or null when undeclared.
 * @property body The sectioned statement this entry asserts.
 * @throws IllegalArgumentException If the entry asserts nothing, the subject
 *   admits neither the guard, the signature subtype, nor a declared section,
 *   or the body validation fails.
 */
public data class SubjectModel(
    public val subject: ModelSubject,
    public val guard: WhenGuard? = null,
    public val signature: SignatureInfo? = null,
    public val body: ModelBody = ModelBody(),
) : ModelEntry {
    init {
        require(signature != null || !body.isEmpty) {
            "Entry for $subject asserts nothing: no signature and no section"
        }
        validateAdmissibility()
        validatePortBounds()
    }

    // The subject admits only the sections its kind allows: assertion sections
    // and guards belong to callable kinds; value-producing kinds declare
    // sources; a class declares nothing besides its signature. Port
    // admissibility follows the same kind: the receiver port requires a
    // method, an explicit source site requires a callable.
    private fun validateAdmissibility() {
        val expected = signatureTypeFor(subject)
        require(signature == null || (expected != null && expected.isInstance(signature))) {
            "Entry for $subject carries a signature its kind does not admit: $signature"
        }
        require(subject is MethodSubject || !body.namesReceiverPort) {
            "Entry for $subject names the receiver port; 'this' exists only in a call to a method"
        }
        if (subject is FunctionSubject || subject is MethodSubject) return
        require(guard == null) { "Entry for $subject carries a when guard; guards apply to callable subjects only" }
        require(!body.declaresExplicitSourceSite) {
            "Entry for $subject declares an explicit source site; sites apply to callable subjects only"
        }
        val admitted = if (subject is ClassSubject) body.isEmpty else body.isEmpty || body.declaresOnlySources
        require(admitted) { "Entry for $subject declares a section its kind does not admit" }
    }

    // A declared callable signature fixes the arity: every argument port the
    // entry names lies inside the parameter list, unless the last parameter
    // collects the variadic tail.
    private fun validatePortBounds() {
        val callable = signature as? CallableSignature ?: return
        if (callable.params.lastOrNull()?.variadic == true) return
        val limit = callable.params.size
        val beyond = namedArgumentPorts().firstOrNull { it.position >= limit }
        require(beyond == null) {
            "Entry for $subject names $beyond beyond its $limit-parameter signature"
        }
    }

    private fun namedArgumentPorts(): List<Port.Argument> =
        buildList {
            guard?.let { add(it.port) }
            body.propagation?.forEach { pair ->
                (pair.from as? Port.Argument)?.let(::add)
                (pair.to as? Port.Argument)?.let(::add)
            }
            body.sinks?.forEach { add(it.port) }
            body.sources?.forEach { source -> source.at?.let(::add) }
        }

    public companion object {
        // The signature mapping carries no discriminator: its subtype is
        // narrowed here from the subject kind (impl.md). A propagation section
        // beside a callable signature is completed into the value-semantics
        // unit with the derived classification before body construction, so
        // ModelBody's propagation-requires-returns rule holds unchanged.
        @JvmStatic
        @JsonCreator
        internal fun decode(
            @JsonProperty("subject") subject: ModelSubject,
            @JsonProperty("when") guard: WhenGuard?,
            @JsonProperty("signature") signature: JsonNode?,
            @JsonUnwrapped sections: SectionFields,
        ): SubjectModel {
            val decoded = signature?.let { narrowSignature(subject, it) }
            val callable = decoded as? CallableSignature
            require(sections.returns == null || callable == null) {
                "Entry for $subject declares 'returns' beside a callable signature: one fact, one source"
            }
            val effectiveReturns =
                sections.returns
                    ?: callable?.returnType?.toReturnKind()?.takeIf { sections.propagation != null }
            return SubjectModel(subject, guard, decoded, sections.complete(effectiveReturns))
        }

        private fun narrowSignature(
            subject: ModelSubject,
            node: JsonNode,
        ): SignatureInfo {
            val expected =
                signatureTypeFor(subject)
                    ?: throw IllegalArgumentException(
                        "Entry for $subject carries a signature; superglobals are hand-declared",
                    )
            return ModelYaml.narrow(node, expected.java)
        }

        // The one authority for the subject-kind → signature-subtype mapping;
        // the admissibility check and the decode-time narrowing both read it.
        // Null marks the kind whose entries admit no signature.
        private fun signatureTypeFor(subject: ModelSubject): KClass<out SignatureInfo>? =
            when (subject) {
                is FunctionSubject, is MethodSubject -> CallableSignature::class
                is ClassSubject -> ClassSignature::class
                is ConstantSubject, is ClassConstantSubject -> TypedSignature::class
                is PropertySubject -> PropertySignature::class
                is VariableSubject -> null
            }
    }
}

/**
 * The five section fields as they decode flat beside `subject`, gathered into
 * the creator through one `@JsonUnwrapped` parameter — before the
 * signature-derived returns completion, so [ModelBody]'s
 * propagation-requires-returns rule stays out of the raw decode.
 */
internal data class SectionFields(
    val returns: ReturnKind? = null,
    val propagation: List<Propagation>? = null,
    val sources: List<SourceDecl>? = null,
    val sinks: List<SinkPoint>? = null,
    val sanitizers: List<SanitizerDecl>? = null,
) {
    /** The entry's body, with [returns] completed by the enclosing creator. */
    fun complete(returns: ReturnKind?): ModelBody = ModelBody(returns, propagation, sources, sinks, sanitizers)

    // The unwrapped path funnels every key the creator does not name into this
    // holder and would ignore the unknown ones; this rejector restores the
    // strict unknown-key failure the format states (impl.md).
    @JsonAnySetter
    fun rejectUnknown(
        key: String,
        @Suppress("UNUSED_PARAMETER") value: JsonNode?,
    ): Unit = throw IllegalArgumentException("Unknown key '$key' in a model entry")
}
