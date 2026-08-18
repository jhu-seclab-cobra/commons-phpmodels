package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Subject-kind admissibility of decoded entries: which sections, guards, and
 * ports each subject kind admits.
 *
 * - `variable entry declaring a sink is rejected` — sources-only kinds.
 * - `class entry declaring sources is rejected` — a class asserts nothing
 *   besides its signature.
 * - `guard on a non-callable subject is rejected` — guards are callable-only.
 * - `receiver port decodes on a method entry` — `this` as a propagation side.
 * - `receiver port on a non-method subject is rejected` — `this` exists only
 *   in a call to a method.
 * - `explicit source site and key patterns decode` — `at:` names the
 *   out-parameter port, `keys:` restricts production to matching array keys.
 * - `explicit source site on a non-callable subject is rejected` — sites
 *   apply to callable subjects only.
 */
internal class ModelLoaderAdmissibilityTest {
    @Test
    fun `variable entry declaring a sink is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    variable: ${'$'}_GET
                  sinks:
                    - port: argument(0)
                      category: sqli
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `class entry declaring sources is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    class: mysqli
                  signature:
                    classifier: class
                  sources:
                    - provenance: [remote]
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `guard on a non-callable subject is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    constant: PHP_EOL
                  when:
                    port: argument(0)
                    is: true
                  signature:
                    type: string
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `receiver port decodes on a method entry`() {
        val model =
            loadModel(
                """
                - subject:
                    method: mysqli_stmt::bind_param
                  returns: bool
                  propagation:
                    - from: argument(1)
                      to: this
                """.trimIndent(),
            )
        assertEquals(
            listOf(Propagation(from = Port.Argument(1), to = Port.Receiver)),
            model.body.propagation,
        )
    }

    @Test
    fun `receiver port on a non-method subject is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strtolower
                  returns: str
                  propagation:
                    - from: this
                      to: return
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `explicit source site and key patterns decode`() {
        val model =
            loadModel(
                """
                - subject:
                    function: parse_str
                  sources:
                    - provenance: [remote]
                      at: argument(1)
                      keys: ["user_.*"]
                """.trimIndent(),
            )
        val source = model.body.sources!!.single()
        assertEquals(Port.Argument(1), source.at)
        assertEquals(listOf(KeyPattern("user_.*")), source.keys)
    }

    @Test
    fun `explicit source site on a non-callable subject is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    variable: ${'$'}_GET
                  sources:
                    - provenance: [remote]
                      at: argument(0)
                """.trimIndent(),
            )
        }
    }
}
