package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Normalization of the interned identity tokens and the reference checks.
 *
 * - `vuln class id folds to lowercase` — any spelling constructs the
 *   lowercased identity, so no reference misses a vocabulary lookup.
 * - `origin id folds to lowercase` — same folding for origin colors.
 * - `vocabulary exception is an IllegalArgumentException` — one failure
 *   family at the load boundary.
 * - `require methods intern declared references case-insensitively` — a
 *   declared name in any case yields its identity.
 * - `require methods reject undeclared references naming the name` — the
 *   failure names the undeclared reference.
 * - `undeclared reference message carries the declared descriptions` — the
 *   description enriches the undeclared-reference error (design.md
 *   VulnClassDecl / OriginDecl).
 */
internal class VocabularyTest {
    private val vocabulary =
        Vocabulary(
            vulnClasses = mapOf(VulnClassId("sqli") to VulnClassDecl(VulnClassId("sqli"), "SQL injection")),
            origins = mapOf(OriginId("remote") to OriginDecl(OriginId("remote"), "Remote user input")),
        )

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

    @Test
    fun `vocabulary exception is an IllegalArgumentException`() {
        assertIs<IllegalArgumentException>(VocabularyException("undeclared"))
    }

    @Test
    fun `require methods intern declared references case-insensitively`() {
        assertEquals(VulnClassId("sqli"), vocabulary.requireVulnClass("SQLI"))
        assertEquals(OriginId("remote"), vocabulary.requireOrigin("Remote"))
    }

    @Test
    fun `require methods reject undeclared references naming the name`() {
        val category = assertFailsWith<VocabularyException> { vocabulary.requireVulnClass("xss") }
        assertTrue("xss" in category.message.orEmpty(), category.message)
        val origin = assertFailsWith<VocabularyException> { vocabulary.requireOrigin("local") }
        assertTrue("local" in origin.message.orEmpty(), origin.message)
    }

    @Test
    fun `undeclared reference message carries the declared descriptions`() {
        val category = assertFailsWith<VocabularyException> { vocabulary.requireVulnClass("xss") }
        assertTrue("SQL injection" in category.message.orEmpty(), category.message)
        val origin = assertFailsWith<VocabularyException> { vocabulary.requireOrigin("local") }
        assertTrue("Remote user input" in origin.message.orEmpty(), origin.message)
    }
}

/**
 * The accumulation and reference-check contracts of design-sets.md.
 *
 * - `merge …` — union in declaration order; an identical redeclaration is
 *   one declaration, a differing description is a conflict naming the name
 *   and both descriptions; `EMPTY` is the identity.
 * - `verify …` — every category and color on the three taint sections of an
 *   entry is declared; an entry without taint sections passes.
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
    fun `merge rejects a differing description naming both`() {
        val failure =
            assertFailsWith<VocabularyException> {
                vocabulary("sqli" to "sql").merge(vocabulary("sqli" to "database"))
            }
        val message = failure.message.orEmpty()
        assertTrue("sqli" in message, message)
        assertTrue("sql" in message && "database" in message, message)
    }

    @Test
    fun `merge rejects a differing provenance description`() {
        assertFailsWith<VocabularyException> {
            vocabulary(provenance = "user-input" to "a").merge(vocabulary(provenance = "user-input" to "b"))
        }
    }

    @Test
    fun `merge with EMPTY is the identity`() {
        val declared = vocabulary("sqli" to "sql")
        assertEquals(declared, Vocabulary.EMPTY.merge(declared))
        assertEquals(declared, declared.merge(Vocabulary.EMPTY))
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
    fun `verify accepts an entry without taint sections`() {
        Vocabulary.EMPTY.verify(loadModel("- subject:\n    function: strlen\n  returns: num\n"))
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
