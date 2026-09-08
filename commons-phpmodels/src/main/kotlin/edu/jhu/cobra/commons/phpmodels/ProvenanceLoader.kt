package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/** The provenance document as written in the file, before validation. */
internal data class ProvenanceFile(
    val producer: String,
    val verification: Verification,
)

/**
 * Decodes one set's [SetProvenance] from its `provenance.yaml`. Where the
 * document lives is the caller's value placement, not this library's.
 */
internal object ProvenanceLoader {
    /**
     * Parses a provenance document into a [SetProvenance].
     *
     * @param input The document content; consumed by the decode, not closed.
     * @return The producer and verification kind the set declares.
     * @throws IllegalArgumentException If a field is missing, the producer is blank, the verification
     *   kind is not `generated` or `manual`, or the document carries a stray key.
     */
    fun load(input: InputStream): SetProvenance {
        val file = ModelYaml.decode(input, jacksonTypeRef<ProvenanceFile>())
        return SetProvenance(file.producer, file.verification)
    }
}
