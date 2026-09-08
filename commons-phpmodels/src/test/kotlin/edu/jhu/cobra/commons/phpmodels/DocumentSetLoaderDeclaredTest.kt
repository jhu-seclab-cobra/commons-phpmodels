package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.VOCABULARY
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.context
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.opener
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.sink
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.sinkCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * The declared load (mapping null) of design-sets.md: vocabulary merges
 * into the context, policy and entries decode against the merge.
 *
 * - `declared set merges vocabulary and decodes policy against the merge` —
 *   the returned vocabulary is the set's own contribution.
 * - `declared set without vocabulary contributes nothing` — empty
 *   vocabulary and policy.
 * - `declared set may reference what it declares` — a document references a
 *   name only the set's own vocabulary declares.
 * - `declared set conflicting redeclaration fails` — a differing description.
 * - `declared set identical redeclaration is admitted` — one declaration.
 * - `declared set policy undeclared reference fails` — policy rows validate
 *   against the merge.
 * - `declared set malformed vocabulary fails`, `declared set malformed policy
 *   fails` — a malformed root file is an [IllegalArgumentException].
 */
internal class DocumentSetLoaderDeclaredTest {
    @Test
    fun `declared set merges vocabulary and decodes policy against the merge`() {
        val set =
            DocumentSetLoader.load(
                opener(
                    "index.txt" to "a.yaml\n",
                    "vocabulary.yaml" to
                        "vulnClasses:\n  - name: xss\n    description: html\nprovenances: []\n",
                    "policy.yaml" to "- origin: user-input\n  enables: [sqli, xss]\n",
                    "a.yaml" to sink("a", "xss"),
                ),
                context,
            )
        val declared = set.vocabulary.vulnClasses.keys
        assertEquals(listOf("xss"), declared.map { it.id })
        assertEquals(setOf(VulnClassId("sqli"), VulnClassId("xss")), set.policy.single().enables)
        val entry = assertIs<ModelEntry>(set.entries.single())
        assertEquals(listOf(VulnClassId("xss")), entry.sinkCategories())
    }

    @Test
    fun `declared set without vocabulary contributes nothing`() {
        val set = DocumentSetLoader.load(opener("index.txt" to "a.yaml\n", "a.yaml" to sink("a", "sqli")), context)
        assertEquals(Vocabulary.EMPTY, set.vocabulary)
        assertEquals(emptyList(), set.policy)
    }

    @Test
    fun `declared set may reference what it declares`() {
        val set =
            DocumentSetLoader.load(
                opener("index.txt" to "a.yaml\n", "vocabulary.yaml" to VOCABULARY, "a.yaml" to sink("a", "sqli")),
            )
        assertEquals(listOf(VulnClassId("sqli")), assertIs<ModelEntry>(set.entries.single()).sinkCategories())
    }

    @Test
    fun `declared set conflicting redeclaration fails`() {
        assertFailsWith<VocabularyException> {
            DocumentSetLoader.load(
                opener(
                    "index.txt" to "",
                    "vocabulary.yaml" to
                        "vulnClasses:\n  - name: sqli\n    description: different\nprovenances: []\n",
                ),
                context,
            )
        }
    }

    @Test
    fun `declared set identical redeclaration is admitted`() {
        val set = DocumentSetLoader.load(opener("index.txt" to "", "vocabulary.yaml" to VOCABULARY), context)
        assertEquals(context, set.vocabulary)
    }

    @Test
    fun `declared set policy undeclared reference fails`() {
        assertFailsWith<VocabularyException> {
            DocumentSetLoader.load(
                opener("index.txt" to "", "policy.yaml" to "- origin: user-input\n  enables: [xss]\n"),
                context,
            )
        }
    }

    @Test
    fun `declared set malformed vocabulary fails`() {
        assertFailsWith<IllegalArgumentException> {
            DocumentSetLoader.load(opener("index.txt" to "", "vocabulary.yaml" to "vulnClasses: []\n"), context)
        }
    }

    @Test
    fun `declared set malformed policy fails`() {
        assertFailsWith<IllegalArgumentException> {
            DocumentSetLoader.load(opener("index.txt" to "", "policy.yaml" to "- origin: user-input\n"), context)
        }
    }
}
