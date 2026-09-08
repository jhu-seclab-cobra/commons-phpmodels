package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.value.BoolVal
import edu.jhu.cobra.commons.value.FloatVal
import edu.jhu.cobra.commons.value.IPrimitiveVal
import edu.jhu.cobra.commons.value.IntVal
import edu.jhu.cobra.commons.value.NullVal
import edu.jhu.cobra.commons.value.StrVal
import edu.jhu.cobra.commons.value.Unsure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Decode and match contract of the `when:` condition (design-conditions.md).
 *
 * - `scalar shapes narrow by yaml shape` — `_`, boolean, integer, float,
 *   null, and text elements narrow to the wildcard and the commons-value
 *   primitives; the YAML 1.1 readings (`no`, `017`, `0x1F`) are pinned.
 * - `quoted scalar keeps the string shape` — quoting forces `StrVal`.
 * - `quoted underscore is still the wildcard` — the text `_` is never a
 *   condition.
 * - `integer beyond Long range reads as a float` — the compared value never
 *   truncates silently; it widens to the float shape.
 * - `non-finite float spelling is rejected` — `.inf` is not a comparable
 *   literal.
 * - `empty pattern is rejected`, `all-wildcard pattern is rejected`,
 *   `non-scalar element is rejected`, `unsure element is rejected` — the
 *   construction rules.
 * - `mapping condition is rejected` — the retired `port`/`is` form fails.
 * - `positions lists every non-wildcard position` — the derived view.
 * - `patterns compare by their expected list` — equality and hash code.
 * - `matches …` — holds, fails, and undecidable outcomes, including a
 *   failing position deciding over an unknown one, a pattern longer than
 *   the call, cross-type inequality, and `NullVal` as a known argument.
 */
internal class ArgPatternTest {
    private fun condition(yaml: String): ArgPattern? =
        loadModel("- subject:\n    function: f\n  when: $yaml\n  returns: any\n").condition

    @Test
    fun `scalar shapes narrow by yaml shape`() {
        assertEquals(
            ArgPattern(
                listOf(
                    null,
                    BoolVal(true),
                    BoolVal(false),
                    IntVal(1),
                    IntVal(15),
                    IntVal(31),
                    FloatVal(1.5),
                    NullVal,
                    NullVal,
                    StrVal("x"),
                ),
            ),
            condition("[_, true, no, 1, 017, 0x1F, 1.5, null, ~, x]"),
        )
    }

    @Test
    fun `quoted scalar keeps the string shape`() {
        assertEquals(ArgPattern(listOf(StrVal("true"), StrVal("1"))), condition("[\"true\", \"1\"]"))
    }

    @Test
    fun `quoted underscore is still the wildcard`() {
        assertEquals(ArgPattern(listOf(null, IntVal(1))), condition("[\"_\", 1]"))
    }

    @Test
    fun `integer beyond Long range reads as a float`() {
        assertEquals(ArgPattern(listOf(FloatVal(1.0e26))), condition("[99999999999999999999999999]"))
    }

    @Test
    fun `non-finite float spelling is rejected`() {
        assertFailsWith<IllegalArgumentException> { condition("[.inf]") }
    }

    @Test
    fun `empty pattern is rejected`() {
        assertFailsWith<IllegalArgumentException> { condition("[]") }
        assertFailsWith<IllegalArgumentException> { ArgPattern(emptyList()) }
    }

    @Test
    fun `all-wildcard pattern is rejected`() {
        assertFailsWith<IllegalArgumentException> { condition("[_, _]") }
        assertFailsWith<IllegalArgumentException> { ArgPattern(listOf(null)) }
    }

    @Test
    fun `non-scalar element is rejected`() {
        assertFailsWith<IllegalArgumentException> { condition("[[1, 2]]") }
        assertFailsWith<IllegalArgumentException> { condition("[{a: 1}]") }
    }

    @Test
    fun `unsure element is rejected`() {
        assertFailsWith<IllegalArgumentException> { ArgPattern(listOf(Unsure.ANY)) }
    }

    @Test
    fun `mapping condition is rejected`() {
        assertFailsWith<IllegalArgumentException> { condition("{port: argument(0), is: true}") }
    }

    @Test
    fun `positions lists every non-wildcard position`() {
        assertEquals(listOf(0, 2), ArgPattern(listOf(IntVal(1), null, StrVal("x"))).positions)
        assertEquals(listOf(1), ArgPattern(listOf(null, IntVal(257))).positions)
    }

    @Test
    fun `patterns compare by their expected list`() {
        assertEquals(ArgPattern(listOf(null, IntVal(1))), ArgPattern(listOf(null, IntVal(1))))
        assertEquals(ArgPattern(listOf(null, IntVal(1))).hashCode(), ArgPattern(listOf(null, IntVal(1))).hashCode())
        assertNotEquals(ArgPattern(listOf(null, IntVal(1))), ArgPattern(listOf(IntVal(1))))
    }

    @Test
    fun `matches holds when every listed position is equal`() {
        val pattern = ArgPattern(listOf(null, IntVal(257)))
        assertEquals(true, pattern.matches(listOf(StrVal("v"), IntVal(257))))
        assertEquals(true, pattern.matches(listOf(null, IntVal(257), Unsure.ANY)))
    }

    @Test
    fun `matches treats NullVal as a known argument`() {
        assertEquals(true, ArgPattern(listOf(NullVal)).matches(listOf<IPrimitiveVal?>(NullVal)))
        assertEquals(false, ArgPattern(listOf(NullVal)).matches(listOf<IPrimitiveVal?>(IntVal(0))))
    }

    @Test
    fun `matches fails on a known unequal argument or a short call`() {
        val pattern = ArgPattern(listOf(null, IntVal(257)))
        assertEquals(false, pattern.matches(listOf(StrVal("v"), IntVal(258))))
        assertEquals(false, pattern.matches(listOf(StrVal("v"))))
        assertEquals(false, ArgPattern(listOf(IntVal(1))).matches(listOf<IPrimitiveVal?>(FloatVal(1.0))))
        assertEquals(false, ArgPattern(listOf(BoolVal(true))).matches(listOf<IPrimitiveVal?>(StrVal("true"))))
    }

    @Test
    fun `matches is undecidable on an unknown argument`() {
        val pattern = ArgPattern(listOf(BoolVal(true), IntVal(2)))
        assertEquals(null, pattern.matches(listOf(null, IntVal(2))))
        assertEquals(null, pattern.matches(listOf(Unsure.ANY, IntVal(2))))
    }

    @Test
    fun `failing position decides over an unknown one`() {
        val pattern = ArgPattern(listOf(BoolVal(true), IntVal(2)))
        assertEquals(false, pattern.matches(listOf(null, IntVal(3))))
    }
}
