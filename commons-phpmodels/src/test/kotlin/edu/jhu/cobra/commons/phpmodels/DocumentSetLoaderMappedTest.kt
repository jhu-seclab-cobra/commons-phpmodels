package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.context
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.mapping
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.opener
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.sink
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.sinkCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The mapped load (mapping non-null) of design-sets.md: the set's own
 * vocabulary is ignored, names are translated into the context's.
 *
 * - `mapped set translates entries and rows and ignores its vocabulary` —
 *   emptied entries drop, the returned vocabulary is empty.
 * - `mapped set undeclared target fails` — every target is declared in the
 *   context; the failure names the target.
 * - `mapped set unlisted name fails` — in an entry and in a policy row; the
 *   failure names the name.
 * - `mapped set discarded policy origin drops the row` — a row whose origin
 *   maps to null.
 */
internal class DocumentSetLoaderMappedTest {
    @Test
    fun `mapped set translates entries and rows and ignores its vocabulary`() {
        val set =
            DocumentSetLoader.load(
                opener(
                    "index.txt" to "a.yaml\n",
                    "vocabulary.yaml" to
                        "vulnClasses:\n  - name: sqli\n    description: conflicting\nprovenances: []\n",
                    "policy.yaml" to "- origin: input\n  enables: [sql, text]\n",
                    "a.yaml" to sink("a", "sql") + sink("b", "text"),
                ),
                context,
                mapping,
            )
        assertEquals(Vocabulary.EMPTY, set.vocabulary)
        assertEquals(listOf(PolicyRow(OriginId("user-input"), setOf(VulnClassId("sqli")))), set.policy)
        val entry = assertIs<ModelEntry>(set.entries.single())
        assertEquals("a", (entry.subject as FunctionSubject).name)
        assertEquals(listOf(VulnClassId("sqli")), entry.sinkCategories())
    }

    @Test
    fun `mapped set undeclared target fails`() {
        val stray = CategoryMapping(mapOf(VulnClassId("sql") to VulnClassId("xss")), emptyMap())
        val failure =
            assertFailsWith<VocabularyException> {
                DocumentSetLoader.load(opener("index.txt" to ""), context, stray)
            }
        assertTrue("xss" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `mapped set unlisted name fails`() {
        val failure =
            assertFailsWith<VocabularyException> {
                DocumentSetLoader.load(
                    opener("index.txt" to "a.yaml\n", "a.yaml" to sink("a", "shell")),
                    context,
                    mapping,
                )
            }
        assertTrue("shell" in failure.message.orEmpty(), failure.message)
        assertFailsWith<VocabularyException> {
            DocumentSetLoader.load(
                opener("index.txt" to "", "policy.yaml" to "- origin: remote\n  enables: [sql]\n"),
                context,
                mapping,
            )
        }
    }

    @Test
    fun `mapped set discarded policy origin drops the row`() {
        val discarding =
            CategoryMapping(mapOf(VulnClassId("sql") to VulnClassId("sqli")), mapOf(OriginId("env") to null))
        val set =
            DocumentSetLoader.load(
                opener("index.txt" to "", "policy.yaml" to "- origin: env\n  enables: [sql]\n"),
                context,
                discarding,
            )
        assertEquals(emptyList(), set.policy)
    }
}
