package edu.jhu.cobra.commons.phpmodels

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * In-memory [ResourceOpener] and document builders shared by the
 * `DocumentSetLoader*Test` files.
 */
internal class TrackingStream(
    content: String,
) : ByteArrayInputStream(content.toByteArray()) {
    var closed = false

    override fun close() {
        closed = true
        super.close()
    }
}

internal class MemoryOpener(
    private val files: Map<String, String>,
) : ResourceOpener {
    val opened = mutableListOf<TrackingStream>()

    override fun open(path: String): InputStream? = files[path]?.let { TrackingStream(it).also(::add) }

    private fun add(stream: TrackingStream) {
        opened.add(stream)
    }
}

internal object DocumentSetFixtures {
    const val VOCABULARY: String =
        "vulnClasses:\n  - name: sqli\n    description: sql injection\n" +
            "provenances:\n  - name: user-input\n    description: request data\n"

    const val PROVENANCE: String = "producer: review\nverification: manual\n"

    val context: Vocabulary = VocabularyLoader.load(VOCABULARY.byteInputStream())

    val mapping: CategoryMapping =
        CategoryMappingLoader.load(
            "categories:\n  sql: sqli\n  text: ignore\nprovenances:\n  input: user-input\n".byteInputStream(),
        )

    fun opener(vararg files: Pair<String, String>): MemoryOpener = MemoryOpener(files.toMap())

    fun sink(
        name: String,
        category: String,
    ): String = "- subject:\n    function: $name\n  sinks:\n    - port: argument(0)\n      category: $category\n"

    fun ModelEntry.sinkCategories(): List<VulnClassId> = body.sinks.orEmpty().map { it.vulnClass }
}
