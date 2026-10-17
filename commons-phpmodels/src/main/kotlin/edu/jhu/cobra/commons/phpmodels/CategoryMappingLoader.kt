package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/**
 * The two sections of a mapping document as written: source name to target
 * name, or to the literal [CategoryMappingLoader.IGNORE]. Values decode
 * nullable because the Kotlin module does not enforce map-value
 * non-nullability (impl.md); the loader rejects the null itself.
 */
internal data class CategoryMappingFile(
    val vulnTypes: Map<String, String?>,
    val sourceKinds: Map<String, String?>,
)

/**
 * Decodes a [CategoryMapping] from a YAML document of two maps,
 * `vulnTypes:` and `sourceKinds:`, each source name to a target name or to
 * the literal `ignore`. Where the document lives is the caller's value
 * placement, not this library's.
 */
public object CategoryMappingLoader {
    /** The target literal marking a discarded source name. */
    public const val IGNORE: String = "ignore"

    /**
     * Parses a mapping document into a [CategoryMapping].
     *
     * @param input The document content; consumed by the decode.
     * @return The translation table, both axes.
     * @throws IllegalArgumentException If a section is missing, a key is
     *   stray, a target is null, or a source is spelled `ignore`.
     */
    public fun load(input: InputStream): CategoryMapping {
        val file = ModelYaml.decode(input, jacksonTypeRef<CategoryMappingFile>())
        return CategoryMapping(
            vulnTypes = file.vulnTypes.translated("vulnTypes") { VulnTypeId(it) },
            sourceKinds = file.sourceKinds.translated("sourceKinds") { SourceKindId(it) },
        )
    }

    private fun <K> Map<String, String?>.translated(
        section: String,
        identify: (String) -> K,
    ): Map<K, K?> =
        entries.associate { (source, target) ->
            require(!source.equals(IGNORE, ignoreCase = true)) {
                "Mapping $section lists '$IGNORE' as a source name; it is the discard literal"
            }
            requireNotNull(target) { "Mapping $section entry '$source' has no target; name one or write '$IGNORE'" }
            val mapped = if (target.equals(IGNORE, ignoreCase = true)) null else identify(target)
            identify(source) to mapped
        }
}
