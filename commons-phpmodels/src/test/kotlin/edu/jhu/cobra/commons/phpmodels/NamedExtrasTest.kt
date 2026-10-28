package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Builtin variadic named-extra facts through the existing signature decoder.
 *
 * - `fixture decodes rejection positions and forwarding` — both established states survive loading.
 * - `unextracted and nonvariadic parameters have no policy` — absence stays null.
 * - `parameter policy round trips through YAML` — each state retains its value.
 * - `negative reception position is rejected` — positions start at zero.
 * - `nonvariadic policy is rejected` — only a variadic parameter owns this fact.
 * - `malformed policy is rejected` — unknown states, keys and missing positions fail decoding.
 */
internal class NamedExtrasTest {
    private fun parameters(): List<ParameterInfo> =
        checkNotNull(javaClass.getResourceAsStream("/named-extras/valid.yaml"))
            .use(ModelLoader::load)
            .map { assertIs<SignatureInfo.CallableSignature>(it.signature).params.single() }

    @Test
    fun `fixture decodes rejection positions and forwarding`() {
        assertEquals(
            listOf(NamedExtras.Reject(0), NamedExtras.Reject(1), NamedExtras.Forward),
            parameters().take(3).map { it.namedExtras },
        )
    }

    @Test
    fun `unextracted and nonvariadic parameters have no policy`() {
        parameters().drop(3).forEach { assertNull(it.namedExtras) }
    }

    @Test
    fun `parameter policy round trips through YAML`() {
        val mapper = YAMLMapper().registerKotlinModule()
        for (parameter in parameters()) {
            val yaml = mapper.writeValueAsString(parameter)
            assertEquals(parameter, ModelYaml.decode(yaml.byteInputStream(), jacksonTypeRef<ParameterInfo>()))
        }
    }

    @Test
    fun `negative reception position is rejected`() {
        assertFailsWith<IllegalArgumentException> { NamedExtras.Reject(-1) }
    }

    @Test
    fun `nonvariadic policy is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ParameterInfo("value", DeclaredType("mixed"), namedExtras = NamedExtras.Forward)
        }
    }

    @Test
    fun `malformed policy is rejected`() {
        val policies =
            listOf(
                "{kind: reject}",
                "{kind: reject, position: null}",
                "{kind: reject, position: -1}",
                "{kind: accept}",
                "{kind: forward, position: 0}",
                "{kind: reject, position: 1, extra: true}",
            )
        for (policy in policies) {
            val yaml = "name: args\ntype: mixed\nvariadic: true\nnamedExtras: $policy\n"
            assertFailsWith<IllegalArgumentException>(policy) {
                ModelYaml.decode<ParameterInfo>(yaml.byteInputStream(), jacksonTypeRef())
            }
        }
    }
}
