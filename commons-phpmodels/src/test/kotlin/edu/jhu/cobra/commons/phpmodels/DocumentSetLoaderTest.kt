package edu.jhu.cobra.commons.phpmodels

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The set-level load of design-sets.md over an in-memory [ResourceOpener].
 *
 * - `manifest …` — absent manifest, absent document, and doubled line fail;
 *   comment and blank lines are not entries; order is manifest order.
 * - `declared …` — vocabulary merges into the context, policy and entries
 *   decode against the merge, an undeclared reference or a conflicting
 *   redeclaration fails, and the returned vocabulary is the set's own.
 * - `mapped …` — vocabulary.yaml is ignored, an undeclared target fails,
 *   entries and rows are translated, emptied entries drop, the returned
 *   vocabulary is empty, and an unlisted name fails.
 * - `closes every stream` — the opener's streams are released.
 */
internal class DocumentSetLoaderTest {
    private class TrackingStream(
        content: String,
    ) : ByteArrayInputStream(content.toByteArray()) {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    private class MemoryOpener(
        private val files: Map<String, String>,
    ) : ResourceOpener {
        val opened = mutableListOf<TrackingStream>()

        override fun open(path: String): InputStream? = files[path]?.let { TrackingStream(it).also(opened::add) }
    }

    private val vocabulary =
        """
        vulnClasses:
          - name: sqli
            description: sql injection
        provenances:
          - name: user-input
            description: request data
        """.trimIndent()

    private val context = VocabularyLoader.load(vocabulary.byteInputStream())

    private fun opener(vararg files: Pair<String, String>): MemoryOpener = MemoryOpener(files.toMap())

    private fun SubjectModel.sinkCategories(): List<VulnClassId> =
        body.sinks
            .orEmpty()
            .map { it.category }

    private fun sink(
        name: String,
        category: String,
    ): String = "- subject:\n    function: $name\n  sinks:\n    - port: argument(0)\n      category: $category\n"

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
        val entry = assertIs<SubjectModel>(set.entries.single())
        assertEquals(listOf(VulnClassId("xss")), entry.sinkCategories())
    }

    @Test
    fun `declared set without vocabulary contributes nothing`() {
        val set = DocumentSetLoader.load(opener("index.txt" to "a.yaml\n", "a.yaml" to sink("a", "sqli")), context)
        assertEquals(Vocabulary.EMPTY, set.vocabulary)
        assertEquals(emptyList(), set.policy)
    }

    @Test
    fun `declared set undeclared reference fails`() {
        assertFailsWith<VocabularyException> {
            DocumentSetLoader.load(opener("index.txt" to "a.yaml\n", "a.yaml" to sink("a", "xss")), context)
        }
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
        val set = DocumentSetLoader.load(opener("index.txt" to "", "vocabulary.yaml" to vocabulary), context)
        assertEquals(context, set.vocabulary)
    }

    private val mapping =
        CategoryMappingLoader.load(
            "categories:\n  sql: sqli\n  text: ignore\nprovenances:\n  input: user-input\n".byteInputStream(),
        )

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
        assertEquals(listOf(PolicyRow(ProvenanceId("user-input"), setOf(VulnClassId("sqli")))), set.policy)
        val entry = assertIs<SubjectModel>(set.entries.single())
        assertEquals("a", (entry.subject as FunctionSubject).name)
        assertEquals(listOf(VulnClassId("sqli")), entry.sinkCategories())
    }

    @Test
    fun `mapped set undeclared target fails`() {
        val stray = CategoryMapping(mapOf(VulnClassId("sql") to VulnClassId("xss")), emptyMap())
        assertFailsWith<VocabularyException> {
            DocumentSetLoader.load(opener("index.txt" to ""), context, stray)
        }
    }

    @Test
    fun `mapped set unlisted name fails`() {
        assertFailsWith<VocabularyException> {
            DocumentSetLoader.load(opener("index.txt" to "a.yaml\n", "a.yaml" to sink("a", "shell")), context, mapping)
        }
        assertFailsWith<VocabularyException> {
            DocumentSetLoader.load(
                opener("index.txt" to "", "policy.yaml" to "- origin: remote\n  enables: [sql]\n"),
                context,
                mapping,
            )
        }
    }

    @Test
    fun `closes every stream`() {
        val opener =
            opener(
                "index.txt" to "a.yaml\n",
                "vocabulary.yaml" to vocabulary,
                "policy.yaml" to "- origin: user-input\n  enables: [sqli]\n",
                "a.yaml" to sink("a", "sqli"),
            )
        DocumentSetLoader.load(opener, context)
        assertEquals(4, opener.opened.size)
        assertTrue(opener.opened.all { it.closed })
    }
}
