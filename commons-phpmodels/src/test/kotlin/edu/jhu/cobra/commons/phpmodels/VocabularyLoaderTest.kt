package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Decode and interning of the vocabulary document.
 *
 * - `vocabulary decodes both sections` — categories and colors keyed by
 *   interned lowercase identity.
 * - `duplicate entry within a section is rejected` — one of the two
 *   declarations would be lost.
 * - `stray key is rejected` — strict decode.
 * - `require methods validate references` — declared names intern
 *   case-insensitively; undeclared names fail.
 */
internal class VocabularyLoaderTest {
    private val document =
        """
        vulnClasses:
          - name: sqli
            description: SQL injection
        provenances:
          - name: remote
            description: Remote user input
        """.trimIndent()

    private fun load(yaml: String): Vocabulary = VocabularyLoader.load(yaml.byteInputStream())

    @Test
    fun `vocabulary decodes both sections`() {
        val vocabulary = load(document)
        assertEquals(setOf(VulnClassId("sqli")), vocabulary.vulnClasses.keys)
        assertEquals(setOf(ProvenanceId("remote")), vocabulary.provenances.keys)
    }

    @Test
    fun `duplicate entry within a section is rejected`() {
        assertFailsWith<VocabularyException> {
            load(
                """
                vulnClasses:
                  - name: sqli
                    description: first
                  - name: SQLI
                    description: second
                provenances: []
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `stray key is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                vulnClasses:
                  - name: sqli
                    description: SQL injection
                    severity: high
                provenances: []
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `require methods validate references`() {
        val vocabulary = load(document)
        assertEquals(VulnClassId("sqli"), vocabulary.requireVulnClass("SQLI"))
        assertEquals(ProvenanceId("remote"), vocabulary.requireProvenance("remote"))
        assertFailsWith<VocabularyException> { vocabulary.requireVulnClass("xss") }
        assertFailsWith<VocabularyException> { vocabulary.requireProvenance("local") }
    }
}
