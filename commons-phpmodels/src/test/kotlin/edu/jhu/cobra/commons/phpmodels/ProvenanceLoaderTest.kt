package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Decode of one provenance document per design-sets.md.
 *
 * - `decodes producer and verification` — both kinds decode from their lowercase spellings.
 * - `rejects …` — a stray key, a missing field, a blank producer, and an unknown kind fail.
 */
internal class ProvenanceLoaderTest {
    private fun load(text: String): SetProvenance = ProvenanceLoader.load(text.byteInputStream())

    @Test
    fun `decodes producer and verification`() {
        assertEquals(
            SetProvenance("phpstorm-stubs 2024.3", Verification.GENERATED),
            load("producer: phpstorm-stubs 2024.3\nverification: generated\n"),
        )
        assertEquals(SetProvenance("review", Verification.MANUAL), load("producer: review\nverification: manual\n"))
    }

    @Test
    fun `rejects a stray key`() {
        assertFailsWith<IllegalArgumentException> { load("producer: x\nverification: manual\nnote: y\n") }
    }

    @Test
    fun `rejects a missing field`() {
        assertFailsWith<IllegalArgumentException> { load("producer: x\n") }
        assertFailsWith<IllegalArgumentException> { load("verification: manual\n") }
    }

    @Test
    fun `rejects a blank producer`() {
        assertFailsWith<IllegalArgumentException> { load("producer: ' '\nverification: manual\n") }
    }

    @Test
    fun `rejects an unknown verification kind`() {
        assertFailsWith<IllegalArgumentException> { load("producer: x\nverification: reviewed\n") }
    }
}
