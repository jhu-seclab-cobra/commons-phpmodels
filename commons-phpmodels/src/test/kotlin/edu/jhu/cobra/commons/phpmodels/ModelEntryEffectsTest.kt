package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.CallableSignature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The effects assertion of the Value unit: its decoding boundary and its
 * admissibility (model.md, model-declarations.md, design-entries.md).
 * `strlen("abc")` returns 3; the hand-written entry below states its Value
 * unit with `effects: []`.
 *
 * - `omitted effects decodes to no assertion` — the body and its Value unit
 *   carry null.
 * - `empty effects decodes to the assertion` — `effects: []` reaches the body
 *   and the Value unit as the empty list, on a function and on a method entry.
 * - `explicit null effects is rejected` — `effects: null` and `effects: ~` are
 *   not omission.
 * - `effects other than the empty sequence is rejected` — a scalar, a mapping
 *   and a nonempty sequence.
 * - `effects on a non-callable subject is rejected` — constant, class,
 *   variable and operation subjects.
 * - `effects beside a signature is rejected` — a callable signature, with and
 *   without a propagation section that derives returns.
 * - `effects without explicit returns is rejected` — decoded and directly
 *   constructed.
 * - `directly constructed entry with effects beside a signature is rejected` —
 *   the entry `init` holds the rule without the creator.
 */
internal class ModelEntryEffectsTest {
    private val strlen = "- subject:\n    function: strlen\n  returns: num\n"
    private val flow = "  propagation:\n    - from: argument(0)\n      to: return\n"

    @Test
    fun `omitted effects decodes to no assertion`() {
        val entry = loadModel(strlen + flow)
        assertNull(entry.body.effects)
        assertNull(entry.body.valueUnit()?.effects)
    }

    @Test
    fun `empty effects decodes to the assertion`() {
        val entry = loadModel(strlen + flow + "  effects: []\n")
        assertEquals(emptyList(), entry.body.effects)
        assertEquals(emptyList(), entry.body.valueUnit()?.effects)
        val method = loadModel("- subject:\n    method: arrayobject::count\n  returns: num\n  effects: []\n")
        assertEquals(emptyList(), method.body.effects)
    }

    @Test
    fun `explicit null effects is rejected`() {
        listOf("null", "~").forEach { value ->
            val failure =
                assertFailsWith<IllegalArgumentException>(value) { load(strlen + flow + "  effects: $value\n") }
            assertIs<ValueInstantiationException>(failure.cause, value)
        }
    }

    @Test
    fun `effects other than the empty sequence is rejected`() {
        listOf("0", "none", "{}", "[io]", "[[]]").forEach { value ->
            assertFailsWith<IllegalArgumentException>(value) { load(strlen + flow + "  effects: $value\n") }
        }
    }

    @Test
    fun `effects on a non-callable subject is rejected`() {
        listOf("constant: PHP_EOL", "class: mysqli", "variable: ${'$'}_GET", "operation: dynamic_invocation").forEach {
            assertFailsWith<IllegalArgumentException>(it) {
                load("- subject:\n    $it\n  returns: str\n  effects: []\n")
            }
        }
    }

    @Test
    fun `effects beside a signature is rejected`() {
        val signed =
            "- subject:\n    function: strlen\n  signature:\n    params:\n      - name: string\n" +
                "        type: string\n    returnType: int\n"
        assertFailsWith<IllegalArgumentException> { load(signed + "  effects: []\n") }
        assertFailsWith<IllegalArgumentException> { load(signed + flow + "  effects: []\n") }
    }

    @Test
    fun `effects without explicit returns is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load("- subject:\n    function: strlen\n" + flow + "  effects: []\n")
        }
        assertFailsWith<IllegalArgumentException> { ModelBody(effects = emptyList()) }
    }

    @Test
    fun `directly constructed entry with effects beside a signature is rejected`() {
        val signature = CallableSignature(listOf(ParameterInfo("string", DeclaredType("string"))), DeclaredType("int"))
        val body = ModelBody(returns = ReturnKind.NUM, effects = emptyList())
        assertFailsWith<IllegalArgumentException> { ModelEntry(FunctionSubject("strlen"), null, signature, body) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(ConstantSubject("PHP_EOL"), body = body) }
        assertEquals(body, ModelEntry(FunctionSubject("strlen"), body = body).body)
    }
}
