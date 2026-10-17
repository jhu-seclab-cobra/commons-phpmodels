package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/** The two declared sections of a vocabulary document, as written in the file. */
internal data class VocabularyFile(
    val vulnTypes: List<VocabularyEntry>,
    val sourceKinds: List<VocabularyEntry>,
)

/** One declared term of either section, before its name is interned. */
internal data class VocabularyEntry(
    val name: String,
    val description: String,
)

/**
 * Decodes the two-axis [Vocabulary] — vulnerability types and source kinds —
 * from a YAML document. Where the document lives — classpath resource, file,
 * artifact — is the caller's value placement, not this library's.
 */
internal object VocabularyLoader {
    /**
     * Parses a vocabulary document into a [Vocabulary].
     *
     * @param input The document content; consumed by the decode, not closed.
     * @return The declared vulnerability types and source kinds, keyed by interned identity.
     * @throws IllegalArgumentException If an entry is malformed or carries a stray key.
     * @throws VocabularyException If a name repeats within a section.
     */
    fun load(input: InputStream): Vocabulary {
        val file = ModelYaml.decode(input, jacksonTypeRef<VocabularyFile>())
        val vulnTypes = file.vulnTypes.declaredBy("vulnTypes") { VulnTypeId(it.name) }
        val sourceKinds = file.sourceKinds.declaredBy("sourceKinds") { SourceKindId(it.name) }
        return Vocabulary(
            vulnTypes = vulnTypes.mapValues { (id, entry) -> VulnTypeDecl(id, entry.description) },
            sourceKinds = sourceKinds.mapValues { (id, entry) -> SourceKindDecl(id, entry.description) },
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
