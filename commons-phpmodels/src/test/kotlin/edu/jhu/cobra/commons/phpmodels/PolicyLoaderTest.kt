package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Decode and vocabulary validation of the policy document.
 *
 * - `policy rows decode and intern their tags` — source kind and enabled
 *   vulnerability types validated against the vocabulary.
 * - `undeclared reference is rejected` — an unknown source kind or
 *   vulnerability type is a
 *   [VocabularyException].
 * - `policy folds rows sharing a source kind` — [TaintPolicy] unions enabled
 *   vulnerability types.
 * - `row enabling no vulnerability type is rejected` — an empty enables set asserts
 *   nothing, like every other empty declared section.
 * - `rows keep file order` — the consumer appends rows in order.
 * - `stray key is rejected` — strict decode.
 * - `empty document decodes to no rows` — `[]` states no policy.
 */
internal class PolicyLoaderTest {
    private val vocabulary =
        Vocabulary(
            vulnTypes =
                mapOf(
                    VulnTypeId("sqli") to VulnTypeDecl(VulnTypeId("sqli"), "SQL injection"),
                    VulnTypeId("xss") to VulnTypeDecl(VulnTypeId("xss"), "Cross-site scripting"),
                ),
            sourceKinds =
                mapOf(
                    SourceKindId("remote") to SourceKindDecl(SourceKindId("remote"), "Remote user input"),
                ),
        )

    private fun load(yaml: String): List<PolicyRow> = PolicyLoader.load(yaml.byteInputStream(), vocabulary)

    @Test
    fun `policy rows decode and intern their tags`() {
        val rows =
            load(
                """
                - sourceKind: remote
                  enables: [sqli, xss]
                """.trimIndent(),
            )
        assertEquals(
            listOf(PolicyRow(SourceKindId("remote"), setOf(VulnTypeId("sqli"), VulnTypeId("xss")))),
            rows,
        )
    }

    @Test
    fun `undeclared reference is rejected`() {
        assertFailsWith<VocabularyException> {
            load(
                """
                - sourceKind: local
                  enables: [sqli]
                """.trimIndent(),
            )
        }
        assertFailsWith<VocabularyException> {
            load(
                """
                - sourceKind: remote
                  enables: [rce]
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `row enabling no vulnerability type is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - sourceKind: remote
                  enables: []
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `policy folds rows sharing a source kind`() {
        val policy =
            TaintPolicy(
                load(
                    """
                    - sourceKind: remote
                      enables: [sqli]
                    - sourceKind: remote
                      enables: [xss]
                    """.trimIndent(),
                ),
            )
        assertEquals(true, policy.isDangerous(SourceKindId("remote"), VulnTypeId("sqli")))
        assertEquals(true, policy.isDangerous(SourceKindId("remote"), VulnTypeId("xss")))
        assertEquals(false, policy.isDangerous(SourceKindId("local"), VulnTypeId("sqli")))
    }

    @Test
    fun `rows keep file order`() {
        val rows = load("- sourceKind: remote\n  enables: [xss]\n- sourceKind: remote\n  enables: [sqli]\n")
        assertEquals(listOf(setOf(VulnTypeId("xss")), setOf(VulnTypeId("sqli"))), rows.map { it.enables })
    }

    @Test
    fun `stray key is rejected`() {
        assertFailsWith<IllegalArgumentException> { load("- sourceKind: remote\n  enables: [sqli]\n  note: x\n") }
    }

    @Test
    fun `empty document decodes to no rows`() {
        assertEquals(emptyList(), load("[]"))
    }
}
