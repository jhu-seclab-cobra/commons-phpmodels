package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Decode contract of the `when:` guard: the scalar shapes a guard admits and
 * the shapes it rejects. The string scalar and the Long-overflow rejection:
 * [ModelLoaderTest]; guard admissibility per subject kind:
 * [ModelLoaderAdmissibilityTest].
 *
 * - `boolean guard value decodes` — `is: true` narrows to the boolean shape.
 * - `integer guard value decodes` — an integral scalar narrows to the
 *   integer shape.
 * - `non-scalar guard value is rejected` — the compared value is exactly one
 *   scalar; a sequence fails the decode.
 * - `float guard value is rejected` — a fractional number is outside the
 *   three admitted shapes.
 * - `non-finite float guard value is rejected` — `.inf` fails the load, and
 *   fails it inside the single-exception contract.
 * - `quoted scalar keeps the string shape` — quoting forces the string
 *   shape over the boolean and integer readings.
 * - `yaml boolean word narrows to the boolean shape` — pins the YAML 1.1
 *   boolean vocabulary (`no`, `off`) on an unquoted scalar.
 * - `yaml octal literal narrows to its decimal value` — pins the YAML 1.1
 *   reading of a zero-prefixed integer.
 * - `return port as guard port is rejected` — the guard tests an argument
 *   port only.
 * - `guard on a generator entry is rejected` — the `when:` field belongs to
 *   the flat model form alone.
 */
internal class WhenGuardTest {
    @Test
    fun `boolean guard value decodes`() {
        val model =
            loadModel(
                """
                - subject:
                    function: ini_set
                  when:
                    port: argument(1)
                    is: true
                  returns: any
                """.trimIndent(),
            )
        assertEquals(WhenGuard(Port.Argument(1), GuardValue.BoolValue(true)), model.guard)
    }

    @Test
    fun `integer guard value decodes`() {
        val model =
            loadModel(
                """
                - subject:
                    function: json_decode
                  when:
                    port: argument(2)
                    is: 512
                  returns: any
                """.trimIndent(),
            )
        assertEquals(WhenGuard(Port.Argument(2), GuardValue.IntValue(512)), model.guard)
    }

    @Test
    fun `non-scalar guard value is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  when:
                    port: argument(0)
                    is: [1, 2]
                  returns: num
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `float guard value is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: round
                  when:
                    port: argument(1)
                    is: 1.5
                  returns: num
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `non-finite float guard value is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: round
                  when:
                    port: argument(1)
                    is: .inf
                  returns: num
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `quoted scalar keeps the string shape`() {
        val model =
            loadModel(
                """
                - subject:
                    function: ini_set
                  when:
                    port: argument(1)
                    is: "true"
                  returns: any
                """.trimIndent(),
            )
        assertEquals(WhenGuard(Port.Argument(1), GuardValue.StrValue("true")), model.guard)
    }

    @Test
    fun `yaml boolean word narrows to the boolean shape`() {
        val model =
            loadModel(
                """
                - subject:
                    function: ini_set
                  when:
                    port: argument(1)
                    is: no
                  returns: any
                """.trimIndent(),
            )
        assertEquals(WhenGuard(Port.Argument(1), GuardValue.BoolValue(false)), model.guard)
    }

    @Test
    fun `yaml octal literal narrows to its decimal value`() {
        val model =
            loadModel(
                """
                - subject:
                    function: chmod
                  when:
                    port: argument(1)
                    is: 017
                  returns: bool
                """.trimIndent(),
            )
        assertEquals(WhenGuard(Port.Argument(1), GuardValue.IntValue(15)), model.guard)
    }

    @Test
    fun `return port as guard port is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  when:
                    port: return
                    is: true
                  returns: num
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `guard on a generator entry is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - name: getters
                  find: function
                  where:
                    - constraint: name
                      pattern: get_.*
                  when:
                    port: argument(0)
                    is: true
                  model:
                    returns: any
                """.trimIndent(),
            )
        }
    }
}
