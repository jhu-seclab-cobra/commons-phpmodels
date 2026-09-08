package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The provenance declaration and the consumer's order over verification
 * kinds per design-sets.md.
 *
 * - `verification has the two declared kinds` — `generated` and `manual`
 *   are the closed set.
 * - `set provenance rejects a blank producer` — the producer names the
 *   generator or reviewer; blank is undeclared.
 * - `set provenance keeps producer and verification` — both fields are
 *   carried as declared.
 * - `default ranks manual above generated` — `DEFAULT` order and comparator sign.
 * - `reversed order ranks generated above manual` — the order is the consumer's.
 * - `rank follows the given order` — rank is the position in the order list.
 * - `equal kinds compare zero` — a tie is left to the consumer.
 * - `rejects …` — a missing or repeated kind fails construction.
 */
internal class ProvenanceTest {
    @Test
    fun `verification has the two declared kinds`() {
        assertEquals(setOf(Verification.GENERATED, Verification.MANUAL), Verification.entries.toSet())
    }

    @Test
    fun `set provenance rejects a blank producer`() {
        assertFailsWith<IllegalArgumentException> { SetProvenance(" ", Verification.MANUAL) }
        assertFailsWith<IllegalArgumentException> { SetProvenance("", Verification.GENERATED) }
    }

    @Test
    fun `set provenance keeps producer and verification`() {
        val provenance = SetProvenance("phpstorm-stubs 2024.3", Verification.GENERATED)
        assertEquals("phpstorm-stubs 2024.3", provenance.producer)
        assertEquals(Verification.GENERATED, provenance.verification)
    }

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
    fun `rank follows the given order`() {
        val reversed = Precedence(listOf(Verification.GENERATED, Verification.MANUAL))
        assertEquals(0, reversed.rank(Verification.GENERATED))
        assertEquals(1, reversed.rank(Verification.MANUAL))
    }

    @Test
    fun `equal kinds compare zero`() {
        assertEquals(0, Precedence.DEFAULT.compare(Verification.MANUAL, Verification.MANUAL))
    }

    @Test
    fun `rejects a missing kind`() {
        assertFailsWith<IllegalArgumentException> { Precedence(listOf(Verification.MANUAL)) }
        assertFailsWith<IllegalArgumentException> { Precedence(emptyList()) }
    }

    @Test
    fun `rejects a repeated kind`() {
        assertFailsWith<IllegalArgumentException> {
            Precedence(listOf(Verification.MANUAL, Verification.GENERATED, Verification.MANUAL))
        }
    }
}
