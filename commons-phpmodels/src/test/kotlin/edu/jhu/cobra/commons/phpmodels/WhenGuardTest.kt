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
