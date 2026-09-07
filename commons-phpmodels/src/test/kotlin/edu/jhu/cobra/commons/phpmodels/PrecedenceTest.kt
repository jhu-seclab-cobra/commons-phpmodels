package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The consumer's order over verification kinds per design-sets.md.
 *
 * - `default ranks manual above generated` — `DEFAULT` order and comparator sign.
 * - `reversed order ranks generated above manual` — the order is the consumer's.
 * - `equal kinds compare zero` — a tie is left to the consumer.
 * - `rejects …` — a missing or repeated kind fails construction.
 */
internal class PrecedenceTest {
    @Test
    fun `default ranks manual above generated`() {
        assertEquals(0, Precedence.DEFAULT.rank(Verification.MANUAL))
        assertEquals(1, Precedence.DEFAULT.rank(Verification.GENERATED))
        assertTrue(Precedence.DEFAULT.compare(Verification.MANUAL, Verification.GENERATED) > 0)
        assertTrue(Precedence.DEFAULT.compare(Verification.GENERATED, Verification.MANUAL) < 0)
    }

    @Test
    fun `reversed order ranks generated above manual`() {
        val reversed = Precedence(listOf(Verification.GENERATED, Verification.MANUAL))
        assertTrue(reversed.compare(Verification.GENERATED, Verification.MANUAL) > 0)
    }

    @Test
    fun `equal kinds compare zero`() {
        assertEquals(0, Precedence.DEFAULT.compare(Verification.MANUAL, Verification.MANUAL))
    }

    @Test
    fun `rejects a missing kind`() {
        assertFailsWith<IllegalArgumentException> { Precedence(listOf(Verification.MANUAL)) }
    }

    @Test
    fun `rejects a repeated kind`() {
        assertFailsWith<IllegalArgumentException> {
            Precedence(listOf(Verification.MANUAL, Verification.GENERATED, Verification.MANUAL))
        }
    }
}
