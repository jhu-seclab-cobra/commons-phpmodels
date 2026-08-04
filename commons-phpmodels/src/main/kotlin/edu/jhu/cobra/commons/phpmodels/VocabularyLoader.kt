package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/** The two declared sections of a vocabulary document, as written in the file. */
internal data class VocabularyFile(
    val vulnClasses: List<VocabularyEntry>,
    val provenances: List<VocabularyEntry>,
)

/** One declared term of either section, before its name is interned. */
internal data class VocabularyEntry(
    val name: String,
    val description: String,
)

/**
 * Decodes the two-axis [Vocabulary] — danger categories and origin colors —
 * from a YAML document. Where the document lives — classpath resource, file,
 * artifact — is the caller's value placement, not this library's.
 */
public object VocabularyLoader {
    /**
     * Parses a vocabulary document into a [Vocabulary].
     *
     * @param input The document content; consumed and closed by the decode.
     * @return The declared danger categories and origin colors, keyed by interned identity.
     * @throws IllegalArgumentException If an entry is malformed or carries a stray key.
     * @throws VocabularyException If a name repeats within a section.
     */
    public fun load(input: InputStream): Vocabulary {
        val file = ModelYaml.decode(input, jacksonTypeRef<VocabularyFile>())
        val vulnClasses = file.vulnClasses.declaredBy("vulnClasses") { VulnClassId(it.name) }
        val provenances = file.provenances.declaredBy("provenances") { ProvenanceId(it.name) }
        return Vocabulary(
            vulnClasses = vulnClasses.mapValues { (id, entry) -> VulnClassDecl(id, entry.description) },
            provenances = provenances.mapValues { (id, entry) -> ProvenanceDecl(id, entry.description) },
        )
    }

    // Keys the section's entries by interned identity, in file order. A repeated
    // name is a configuration mistake: one of the two declarations would be lost.
    private fun <K> List<VocabularyEntry>.declaredBy(
        section: String,
        identify: (VocabularyEntry) -> K,
    ): Map<K, VocabularyEntry> {
        val declared = LinkedHashMap<K, VocabularyEntry>()
        for (entry in this) {
            if (declared.put(identify(entry), entry) != null) {
                throw VocabularyException("Duplicate $section entry '${entry.name}'")
            }
        }
        return declared
    }
}
