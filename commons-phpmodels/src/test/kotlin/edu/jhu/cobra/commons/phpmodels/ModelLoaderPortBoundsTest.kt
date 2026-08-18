package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Port semantics fixed by a declared callable signature: arity bounds,
 * by-reference requirements on written-into ports, and the void return type.
 *
 * - `port beyond the declared parameter list is rejected` — a guard,
 *   propagation, or sink argument port outside a callable signature's arity.
 * - `source site beyond the declared parameter list is rejected` — the
 *   arity bound covers the source `at` port too.
 * - `variadic signature admits ports beyond the declared list` — the
 *   variadic tail collects every remaining position.
 * - `written ports into by-reference parameters decode` — a propagation
 *   target and a source site on by-reference parameters.
 * - `propagation into a by-value parameter is rejected` — a flow cannot
 *   store into an argument taken by value.
 * - `source site names a by-value parameter is rejected` — an out-parameter
 *   site requires the by-reference declaration.
 * - `written port in a by-value variadic tail is rejected` — the tail
 *   position resolves to the variadic parameter's by-reference flag.
 * - `flow into the result of a void callable is rejected` — a void return
 *   type declares there is no result to flow into.
 */
internal class ModelLoaderPortBoundsTest {
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
                  when:
                    port: argument(1)
                    is: true
                  sinks:
                    - port: argument(0)
                      category: sqli
                """.trimIndent(),
            )
        }
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
        assertEquals(listOf(SinkPoint(Port.Argument(5), VulnClassId("sqli"))), model.body.sinks)
    }

    @Test
    fun `written ports into by-reference parameters decode`() {
        val model =
            loadModel(
                """
                - subject:
                    function: parse_str
                  signature:
                    params:
                      - name: string
                        type: string
                      - name: result
                        type: array
                        byRef: true
                    returnType: void
                  propagation:
                    - from: argument(0)
                      to: argument(1)
                  sources:
                    - provenance: [remote]
                      at: argument(1)
                """.trimIndent(),
            )
        val source = model.body.sources!!.single()
        assertEquals(Port.Argument(1), source.at)
    }

    @Test
    fun `propagation into a by-value parameter is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: value
                        type: string
                      - name: target
                        type: array
                    returnType: void
                  propagation:
                    - from: argument(0)
                      to: argument(1)
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `source site names a by-value parameter is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: result
                        type: array
                    returnType: bool
                  sources:
                    - provenance: [remote]
                      at: argument(0)
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `written port in a by-value variadic tail is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: format
                        type: string
                      - name: values
                        type: mixed
                        variadic: true
                    returnType: bool
                  sources:
                    - provenance: [remote]
                      at: argument(5)
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `flow into the result of a void callable is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: value
                        type: string
                    returnType: void
                  propagation:
                    - from: argument(0)
                      to: return
                """.trimIndent(),
            )
        }
    }
}
