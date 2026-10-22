package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The arity bound a declared callable signature fixes on argument ports
 * (design-entries.md). Written-port and void rules:
 * [ModelEntryWrittenPortTest]; direct construction: [ModelEntryTest].
 *
 * - `port beyond the declared parameter list is rejected` — a condition,
 *   propagation, or sink argument port outside a callable signature's arity.
 * - `port at the last declared position is admitted` — the arity bound is
 *   exclusive of the parameter count.
 * - `entry without a signature is not arity-checked` — no declared list,
 *   no bound.
 * - `source site beyond the declared parameter list is rejected` — the
 *   arity bound covers the source `at` port too.
 * - `checked port beyond the declared parameter list is rejected` — the
 *   arity bound covers a sanitizers check's argument port too.
 * - `one-opcode keys decode into the callable signature` — the declared
 *   counts and rope format, and empty or null when the keys are absent.
 * - `method entry declaring a one-opcode key is rejected` — the facts
 *   describe a function only.
 * - `variadic signature admits ports beyond the declared list` — the
 *   variadic tail collects every remaining position.
 */
internal class ModelEntryArityTest {
    @Test
    fun `port beyond the declared parameter list is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: query
                        type: string
                    returnType: bool
                  sinks:
                    - port: argument(1)
                      vulnType: sqli
                """.trimIndent(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: value
                        type: string
                    returnType: string
                  propagation:
                    - from: argument(1)
                      to: return
                """.trimIndent(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: value
                        type: string
                    returnType: bool
                  when: [_, true]
                  sinks:
                    - port: argument(0)
                      vulnType: sqli
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `port at the last declared position is admitted`() {
        val model =
            loadModel(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: query
                        type: string
                      - name: mode
                        type: int
                    returnType: bool
                  when: [_, 1]
                  sinks:
                    - port: argument(1)
                      vulnType: sqli
                """.trimIndent(),
            )
        assertEquals(listOf(SinkDecl(Port.Argument(1), VulnTypeId("sqli"))), model.body.sinks)
    }

    @Test
    fun `entry without a signature is not arity-checked`() {
        val model =
            loadModel(
                """
                - subject:
                    function: foo
                  when: [_, _, _, _, _, _, _, _, _, true]
                  sinks:
                    - port: argument(9)
                      vulnType: sqli
                """.trimIndent(),
            )
        assertEquals(listOf(SinkDecl(Port.Argument(9), VulnTypeId("sqli"))), model.body.sinks)
    }

    @Test
    fun `source site beyond the declared parameter list is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: value
                        type: string
                    returnType: bool
                  sources:
                    - sourceKinds: [remote]
                      at: argument(1)
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `checked port beyond the declared parameter list is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: ctype_digit
                  signature:
                    params:
                      - name: text
                        type: mixed
                    returnType: bool
                  sanitizers:
                    - check:
                        argument: argument(1)
                        accepts: true
                        within: "[0-9]"
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `variadic signature admits ports beyond the declared list`() {
        val model =
            loadModel(
                """
                - subject:
                    function: sprintf
                  signature:
                    params:
                      - name: format
                        type: string
                      - name: values
                        type: mixed
                        variadic: true
                    returnType: string
                  sinks:
                    - port: argument(5)
                      vulnType: sqli
                """.trimIndent(),
            )
        assertEquals(listOf(SinkDecl(Port.Argument(5), VulnTypeId("sqli"))), model.body.sinks)
    }

    @Test
    fun `one-opcode keys decode into the callable signature`() {
        val declared =
            loadModel(withKeys("framelessArities: [1, 2]", "specialArities: [2]", "ropeFormat: a")).signature
        declared as SignatureInfo.CallableSignature
        assertEquals(setOf(1, 2), declared.framelessArities)
        assertEquals(setOf(2), declared.specialArities)
        assertEquals("a", declared.ropeFormat)
        val absent = loadModel(withKeys()).signature as SignatureInfo.CallableSignature
        assertEquals(emptySet(), absent.framelessArities)
        assertEquals(emptySet(), absent.specialArities)
        assertEquals(null, absent.ropeFormat)
    }

    @Test
    fun `method entry declaring a one-opcode key is rejected`() {
        for (key in listOf("framelessArities: [1]", "specialArities: [2]", "ropeFormat: a")) {
            assertFailsWith<IllegalArgumentException> { load(withKeys(key, subject = "method: C::foo")) }
        }
        assertEquals(1, load(withKeys(subject = "method: C::foo")).size)
    }

    // One entry of an optional second parameter; [keys] are the last signature lines.
    private fun withKeys(vararg keys: String, subject: String = "function: foo"): String =
        listOf(
            "- subject:",
            "    $subject",
            "  signature:",
            "    params:",
            "      - name: a",
            "        type: string",
            "      - name: b",
            "        type: string",
            "        optional: true",
            "    returnType: bool",
        ).plus(keys.map { "    $it" }).joinToString("\n")
}
