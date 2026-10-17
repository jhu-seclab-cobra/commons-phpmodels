package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Normalization of the interned identity tokens and the reference checks.
 *
 * - `vuln type id folds to lowercase` — any spelling constructs the
 *   lowercased identity, so no reference misses a vocabulary lookup.
 * - `source kind id folds to lowercase` — same folding for source kinds.
 * - `vocabulary exception is an IllegalArgumentException` — one failure
 *   family at the load boundary.
 * - `require methods intern declared references case-insensitively` — a
 *   declared name in any case yields its identity.
 * - `require methods reject undeclared references naming the name` — the
 *   failure names the undeclared reference.
 * - `undeclared reference message carries the declared descriptions` — the
 *   description enriches the undeclared-reference error (design.md
 *   VulnTypeDecl / SourceKindDecl).
 */
internal class VocabularyTest {
    private val vocabulary =
        Vocabulary(
            vulnTypes = mapOf(VulnTypeId("sqli") to VulnTypeDecl(VulnTypeId("sqli"), "SQL injection")),
            sourceKinds = mapOf(SourceKindId("remote") to SourceKindDecl(SourceKindId("remote"), "Remote user input")),
        )

    @Test
    fun `vuln type id folds to lowercase`() {
        assertEquals("sqli", VulnTypeId("SQLI").id)
        assertEquals(VulnTypeId("sqli"), VulnTypeId("SqLi"))
    }

    @Test
    fun `source kind id folds to lowercase`() {
        assertEquals("remote", SourceKindId("REMOTE").id)
        assertEquals(SourceKindId("remote"), SourceKindId("Remote"))
    }

    @Test
    fun `vocabulary exception is an IllegalArgumentException`() {
        assertIs<IllegalArgumentException>(VocabularyException("undeclared"))
    }

    @Test
    fun `require methods intern declared references case-insensitively`() {
        assertEquals(VulnTypeId("sqli"), vocabulary.requireVulnType("SQLI"))
        assertEquals(SourceKindId("remote"), vocabulary.requireSourceKind("Remote"))
    }

    @Test
    fun `require methods reject undeclared references naming the name`() {
        val vulnType = assertFailsWith<VocabularyException> { vocabulary.requireVulnType("xss") }
        assertTrue("xss" in vulnType.message.orEmpty(), vulnType.message)
        val sourceKind = assertFailsWith<VocabularyException> { vocabulary.requireSourceKind("local") }
        assertTrue("local" in sourceKind.message.orEmpty(), sourceKind.message)
    }

    @Test
    fun `undeclared reference message carries the declared descriptions`() {
        val vulnType = assertFailsWith<VocabularyException> { vocabulary.requireVulnType("xss") }
        assertTrue("SQL injection" in vulnType.message.orEmpty(), vulnType.message)
        val sourceKind = assertFailsWith<VocabularyException> { vocabulary.requireSourceKind("local") }
        assertTrue("Remote user input" in sourceKind.message.orEmpty(), sourceKind.message)
    }
}

/**
 * The accumulation and reference-check contracts of design-sets.md.
 *
 * - `merge …` — union in declaration order; an identical redeclaration is
 *   one declaration, a differing description is a conflict naming the name
 *   and both descriptions; `EMPTY` is the identity.
 * - `verify …` — every vulnerability type and source kind on the three taint sections of an
 *   entry is declared; an entry without taint sections passes.
 */
internal class VocabularyMergeVerifyTest {
    private fun vocabulary(
        vararg vulnTypes: Pair<String, String>,
        sourceKind: Pair<String, String> = "user-input" to "request data",
    ): Vocabulary {
        val sourceKindId = SourceKindId(sourceKind.first)
        return Vocabulary(
            vulnTypes =
                vulnTypes.associate { (name, text) -> VulnTypeId(name) to VulnTypeDecl(VulnTypeId(name), text) },
            sourceKinds = mapOf(sourceKindId to SourceKindDecl(sourceKindId, sourceKind.second)),
        )
    }

    @Test
    fun `merge unions distinct names in declaration order`() {
        val merged = vocabulary("sqli" to "sql").merge(vocabulary("xss" to "html"))
        assertEquals(listOf("sqli", "xss"), merged.vulnTypes.keys.map { it.id })
        assertEquals(listOf("user-input"), merged.sourceKinds.keys.map { it.id })
    }

    @Test
    fun `merge admits an identical redeclaration`() {
        val merged = vocabulary("sqli" to "sql").merge(vocabulary("sqli" to "sql"))
        assertEquals(1, merged.vulnTypes.size)
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
    fun `merge rejects a differing source kind description`() {
        assertFailsWith<VocabularyException> {
            vocabulary(sourceKind = "user-input" to "a").merge(vocabulary(sourceKind = "user-input" to "b"))
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
        assertEquals(0, Vocabulary.EMPTY.vulnTypes.size + Vocabulary.EMPTY.sourceKinds.size)
    }

    @Test
    fun `verify accepts declared references on every section`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: query
                  sources:
                    - sourceKinds: [user-input]
                  sinks:
                    - port: argument(0)
                      vulnType: sqli
                  sanitizers:
                    - vulnTypes: [sqli]
                """.trimIndent(),
            )
        vocabulary("sqli" to "sql").verify(entry)
    }

    @Test
    fun `verify accepts an entry without taint sections`() {
        Vocabulary.EMPTY.verify(loadModel("- subject:\n    function: strlen\n  returns: num\n"))
    }

    @Test
    fun `verify rejects an undeclared sink vulnerability type`() {
        val entry =
            loadModel("- subject:\n    function: query\n  sinks:\n    - port: argument(0)\n      vulnType: xss\n")
        assertFailsWith<VocabularyException> { vocabulary("sqli" to "sql").verify(entry) }
    }

    @Test
    fun `verify rejects an undeclared source kind`() {
        val entry = loadModel("- subject:\n    variable: \$_GET\n  sources:\n    - sourceKinds: [remote]\n")
        assertFailsWith<VocabularyException> { vocabulary("sqli" to "sql").verify(entry) }
    }

    @Test
    fun `verify rejects an undeclared sanitizer vulnerability type`() {
        val entry = loadModel("- subject:\n    function: esc_html\n  sanitizers:\n    - vulnTypes: [xss]\n")
        assertFailsWith<VocabularyException> { vocabulary("sqli" to "sql").verify(entry) }
    }
}
