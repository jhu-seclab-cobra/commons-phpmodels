package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.InputStream

/** One policy row as written in the file, before its tags are interned. */
internal data class PolicyRowEntry(
    val sourceKind: String,
    val enables: List<String>,
)

/**
 * Decodes the source kind → vulnerability types [PolicyRow] list from a YAML
 * document, validating every source kind and vulnerability type against an
 * already-interned [Vocabulary].
 */
internal object PolicyLoader {
    /**
     * Parses a policy document into a list of [PolicyRow].
     *
     * @param input The document content; consumed by the decode, not closed.
     * @param vocabulary Declared source kinds and vulnerability types; every row tag is validated against it.
     * @return The policy rows in file order.
     * @throws IllegalArgumentException If a row is malformed or carries a stray key.
     * @throws VocabularyException If a row references an undeclared source kind or vulnerability type.
     */
    fun load(
        input: InputStream,
        vocabulary: Vocabulary,
    ): List<PolicyRow> =
        ModelYaml.decode(input, jacksonTypeRef<List<PolicyRowEntry>>()).map { row ->
            PolicyRow(
                sourceKind = vocabulary.requireSourceKind(row.sourceKind),
                enables = row.enables.map(vocabulary::requireVulnType).toSet(),
            )
        }
}
