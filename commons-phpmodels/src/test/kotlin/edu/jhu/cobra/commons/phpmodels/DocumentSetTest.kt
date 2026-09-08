package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The loaded-set value types of design-sets.md.
 *
 * - `entries flatten documents in manifest then document order` — the
 *   consumer appends entries in that order.
 * - `entries of an empty set is the empty list` — a set listing nothing.
 * - `provenance defaults to null` — a root without `provenance.yaml`.
 * - `exception message names the detail and the path` — the failure names
 *   the document among the many a set lists.
 * - `exception keeps the cause and is an IllegalArgumentException` — the
 *   decode failure of a malformed document stays reachable.
 * - `resource opener yields null for an absent path` — the caller's storage
 *   reports absence with null, never an exception.
 */
internal class DocumentSetTest {
    private fun entry(name: String): ModelEntry = ModelEntry(FunctionSubject(name), body = ModelBody(ReturnKind.ANY))

    @Test
    fun `entries flatten documents in manifest then document order`() {
        val set =
            DocumentSet(
                vocabulary = Vocabulary.EMPTY,
                policy = emptyList(),
                documents =
                    listOf(
                        Document("b.yaml", listOf(entry("b1"), entry("b2"))),
                        Document("a.yaml", listOf(entry("a1"))),
                    ),
            )
        assertEquals(listOf("b1", "b2", "a1"), set.entries.map { it.subject.name })
    }

    @Test
    fun `entries of an empty set is the empty list`() {
        assertEquals(emptyList(), DocumentSet(Vocabulary.EMPTY, emptyList(), emptyList()).entries)
    }

    @Test
    fun `provenance defaults to null`() {
        assertNull(DocumentSet(Vocabulary.EMPTY, emptyList(), emptyList()).provenance)
    }

    @Test
    fun `exception message names the detail and the path`() {
        val failure = DocumentSetException("models/a.yaml", "listed document is absent")
        assertEquals("models/a.yaml", failure.path)
        val message = failure.message.orEmpty()
        assertTrue("listed document is absent" in message, message)
        assertTrue("'models/a.yaml'" in message, message)
    }

    @Test
    fun `exception keeps the cause and is an IllegalArgumentException`() {
        val cause = IllegalArgumentException("Malformed model document")
        val failure = DocumentSetException("a.yaml", "malformed", cause)
        assertSame(cause, failure.cause)
        assertIs<IllegalArgumentException>(failure)
    }

    @Test
    fun `resource opener yields null for an absent path`() {
        val opener = ResourceOpener { path -> if (path == "index.txt") "".byteInputStream() else null }
        assertNull(opener.open("missing.yaml"))
        opener.open("index.txt").let { assertTrue(it != null) }
    }
}
