package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/** One policy row as written in the file, before its tags are interned. */
internal data class PolicyRowEntry(
    val origin: String,
    val enables: List<String>,
)

/**
 * Decodes the origin→categories [PolicyRow] list from a YAML document,
 * validating every origin color and danger category against an
 * already-interned [Vocabulary].
 */
public object PolicyLoader {
    /**
     * Parses a policy document into a list of [PolicyRow].
     *
     * @param input The document content; consumed and closed by the decode.
     * @param vocabulary Declared colors and categories; every row tag is validated against it.
     * @return The policy rows in file order.
     * @throws IllegalArgumentException If a row is malformed or carries a stray key.
     * @throws VocabularyException If a row references an undeclared color or category.
     */
    public fun load(
        input: InputStream,
        vocabulary: Vocabulary,
    ): List<PolicyRow> =
        ModelYaml.decode(input, jacksonTypeRef<List<PolicyRowEntry>>()).map { row ->
            PolicyRow(
                origin = vocabulary.requireProvenance(row.origin),
                enables = row.enables.map(vocabulary::requireVulnClass).toSet(),
            )
        }
}
