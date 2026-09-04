package edu.jhu.cobra.commons.phpmodels

import java.io.InputStream

/**
 * The caller's storage of one document set. Given a path relative to the
 * set root, yields the content or null when nothing is there. The library
 * never touches the classpath or the file system itself; the loader closes
 * every stream it opens.
 */
public fun interface ResourceOpener {
    /** Opens the resource at [path] under the set root, or returns null when absent. */
    public fun open(path: String): InputStream?
}

/**
 * One decoded model document of a set.
 *
 * @property path The document's manifest spelling, relative to the root.
 * @property entries The document's entries in document order.
 */
public data class Document(
    val path: String,
    val entries: List<ModelEntry>,
)

/**
 * One loaded document set, ready for a consumer to mount: the caller unions
 * [vocabulary] into its accumulator and appends [policy] and [entries] in
 * order.
 *
 * @property vocabulary The declarations this set contributed; empty for a mapped set.
 * @property policy This set's rows, already in the consumer's names.
 * @property documents The listed documents in manifest order.
 */
public data class DocumentSet(
    val vocabulary: Vocabulary,
    val policy: List<PolicyRow>,
    val documents: List<Document>,
) {
    /** Every document's entries in manifest then document order. */
    public val entries: List<ModelEntry>
        get() = documents.flatMap { it.entries }
}

/**
 * Raised when a document set's storage does not match its manifest: the
 * manifest is absent, a listed document is absent, or a path is listed twice.
 *
 * @property path The manifest or document path the failure names.
 */
public class DocumentSetException(
    public val path: String,
    detail: String,
) : IllegalArgumentException("Document set: $detail ('$path')")
