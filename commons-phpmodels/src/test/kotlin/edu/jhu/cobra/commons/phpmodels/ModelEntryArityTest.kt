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
                      category: sqli
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
                      category: sqli
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
                      category: sqli
                """.trimIndent(),
            )
        assertEquals(listOf(SinkDecl(Port.Argument(1), VulnClassId("sqli"))), model.body.sinks)
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
                      category: sqli
                """.trimIndent(),
            )
        assertEquals(listOf(SinkDecl(Port.Argument(9), VulnClassId("sqli"))), model.body.sinks)
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
                    - provenance: [remote]
                      at: argument(1)
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
                      category: sqli
                """.trimIndent(),
            )
        assertEquals(listOf(SinkDecl(Port.Argument(5), VulnClassId("sqli"))), model.body.sinks)
    }
}
