package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * By-reference requirements on written-into ports and the void return type
 * a declared callable signature fixes (design-entries.md). Arity bound:
 * [ModelEntryArityTest]; direct construction: [ModelEntryTest].
 *
 * - `variadic by-reference tail admits a written port` — the tail position
 *   resolves to the variadic parameter's by-reference flag.
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
internal class ModelEntryWrittenPortTest {
    @Test
    fun `variadic by-reference tail admits a written port`() {
        val model =
            loadModel(
                """
                - subject:
                    function: foo
                  signature:
                    params:
                      - name: format
                        type: string
                      - name: outputs
                        type: mixed
                        byRef: true
                        variadic: true
                    returnType: bool
                  sources:
                    - provenance: [remote]
                      at: argument(5)
                """.trimIndent(),
            )
        val source = model.body.sources!!.single()
        assertEquals(Port.Argument(5), source.at)
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
