package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The translation table of model-sets.md "Translation of One Entry".
 *
 * - `sink …`, `sanitizer …`, `source …` — mapped name replaced, discarded
 *   name removed, emptied element removed.
 * - `entry emptied …` — an entry without a signature that loses its last
 *   section is null; one with a signature keeps the signature.
 * - `value semantics pass through` — returns and propagation are untouched.
 * - `policy …` — origin and categories replaced; discarded origin or
 *   emptied enables drops the row.
 * - `unlisted name fails` — an unlisted name is a failure, not a pass-through.
 */
internal class CategoryMappingTest {
    private val mapping =
        CategoryMapping(
            categories =
                mapOf(
                    VulnClassId("sql") to VulnClassId("sqli"),
                    VulnClassId("html") to VulnClassId("xss"),
                    VulnClassId("text") to null,
                ),
            provenances = mapOf(ProvenanceId("input") to ProvenanceId("user-input"), ProvenanceId("env") to null),
        )

    private fun model(sections: String): SubjectModel =
        loadModel("- subject:\n    function: f\n" + sections.trimIndent().prependIndent("  ") + "\n")

    @Test
    fun `sink category replaced and discarded sink removed`() {
        val entry =
            model(
                """
                sinks:
                  - port: argument(0)
                    category: sql
                  - port: argument(1)
                    category: text
                """,
            )
        val translated = assertIs<SubjectModel>(mapping.apply(entry))
        assertEquals(listOf(SinkPoint(Port.Argument(0), VulnClassId("sqli"))), translated.body.sinks)
    }

    @Test
    fun `sanitizer categories replaced and emptied element removed`() {
        val entry =
            model(
                """
                sanitizers:
                  - categories: [sql, text]
                  - categories: [text]
                """,
            )
        val translated = assertIs<SubjectModel>(mapping.apply(entry))
        assertEquals(listOf(SanitizerDecl(setOf(VulnClassId("sqli")))), translated.body.sanitizers)
    }

    @Test
    fun `source colors replaced and emptied element removed`() {
        val entry =
            loadModel(
                """
                - subject:
                    variable: ${'$'}_GET
                  sources:
                    - provenance: [input, env]
                    - provenance: [env]
                """.trimIndent(),
            )
        val translated = assertIs<SubjectModel>(mapping.apply(entry))
        assertEquals(listOf(SourceDecl(setOf(ProvenanceId("user-input")))), translated.body.sources)
    }

    @Test
    fun `entry emptied without signature is dropped`() {
        val entry = model("sinks:\n  - port: argument(0)\n    category: text")
        assertNull(mapping.apply(entry))
    }

    @Test
    fun `entry emptied with signature keeps the signature`() {
        val entry =
            model(
                """
                signature:
                  params:
                    - name: text
                      type: string
                  returnType: string
                sinks:
                  - port: argument(0)
                    category: text
                """,
            )
        val translated = assertIs<SubjectModel>(mapping.apply(entry))
        assertEquals(true, translated.body.isEmpty)
        assertEquals(entry.signature, translated.signature)
    }

    @Test
    fun `value semantics pass through`() {
        val entry =
            model(
                """
                returns: any
                propagation:
                  - from: argument(0)
                    to: return
                sinks:
                  - port: argument(0)
                    category: html
                """,
            )
        val translated = assertIs<SubjectModel>(mapping.apply(entry))
        assertEquals(entry.body.valueSemantics(), translated.body.valueSemantics())
        val sinks = translated.body.sinks.orEmpty()
        assertEquals(listOf(VulnClassId("xss")), sinks.map { it.category })
    }

    @Test
    fun `generator emptied is dropped`() {
        val entry =
            load(
                """
                - name: printers
                  find: function
                  where:
                    - constraint: name
                      pattern: print.*
                  model:
                    sinks:
                      - port: argument(0)
                        category: text
                """.trimIndent(),
            ).single()
        assertNull(mapping.apply(entry))
    }

    @Test
    fun `policy rows translated and emptied rows dropped`() {
        val rows =
            listOf(
                PolicyRow(ProvenanceId("input"), setOf(VulnClassId("sql"), VulnClassId("text"))),
                PolicyRow(ProvenanceId("input"), setOf(VulnClassId("text"))),
                PolicyRow(ProvenanceId("env"), setOf(VulnClassId("sql"))),
            )
        assertEquals(listOf(PolicyRow(ProvenanceId("user-input"), setOf(VulnClassId("sqli")))), mapping.apply(rows))
    }

    @Test
    fun `unlisted name fails`() {
        val entry = model("sinks:\n  - port: argument(0)\n    category: shell")
        assertFailsWith<VocabularyException> { mapping.apply(entry) }
        assertFailsWith<VocabularyException> { mapping.provenance(ProvenanceId("remote")) }
    }
}
