package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The translation table of model-sets.md "Translation of One Entry".
 *
 * - `sink …`, `sanitizer …`, `source …` — mapped name replaced, discarded
 *   name removed, emptied element removed.
 * - `entry emptied …` — an entry without a signature that loses its last
 *   section is null; one with a signature keeps the signature.
 * - `value semantics pass through` — returns and propagation are untouched.
 * - `subject condition signature and ports pass through` — translation
 *   touches names only.
 * - `category and origin translate directly` — mapped name, discarded
 *   name as null.
 * - `policy …` — origin and categories replaced; discarded origin or
 *   emptied enables drops the row.
 * - `unlisted name fails` — an unlisted name is a failure, not a pass-through;
 *   the failure names the name, on an entry, a row, and a direct lookup.
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
            origins = mapOf(OriginId("input") to OriginId("user-input"), OriginId("env") to null),
        )

    private fun model(sections: String): ModelEntry =
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
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(listOf(SinkDecl(Port.Argument(0), VulnClassId("sqli"))), translated.body.sinks)
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
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
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
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(listOf(SourceDecl(setOf(OriginId("user-input")))), translated.body.sources)
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
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
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
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(entry.body.valueSemantics(), translated.body.valueSemantics())
        val sinks = translated.body.sinks.orEmpty()
        assertEquals(listOf(VulnClassId("xss")), sinks.map { it.vulnClass })
    }

    @Test
    fun `policy rows translated and emptied rows dropped`() {
        val rows =
            listOf(
                PolicyRow(OriginId("input"), setOf(VulnClassId("sql"), VulnClassId("text"))),
                PolicyRow(OriginId("input"), setOf(VulnClassId("text"))),
                PolicyRow(OriginId("env"), setOf(VulnClassId("sql"))),
            )
        assertEquals(listOf(PolicyRow(OriginId("user-input"), setOf(VulnClassId("sqli")))), mapping.apply(rows))
    }

    @Test
    fun `subject condition signature and ports pass through`() {
        val entry =
            model(
                """
                when: [_, true]
                signature:
                  params:
                    - name: query
                      type: string
                    - name: mode
                      type: bool
                    - name: out
                      type: array
                      byRef: true
                  returnType: string
                sinks:
                  - port: argument(0)
                    category: sql
                sources:
                  - provenance: [input]
                    at: argument(2)
                    keys: ["k.*"]
                """,
            )
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(entry.subject, translated.subject)
        assertEquals(entry.condition, translated.condition)
        assertEquals(entry.signature, translated.signature)
        assertEquals(entry.body.sinks?.map { it.port }, translated.body.sinks?.map { it.port })
        val source = translated.body.sources!!.single()
        assertEquals(Port.Argument(2), source.at)
        assertEquals(listOf(KeyPattern("k.*")), source.keys)
    }

    @Test
    fun `category and origin translate directly`() {
        assertEquals(VulnClassId("sqli"), mapping.category(VulnClassId("sql")))
        assertNull(mapping.category(VulnClassId("text")))
        assertEquals(OriginId("user-input"), mapping.origin(OriginId("input")))
        assertNull(mapping.origin(OriginId("env")))
    }

    @Test
    fun `unlisted name fails`() {
        val entry = model("sinks:\n  - port: argument(0)\n    category: shell")
        val onEntry = assertFailsWith<VocabularyException> { mapping.apply(entry) }
        assertTrue("shell" in onEntry.message.orEmpty(), onEntry.message)
        val strayOrigin = listOf(PolicyRow(OriginId("remote"), setOf(VulnClassId("sql"))))
        val onRow = assertFailsWith<VocabularyException> { mapping.apply(strayOrigin) }
        assertTrue("remote" in onRow.message.orEmpty(), onRow.message)
        val strayCategory = listOf(PolicyRow(OriginId("input"), setOf(VulnClassId("shell"))))
        assertFailsWith<VocabularyException> { mapping.apply(strayCategory) }
        assertFailsWith<VocabularyException> { mapping.category(VulnClassId("shell")) }
        assertFailsWith<VocabularyException> { mapping.origin(OriginId("remote")) }
    }
}
