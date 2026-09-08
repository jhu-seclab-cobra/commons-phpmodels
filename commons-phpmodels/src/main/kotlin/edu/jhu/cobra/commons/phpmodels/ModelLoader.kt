package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/**
 * Decodes one model document's entries in file order. Construction-time
 * validation runs during the decode; vocabulary interning of the entries'
 * color and category references stays with the caller, which owns the
 * cross-document load order.
 */
internal object ModelLoader {
    /**
     * Parses a model document into its [ModelEntry] list.
     *
     * @param input The document content; consumed by the decode, not closed.
     * @return The decoded entries in file order.
     * @throws IllegalArgumentException If an entry is malformed: unknown key,
     *   unknown subject kind, malformed spelling, inadmissible section, a
     *   failed body validation, or two entries sharing subject and condition.
     */
    fun load(input: InputStream): List<ModelEntry> {
        val entries = ModelYaml.decode(input, jacksonTypeRef<List<ModelEntry>>())
        val keys = HashSet<Pair<ModelSubject, ArgPattern?>>()
        for (entry in entries) {
            require(keys.add(entry.subject to entry.condition)) {
                "Duplicate entry for ${entry.subject} under condition ${entry.condition?.expected ?: "none"}"
            }
        }
        return entries
    }
}
