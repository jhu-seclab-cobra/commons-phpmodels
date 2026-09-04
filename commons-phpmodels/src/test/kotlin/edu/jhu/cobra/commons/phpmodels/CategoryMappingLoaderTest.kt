package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Decoding of the mapping document shape (design-sets.md, impl.md).
 *
 * - `decodes both axes with ignore as discard` — the literal maps to null.
 * - `rejects …` — null target, missing section, stray key, and a source
 *   spelled `ignore` fail the decode.
 */
internal class CategoryMappingLoaderTest {
    private fun load(yaml: String): CategoryMapping = CategoryMappingLoader.load(yaml.byteInputStream())

    @Test
    fun `decodes both axes with ignore as discard`() {
        val mapping =
            load(
                """
                categories:
                  sql: sqli
                  text: ignore
                provenances:
                  input: user-input
                """.trimIndent(),
            )
        assertEquals(VulnClassId("sqli"), mapping.category(VulnClassId("sql")))
        assertNull(mapping.category(VulnClassId("text")))
        assertEquals(ProvenanceId("user-input"), mapping.provenance(ProvenanceId("input")))
    }

    @Test
    fun `folds names to lowercase`() {
        val mapping = load("categories:\n  SQL: SQLi\nprovenances: {}\n")
        assertEquals(VulnClassId("sqli"), mapping.category(VulnClassId("sql")))
    }

    @Test
    fun `rejects a null target`() {
        assertFailsWith<IllegalArgumentException> { load("categories:\n  sql:\nprovenances: {}\n") }
    }

    @Test
    fun `rejects a missing section`() {
        assertFailsWith<IllegalArgumentException> { load("categories:\n  sql: sqli\n") }
    }

    @Test
    fun `rejects a stray key`() {
        assertFailsWith<IllegalArgumentException> { load("categories: {}\nprovenances: {}\ncolors: {}\n") }
    }

    @Test
    fun `rejects ignore as a source name`() {
        assertFailsWith<IllegalArgumentException> { load("categories:\n  ignore: sqli\nprovenances: {}\n") }
    }
}
