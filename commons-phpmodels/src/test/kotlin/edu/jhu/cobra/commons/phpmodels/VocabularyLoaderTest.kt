package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Decode and interning of the vocabulary document.
 *
 * - `vocabulary decodes both sections` — vulnerability types and source kinds keyed by
 *   interned lowercase identity.
 * - `duplicate entry within a section is rejected` — one of the two
 *   declarations would be lost.
 * - `one name may appear in both sections` — uniqueness is per section, not
 *   per document.
 * - `stray key is rejected` — strict decode.
 * - `missing section is rejected` — both sections are required.
 * - `descriptions are kept` — the human text travels with the declaration.
 * - `empty sections decode to the empty vocabulary` — `[]` declares nothing.
 * - `require methods validate references` — declared names intern
 *   case-insensitively; undeclared names fail.
 */
internal class VocabularyLoaderTest {
    private val document =
        """
        vulnTypes:
          - name: sqli
            description: SQL injection
        sourceKinds:
          - name: remote
            description: Remote user input
        """.trimIndent()

    private fun load(yaml: String): Vocabulary = VocabularyLoader.load(yaml.byteInputStream())

    @Test
    fun `vocabulary decodes both sections`() {
        val vocabulary = load(document)
        assertEquals(setOf(VulnTypeId("sqli")), vocabulary.vulnTypes.keys)
        assertEquals(setOf(SourceKindId("remote")), vocabulary.sourceKinds.keys)
    }

    @Test
    fun `duplicate entry within a section is rejected`() {
        assertFailsWith<VocabularyException> {
            load(
                """
                vulnTypes:
                  - name: sqli
                    description: first
                  - name: SQLI
                    description: second
                sourceKinds: []
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `one name may appear in both sections`() {
        val vocabulary =
            load(
                """
                vulnTypes:
                  - name: header
                    description: Header injection
                sourceKinds:
                  - name: header
                    description: Header-borne input
                """.trimIndent(),
            )
        assertEquals(setOf(VulnTypeId("header")), vocabulary.vulnTypes.keys)
        assertEquals(setOf(SourceKindId("header")), vocabulary.sourceKinds.keys)
    }

    @Test
    fun `stray key is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                vulnTypes:
                  - name: sqli
                    description: SQL injection
                    severity: high
                sourceKinds: []
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `require methods validate references`() {
        val vocabulary = load(document)
        assertEquals(VulnTypeId("sqli"), vocabulary.requireVulnType("SQLI"))
        assertEquals(SourceKindId("remote"), vocabulary.requireSourceKind("remote"))
        assertFailsWith<VocabularyException> { vocabulary.requireVulnType("xss") }
        assertFailsWith<VocabularyException> { vocabulary.requireSourceKind("local") }
    }

    @Test
    fun `missing section is rejected`() {
        assertFailsWith<IllegalArgumentException> { load("vulnTypes: []\n") }
        assertFailsWith<IllegalArgumentException> { load("sourceKinds: []\n") }
    }

    @Test
    fun `descriptions are kept`() {
        val vocabulary = load(document)
        assertEquals("SQL injection", vocabulary.vulnTypes.getValue(VulnTypeId("sqli")).description)
        assertEquals("Remote user input", vocabulary.sourceKinds.getValue(SourceKindId("remote")).description)
    }

    @Test
    fun `empty sections decode to the empty vocabulary`() {
        assertEquals(Vocabulary.EMPTY, load("vulnTypes: []\nsourceKinds: []\n"))
    }
}
