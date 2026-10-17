package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.value.BoolVal
import edu.jhu.cobra.commons.value.IntVal
import edu.jhu.cobra.commons.value.NullVal
import edu.jhu.cobra.commons.value.Unsure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Decode and construction contract of a sanitizers element's check
 * (design-bodies.md, agent decision CK-1).
 *
 * - `check with a class decodes` — argument port, accepting value, and
 *   `within` class.
 * - `check with a type set decodes` — `types` reads as a declared type.
 * - `check with only vulnerability types decodes` — vulnerability types hold on
 *   the accepted
 *   argument.
 * - `accepting value narrows as a condition scalar` — integer, boolean, and
 *   the explicit null scalar read as the commons-value primitives.
 * - `absent accepting value is rejected`, `non-scalar accepting value is
 *   rejected` — the creator rules.
 * - `check stating nothing about the argument is rejected`, `void type set
 *   is rejected`, `unsure accepting value is rejected` — the construction
 *   rules.
 */
internal class ArgumentCheckTest {
    private fun sanitizer(element: String): SanitizerDecl =
        loadModel("- subject:\n    function: f\n  sanitizers:\n" + element.trimIndent().prependIndent("    ") + "\n")
            .body
            .sanitizers
            .orEmpty()
            .single()

    @Test
    fun `check with a class decodes`() {
        val decl =
            sanitizer(
                """
                - check:
                    argument: argument(0)
                    accepts: true
                    within: "[0-9]"
                """,
            )
        val check = ArgumentCheck(Port.Argument(0), BoolVal(true), CharacterClass("[0-9]"))
        assertEquals(SanitizerDecl(emptySet(), check), decl)
    }

    @Test
    fun `check with a type set decodes`() {
        val decl =
            sanitizer(
                """
                - check:
                    argument: argument(0)
                    accepts: true
                    types: int|float
                """,
            )
        assertEquals(DeclaredType("int|float"), decl.check?.types)
    }

    @Test
    fun `check with only vulnerability types decodes`() {
        val decl =
            sanitizer(
                """
                - vulnTypes: [sqli]
                  check:
                    argument: argument(1)
                    accepts: 1
                """,
            )
        assertEquals(SanitizerDecl(setOf(VulnTypeId("sqli")), ArgumentCheck(Port.Argument(1), IntVal(1))), decl)
    }

    @Test
    fun `accepting value narrows as a condition scalar`() {
        fun accepts(scalar: String) =
            sanitizer("- vulnTypes: [sqli]\n  check:\n    argument: argument(0)\n    accepts: $scalar")
                .check
                ?.accepts
        assertEquals(IntVal(0), accepts("0"))
        assertEquals(BoolVal(false), accepts("false"))
        assertEquals(NullVal, accepts("null"))
    }

    @Test
    fun `absent accepting value is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            sanitizer("- vulnTypes: [sqli]\n  check:\n    argument: argument(0)")
        }
    }

    @Test
    fun `non-scalar accepting value is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            sanitizer("- vulnTypes: [sqli]\n  check:\n    argument: argument(0)\n    accepts: [true]")
        }
    }

    @Test
    fun `check stating nothing about the argument is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            sanitizer("- check:\n    argument: argument(0)\n    accepts: true")
        }
        assertFailsWith<IllegalArgumentException> {
            SanitizerDecl(emptySet(), ArgumentCheck(Port.Argument(0), BoolVal(true)))
        }
    }

    @Test
    fun `void type set is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ArgumentCheck(Port.Argument(0), BoolVal(true), types = DeclaredType("void"))
        }
    }

    @Test
    fun `unsure accepting value is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ArgumentCheck(Port.Argument(0), Unsure.ANY)
        }
    }
}
