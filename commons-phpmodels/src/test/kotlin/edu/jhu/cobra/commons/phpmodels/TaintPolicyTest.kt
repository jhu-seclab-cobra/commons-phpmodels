package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The source kind → vulnerability types matrix of design.md "Policy".
 *
 * - `row enabling no vulnerability type is rejected` — an empty enables set asserts
 *   nothing.
 * - `row keeps its source kind and enabled set` — one row is one statement.
 * - `enabled pair is dangerous` — the row's source kind reaching a sink of an
 *   enabled vulnerability type is a vulnerability.
 * - `unenabled vulnerability type is not dangerous` — a declared source kind
 *   reaching a vulnerability type outside its set.
 * - `unknown source kind is not dangerous` — a source kind without a row
 *   enables nothing.
 * - `rows sharing a source kind accumulate by union` — the enabled sets union.
 * - `empty policy is never dangerous` — no row, no vulnerability.
 */
internal class TaintPolicyTest {
    private val remote = SourceKindId("remote")
    private val sqli = VulnTypeId("sqli")
    private val xss = VulnTypeId("xss")

    @Test
    fun `row enabling no vulnerability type is rejected`() {
        assertFailsWith<IllegalArgumentException> { PolicyRow(remote, emptySet()) }
    }

    @Test
    fun `row keeps its source kind and enabled set`() {
        val row = PolicyRow(remote, setOf(sqli, xss))
        assertEquals(remote, row.sourceKind)
        assertEquals(setOf(sqli, xss), row.enables)
    }

    @Test
    fun `enabled pair is dangerous`() {
        assertEquals(true, TaintPolicy(listOf(PolicyRow(remote, setOf(sqli)))).isDangerous(remote, sqli))
    }

    @Test
    fun `unenabled vulnerability type is not dangerous`() {
        assertEquals(false, TaintPolicy(listOf(PolicyRow(remote, setOf(sqli)))).isDangerous(remote, xss))
    }

    @Test
    fun `unknown source kind is not dangerous`() {
        val policy = TaintPolicy(listOf(PolicyRow(remote, setOf(sqli))))
        assertEquals(false, policy.isDangerous(SourceKindId("local"), sqli))
    }

    @Test
    fun `rows sharing a source kind accumulate by union`() {
        val policy = TaintPolicy(listOf(PolicyRow(remote, setOf(sqli)), PolicyRow(remote, setOf(xss))))
        assertEquals(true, policy.isDangerous(remote, sqli))
        assertEquals(true, policy.isDangerous(remote, xss))
    }

    @Test
    fun `empty policy is never dangerous`() {
        assertEquals(false, TaintPolicy(emptyList()).isDangerous(remote, sqli))
    }
}
