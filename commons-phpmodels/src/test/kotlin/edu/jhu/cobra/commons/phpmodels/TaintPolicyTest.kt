package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The origin→categories matrix of design.md "Policy".
 *
 * - `row enabling no category is rejected` — an empty enables set asserts
 *   nothing.
 * - `row keeps its origin and enabled set` — one row is one statement.
 * - `enabled pair is dangerous` — the row's origin reaching a sink of an
 *   enabled category is a vulnerability.
 * - `unenabled category is not dangerous` — a declared origin reaching a
 *   category outside its set.
 * - `unknown origin is not dangerous` — a color without a row enables nothing.
 * - `rows sharing an origin accumulate by union` — the enabled sets union.
 * - `empty policy is never dangerous` — no row, no vulnerability.
 */
internal class TaintPolicyTest {
    private val remote = OriginId("remote")
    private val sqli = VulnClassId("sqli")
    private val xss = VulnClassId("xss")

    @Test
    fun `row enabling no category is rejected`() {
        assertFailsWith<IllegalArgumentException> { PolicyRow(remote, emptySet()) }
    }

    @Test
    fun `row keeps its origin and enabled set`() {
        val row = PolicyRow(remote, setOf(sqli, xss))
        assertEquals(remote, row.origin)
        assertEquals(setOf(sqli, xss), row.enables)
    }

    @Test
    fun `enabled pair is dangerous`() {
        assertEquals(true, TaintPolicy(listOf(PolicyRow(remote, setOf(sqli)))).isDangerous(remote, sqli))
    }

    @Test
    fun `unenabled category is not dangerous`() {
        assertEquals(false, TaintPolicy(listOf(PolicyRow(remote, setOf(sqli)))).isDangerous(remote, xss))
    }

    @Test
    fun `unknown origin is not dangerous`() {
        assertEquals(false, TaintPolicy(listOf(PolicyRow(remote, setOf(sqli)))).isDangerous(OriginId("local"), sqli))
    }

    @Test
    fun `rows sharing an origin accumulate by union`() {
        val policy = TaintPolicy(listOf(PolicyRow(remote, setOf(sqli)), PolicyRow(remote, setOf(xss))))
        assertEquals(true, policy.isDangerous(remote, sqli))
        assertEquals(true, policy.isDangerous(remote, xss))
    }

    @Test
    fun `empty policy is never dangerous`() {
        assertEquals(false, TaintPolicy(emptyList()).isDangerous(remote, sqli))
    }
}
