package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStream

/**
 * The one YAML decoder every document of the format passes through, so the
 * strictness the design states is declared once instead of per loader.
 * Internal — the three loaders are the public surface, so no Jackson type
 * crosses the API. Verified library behavior: docs/impl.md.
 */
internal object ModelYaml {
    // FAIL_ON_UNKNOWN_PROPERTIES is a stated design rule, not an inherited default:
    // a stray key is a configuration mistake and fails the load. Case-insensitive
    // enums let the files keep the lowercase vocabulary used by the identifiers
    // beside them; an unrecognized constant still fails. Duplicate-key detection
    // closes the stream-level gap: without it a doubled key decodes last-wins,
    // silently dropping the earlier declaration.
    private val mapper: ObjectMapper =
        YAMLMapper
            .builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build()
            .registerKotlinModule()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    /**
     * Decodes [input] into [shape].
     *
     * @throws IllegalArgumentException If the content does not decode into [shape]
     *   — unknown key, unknown discriminator, missing field, rejected value.
     */
    fun <T> decode(
        input: InputStream,
        shape: TypeReference<T>,
    ): T =
        try {
            mapper.readValue(input, shape)
        } catch (cause: JsonProcessingException) {
            throw IllegalArgumentException("Malformed model document: ${cause.originalMessage}", cause)
        }

    /**
     * Converts an already-decoded tree [node] into [shape], keeping the
     * mapper's strictness. Used by creators that narrow a mapping whose
     * target type depends on a sibling field.
     *
     * @throws JsonProcessingException If the node does not decode into [shape];
     *   propagates into the enclosing [decode] call's failure handling.
     */
    fun <T : Any> narrow(
        node: JsonNode,
        shape: Class<T>,
    ): T = mapper.treeToValue(node, shape)
}
