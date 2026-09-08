package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.PROVENANCE
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.VOCABULARY
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.context
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.mapping
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.opener
import edu.jhu.cobra.commons.phpmodels.DocumentSetFixtures.sink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The set-level load of design-sets.md over an in-memory [ResourceOpener]:
 * manifest, listed documents, provenance, and stream handling. Vocabulary
 * accumulation: [DocumentSetLoaderDeclaredTest]; translation:
 * [DocumentSetLoaderMappedTest].
 *
 * - `file names are the fixed constants` — the four root file names.
 * - `manifest …` — absent manifest, absent document, and doubled line fail;
 *   comment and blank lines are not entries; order is manifest order.
 * - `listed document …` — a malformed document and an undeclared reference
 *   both name the document; the decode failure stays as the cause.
 * - `loading twice yields equal sets` — the load is a pure function of the
 *   storage.
 * - `provenance …` — `provenance.yaml` attaches to a declared and a mapped
 *   load, is null when absent, and fails the load when malformed.
 * - `closes every stream` — the opener's streams are released on success.
 * - `closes the stream of a malformed document` — and on failure.
 */
internal class DocumentSetLoaderTest {
    @Test
    fun `file names are the fixed constants`() {
        assertEquals("index.txt", DocumentSetLoader.MANIFEST)
        assertEquals("vocabulary.yaml", DocumentSetLoader.VOCABULARY)
        assertEquals("policy.yaml", DocumentSetLoader.POLICY)
        assertEquals("provenance.yaml", DocumentSetLoader.PROVENANCE)
    }

    @Test
    fun `manifest absent fails`() {
        val failure = assertFailsWith<DocumentSetException> { DocumentSetLoader.load(opener()) }
        assertEquals(DocumentSetLoader.MANIFEST, failure.path)
    }

    @Test
    fun `manifest skips comments and blanks and keeps order`() {
        val set =
            DocumentSetLoader.load(
                opener(
                    "index.txt" to "# header\n\n  b.yaml \na.yaml\n",
                    "b.yaml" to sink("b", "sqli"),
                    "a.yaml" to sink("a", "sqli"),
                ),
                context,
            )
        assertEquals(listOf("b.yaml", "a.yaml"), set.documents.map { it.path })
        assertEquals(2, set.entries.size)
    }

    @Test
    fun `manifest doubled line fails`() {
        val failure =
            assertFailsWith<DocumentSetException> {
                val files = opener("index.txt" to "a.yaml\na.yaml\n", "a.yaml" to sink("a", "sqli"))
                DocumentSetLoader.load(files, context)
            }
        assertEquals("a.yaml", failure.path)
    }

    @Test
    fun `listed document absent fails`() {
        val failure =
            assertFailsWith<DocumentSetException> {
                DocumentSetLoader.load(opener("index.txt" to "missing.yaml\n"), context)
            }
        assertEquals("missing.yaml", failure.path)
    }

    @Test
    fun `listed document malformed names the document and keeps the cause`() {
        val failure =
            assertFailsWith<DocumentSetException> {
                DocumentSetLoader.load(opener("index.txt" to "bad.yaml\n", "bad.yaml" to "- subject:\n    trait: x\n"))
            }
        assertEquals("bad.yaml", failure.path)
        assertIs<IllegalArgumentException>(failure.cause)
    }

    @Test
    fun `listed document undeclared reference names the document`() {
        val failure =
            assertFailsWith<VocabularyException> {
                DocumentSetLoader.load(opener("index.txt" to "a.yaml\n", "a.yaml" to sink("a", "xss")), context)
            }
        assertTrue("'a.yaml'" in failure.message.orEmpty(), "expected the document path, was: ${failure.message}")
    }

    @Test
    fun `loading twice yields equal sets`() {
        val files =
            arrayOf(
                "index.txt" to "a.yaml\n",
                "vocabulary.yaml" to "vulnClasses:\n  - name: xss\n    description: html\nprovenances: []\n",
                "policy.yaml" to "- origin: user-input\n  enables: [sqli, xss]\n",
                "provenance.yaml" to PROVENANCE,
                "a.yaml" to sink("a", "xss"),
            )
        assertEquals(DocumentSetLoader.load(opener(*files), context), DocumentSetLoader.load(opener(*files), context))
    }

    @Test
    fun `provenance attaches to a declared load`() {
        val set = DocumentSetLoader.load(opener("index.txt" to "", "provenance.yaml" to PROVENANCE), context)
        assertEquals(SetProvenance("review", Verification.MANUAL), set.provenance)
    }

    @Test
    fun `provenance attaches to a mapped load`() {
        val set = DocumentSetLoader.load(opener("index.txt" to "", "provenance.yaml" to PROVENANCE), context, mapping)
        assertEquals(SetProvenance("review", Verification.MANUAL), set.provenance)
    }

    @Test
    fun `provenance is null when absent`() {
        assertEquals(null, DocumentSetLoader.load(opener("index.txt" to ""), context).provenance)
    }

    @Test
    fun `provenance malformed fails`() {
        val files = opener("index.txt" to "", "provenance.yaml" to "producer: x\nverification: reviewed\n")
        assertFailsWith<IllegalArgumentException> { DocumentSetLoader.load(files) }
    }

    @Test
    fun `closes every stream`() {
        val opener =
            opener(
                "index.txt" to "a.yaml\n",
                "provenance.yaml" to PROVENANCE,
                "vocabulary.yaml" to VOCABULARY,
                "policy.yaml" to "- origin: user-input\n  enables: [sqli]\n",
                "a.yaml" to sink("a", "sqli"),
            )
        DocumentSetLoader.load(opener, context)
        assertEquals(5, opener.opened.size)
        assertTrue(opener.opened.all { it.closed })
    }

    @Test
    fun `closes the stream of a malformed document`() {
        val opener = opener("index.txt" to "bad.yaml\n", "bad.yaml" to "- subject:\n    trait: x\n")
        assertFailsWith<DocumentSetException> { DocumentSetLoader.load(opener, context) }
        assertEquals(2, opener.opened.size)
        assertTrue(opener.opened.all { it.closed })
    }
}
