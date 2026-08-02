package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/**
 * Decodes one model document's entries — explicit models and generators, in
 * file order. Construction-time validation runs during the decode; vocabulary
 * interning of the entries' color and category references stays with the
 * caller, which owns the cross-document load order.
 */
public object ModelLoader {
    /**
     * Parses a model document into its [ModelEntry] list.
     *
     * @param input The document content; consumed and closed by the decode.
     * @return The decoded entries in file order.
     * @throws IllegalArgumentException If an entry is malformed: unknown key,
     *   unknown subject kind, malformed spelling, inadmissible section, or a
     *   failed body validation.
     */
    public fun load(input: InputStream): List<ModelEntry> = ModelYaml.decode(input, jacksonTypeRef<List<ModelEntry>>())
}
