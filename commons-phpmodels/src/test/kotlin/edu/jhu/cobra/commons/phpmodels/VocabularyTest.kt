package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Normalization of the interned identity tokens.
 *
 * - `vuln class id folds to lowercase` — any spelling constructs the
 *   lowercased identity, so no reference misses a vocabulary lookup.
 * - `origin id folds to lowercase` — same folding for origin colors.
 */
internal class VocabularyTest {
    @Test
    fun `vuln class id folds to lowercase`() {
        assertEquals("sqli", VulnClassId("SQLI").id)
        assertEquals(VulnClassId("sqli"), VulnClassId("SqLi"))
    }

    @Test
    fun `origin id folds to lowercase`() {
        assertEquals("remote", OriginId("REMOTE").id)
        assertEquals(OriginId("remote"), OriginId("Remote"))
    }
}

/**
 * The accumulation and reference-check contracts of design-sets.md.
 *
 * - `merge …` — union in declaration order; an identical redeclaration is
 *   one declaration, a differing description is a conflict.
 * - `verify …` — every category and color on the three taint sections of an
 *   entry is declared.
 */
internal class VocabularyMergeVerifyTest {
    private fun vocabulary(
        vararg classes: Pair<String, String>,
        provenance: Pair<String, String> = "user-input" to "request data",
    ): Vocabulary {
        val color = OriginId(provenance.first)
        return Vocabulary(
            vulnClasses =
                classes.associate { (name, text) -> VulnClassId(name) to VulnClassDecl(VulnClassId(name), text) },
            origins = mapOf(color to OriginDecl(color, provenance.second)),
        )
    }

    @Test
    fun `merge unions distinct names in declaration order`() {
        val merged = vocabulary("sqli" to "sql").merge(vocabulary("xss" to "html"))
        assertEquals(listOf("sqli", "xss"), merged.vulnClasses.keys.map { it.id })
        assertEquals(listOf("user-input"), merged.origins.keys.map { it.id })
    }

    @Test
    fun `merge admits an identical redeclaration`() {
        val merged = vocabulary("sqli" to "sql").merge(vocabulary("sqli" to "sql"))
        assertEquals(1, merged.vulnClasses.size)
    }

    @Test
    fun `merge rejects a differing description`() {
        val failure =
            assertFailsWith<VocabularyException> {
                vocabulary("sqli" to "sql").merge(vocabulary("sqli" to "database"))
            }
        assertEquals(true, failure.message?.contains("sqli"))
    }

    @Test
    fun `merge rejects a differing provenance description`() {
        assertFailsWith<VocabularyException> {
            vocabulary(provenance = "user-input" to "a").merge(vocabulary(provenance = "user-input" to "b"))
        }
    }

    @Test
    fun `EMPTY declares nothing`() {
        assertEquals(0, Vocabulary.EMPTY.vulnClasses.size + Vocabulary.EMPTY.origins.size)
    }

    @Test
    fun `verify accepts declared references on every section`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: query
                  sources:
                    - provenance: [user-input]
                  sinks:
                    - port: argument(0)
                      category: sqli
                  sanitizers:
                    - categories: [sqli]
                """.trimIndent(),
            )
        vocabulary("sqli" to "sql").verify(entry)
    }

    @Test
    fun `verify rejects an undeclared sink category`() {
        val entry =
            loadModel("- subject:\n    function: query\n  sinks:\n    - port: argument(0)\n      category: xss\n")
        assertFailsWith<VocabularyException> { vocabulary("sqli" to "sql").verify(entry) }
    }

    @Test
    fun `verify rejects an undeclared source color`() {
        val entry = loadModel("- subject:\n    variable: \$_GET\n  sources:\n    - provenance: [remote]\n")
        assertFailsWith<VocabularyException> { vocabulary("sqli" to "sql").verify(entry) }
    }

    @Test
    fun `verify rejects an undeclared sanitizer category`() {
        val entry = loadModel("- subject:\n    function: esc_html\n  sanitizers:\n    - categories: [xss]\n")
        assertFailsWith<VocabularyException> { vocabulary("sqli" to "sql").verify(entry) }
    }
}
