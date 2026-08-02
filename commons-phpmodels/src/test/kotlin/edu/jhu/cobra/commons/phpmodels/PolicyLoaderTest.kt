package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Decode and vocabulary validation of the policy document.
 *
 * - `policy rows decode and intern their tags` — origin and enabled
 *   categories validated against the vocabulary.
 * - `undeclared reference is rejected` — an unknown color or category is a
 *   [VocabularyException].
 * - `policy folds rows sharing an origin` — [TaintPolicy] unions enabled
 *   categories.
 */
internal class PolicyLoaderTest {
    private val vocabulary =
        Vocabulary(
            vulnClasses =
                mapOf(
                    VulnClassId("sqli") to VulnClassDecl(VulnClassId("sqli"), "SQL injection"),
                    VulnClassId("xss") to VulnClassDecl(VulnClassId("xss"), "Cross-site scripting"),
                ),
            provenances =
                mapOf(
                    ProvenanceId("remote") to ProvenanceDecl(ProvenanceId("remote"), "Remote user input"),
                ),
        )

    private fun load(yaml: String): List<PolicyRow> = PolicyLoader.load(yaml.byteInputStream(), vocabulary)

    @Test
    fun `policy rows decode and intern their tags`() {
        val rows =
            load(
                """
                - origin: remote
                  enables: [sqli, xss]
                """.trimIndent(),
            )
        assertEquals(
            listOf(PolicyRow(ProvenanceId("remote"), setOf(VulnClassId("sqli"), VulnClassId("xss")))),
            rows,
        )
    }

    @Test
    fun `undeclared reference is rejected`() {
        assertFailsWith<VocabularyException> {
            load(
                """
                - origin: local
                  enables: [sqli]
                """.trimIndent(),
            )
        }
        assertFailsWith<VocabularyException> {
            load(
                """
                - origin: remote
                  enables: [rce]
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `policy folds rows sharing an origin`() {
        val policy =
            TaintPolicy(
                load(
                    """
                    - origin: remote
                      enables: [sqli]
                    - origin: remote
                      enables: [xss]
                    """.trimIndent(),
                ),
            )
        assertEquals(true, policy.isDangerous(ProvenanceId("remote"), VulnClassId("sqli")))
        assertEquals(true, policy.isDangerous(ProvenanceId("remote"), VulnClassId("xss")))
        assertEquals(false, policy.isDangerous(ProvenanceId("local"), VulnClassId("sqli")))
    }
}
