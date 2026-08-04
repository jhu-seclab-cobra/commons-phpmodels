package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Pins the Jackson mechanisms the subject decoding of [design.md] relies on:
 * a one-key wrapper mapping (`function: strlen`) routed by
 * `Id.NAME`/`As.WRAPPER_OBJECT` into a subtype whose companion carries a
 * delegating string creator. Throwaway replica types; production types must
 * not leak in.
 */
internal class JacksonSubjectProbeTest {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes(
        JsonSubTypes.Type(value = ProbeFunction::class, name = "function"),
        JsonSubTypes.Type(value = ProbeMethod::class, name = "method"),
    )
    sealed interface ProbeSubject

    class ProbeFunction(
        val name: String,
    ) : ProbeSubject {
        companion object {
            @JvmStatic
            @JsonCreator
            fun parse(raw: String): ProbeFunction = ProbeFunction(raw.lowercase())
        }
    }

    class ProbeMethod(
        val owner: String,
        val name: String,
    ) : ProbeSubject {
        companion object {
            @JvmStatic
            @JsonCreator
            fun parse(raw: String): ProbeMethod {
                val pieces = raw.split("::")
                require(pieces.size == 2) { "Expected class::name, got '$raw'" }
                return ProbeMethod(pieces[0].lowercase(), pieces[1].lowercase())
            }
        }
    }

    data class ProbeEntry(
        val subject: ProbeSubject,
        val note: String? = null,
    )

    private val mapper: ObjectMapper =
        ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private inline fun <reified T> decode(yaml: String): T = mapper.readValue(yaml, jacksonTypeRef<T>())

    @Test
    fun `wrapper object key routes to the subtype string creator`() {
        val entry = decode<ProbeEntry>("subject:\n  function: STRLEN\n")
        val subject = assertIs<ProbeFunction>(entry.subject)
        assertEquals("strlen", subject.name)
    }

    @Test
    fun `wrapper object decodes a member spelling through the splitter`() {
        val entry = decode<ProbeEntry>("subject:\n  method: mysqli::Query\n")
        val subject = assertIs<ProbeMethod>(entry.subject)
        assertEquals("mysqli", subject.owner)
        assertEquals("query", subject.name)
    }

    @Test
    fun `unknown wrapper key is rejected`() {
        assertFailsWith<InvalidTypeIdException> {
            decode<ProbeEntry>("subject:\n  trait: foo\n")
        }
    }

    @Test
    fun `creator require failure surfaces as instantiation failure`() {
        assertFailsWith<ValueInstantiationException> {
            decode<ProbeEntry>("subject:\n  method: no_separator\n")
        }
    }

    @Test
    fun `value class init require rejects at decode`() {
        // The value-class unwrapping path reports the init failure as a plain
        // JsonMappingException (not ValueInstantiationException); still a
        // JsonProcessingException, so the one-catch load boundary holds.
        val failure =
            assertFailsWith<JsonMappingException> {
                decode<ProbeTyped>("type: 'not a type!'\n")
            }
        assertIs<IllegalArgumentException>(failure.cause)
        assertEquals("int", decode<ProbeTyped>("type: int\n").type.raw)
    }

    @Test
    fun `tree node parameter narrows per sibling field inside a creator`() {
        val callable = decode<ProbeSigEntry>("kind: callable\nsignature:\n  returnType: string\n")
        assertIs<ProbeSig.Callable>(callable.signature)
        val typed = decode<ProbeSigEntry>("kind: typed\nsignature:\n  type: int\n")
        assertIs<ProbeSig.Typed>(typed.signature)
    }

    @Test
    fun `tree narrowing keeps strict unknown-key rejection`() {
        // The inner treeToValue failure propagates as its own
        // UnrecognizedPropertyException rather than being wrapped into a
        // ValueInstantiationException by the outer creator; still one catch.
        assertFailsWith<UnrecognizedPropertyException> {
            decode<ProbeSigEntry>("kind: typed\nsignature:\n  type: int\n  stray: 1\n")
        }
    }
}

@JvmInline
internal value class ProbeType(
    val raw: String,
) {
    init {
        require(raw.all { it.isLetterOrDigit() || it == '_' || it == '\\' }) { "Bad type '$raw'" }
    }
}

internal data class ProbeTyped(
    val type: ProbeType,
)

internal sealed interface ProbeSig {
    data class Callable(
        val returnType: String,
    ) : ProbeSig

    data class Typed(
        val type: String,
    ) : ProbeSig
}

/**
 * Replica of the entry-level signature narrowing: the creator receives the
 * signature as a raw tree and converts it to the subtype the sibling `kind`
 * field selects, through the same strict mapper.
 */
internal class ProbeSigEntry private constructor(
    val kind: String,
    val signature: ProbeSig,
) {
    companion object {
        private val mapper: ObjectMapper =
            ObjectMapper(YAMLFactory())
                .registerKotlinModule()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        @JvmStatic
        @JsonCreator
        fun decode(
            kind: String,
            signature: com.fasterxml.jackson.databind.JsonNode,
        ): ProbeSigEntry {
            val decoded: ProbeSig =
                when (kind) {
                    "callable" -> mapper.treeToValue(signature, ProbeSig.Callable::class.java)
                    else -> mapper.treeToValue(signature, ProbeSig.Typed::class.java)
                }
            return ProbeSigEntry(kind, decoded)
        }
    }
}
