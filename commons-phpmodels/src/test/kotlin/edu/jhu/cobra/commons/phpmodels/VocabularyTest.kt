package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Normalization of the interned identity tokens.
 *
 * - `vuln class id folds to lowercase` — any spelling constructs the
 *   lowercased identity, so no reference misses a vocabulary lookup.
 * - `provenance id folds to lowercase` — same folding for origin colors.
 */
internal class VocabularyTest {
    @Test
    fun `vuln class id folds to lowercase`() {
        assertEquals("sqli", VulnClassId("SQLI").id)
        assertEquals(VulnClassId("sqli"), VulnClassId("SqLi"))
    }

    @Test
    fun `provenance id folds to lowercase`() {
        assertEquals("remote", ProvenanceId("REMOTE").id)
        assertEquals(ProvenanceId("remote"), ProvenanceId("Remote"))
    }
}
