package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.value.BoolVal
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
 * - `sanitizer check …` — a check element bounding its argument by a class
 *   or a type set stays when its vulnerability types empty; one stating
 *   only vulnerability types is removed.
 * - `entry emptied …` — an entry without a signature that loses its last
 *   section is null; one with a signature keeps the signature.
 * - `value units pass through` — returns and propagation are untouched.
 * - `raises passes through` — the raises section names classes, not
 *   vocabulary names, and is untouched.
 * - `subject condition signature and ports pass through` — translation
 *   touches names only.
 * - `vulnerability type and source kind translate directly` — mapped name, discarded
 *   name as null.
 * - `policy …` — source kind and vulnerability types replaced; discarded source kind or
 *   emptied enables drops the row.
 * - `unlisted name fails` — an unlisted name is a failure, not a pass-through;
 *   the failure names the name, on an entry, a row, and a direct lookup.
 */
internal class CategoryMappingTest {
    private val mapping =
        CategoryMapping(
            vulnTypes =
                mapOf(
                    VulnTypeId("sql") to VulnTypeId("sqli"),
                    VulnTypeId("html") to VulnTypeId("xss"),
                    VulnTypeId("text") to null,
                ),
            sourceKinds = mapOf(SourceKindId("input") to SourceKindId("user-input"), SourceKindId("env") to null),
        )

    private fun model(sections: String): ModelEntry =
        loadModel("- subject:\n    function: f\n" + sections.trimIndent().prependIndent("  ") + "\n")

    @Test
    fun `sink vulnerability type replaced and discarded sink removed`() {
        val entry =
            model(
                """
                sinks:
                  - port: argument(0)
                    vulnType: sql
                  - port: argument(1)
                    vulnType: text
                """,
            )
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(listOf(SinkDecl(Port.Argument(0), VulnTypeId("sqli"))), translated.body.sinks)
    }

    @Test
    fun `sanitizer vulnerability types replaced and emptied element removed`() {
        val entry =
            model(
                """
                sanitizers:
                  - vulnTypes: [sql, text]
                  - vulnTypes: [text]
                """,
            )
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(listOf(SanitizerDecl(setOf(VulnTypeId("sqli")))), translated.body.sanitizers)
    }

    @Test
    fun `sanitizer check with a bound stays and a check with only vulnerability types is removed`() {
        val entry =
            model(
                """
                sanitizers:
                  - vulnTypes: [text]
                    check:
                      argument: argument(0)
                      accepts: true
                      within: "[0-9]"
                  - vulnTypes: [text]
                    check:
                      argument: argument(0)
                      accepts: true
                """,
            )
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        val check = ArgumentCheck(Port.Argument(0), BoolVal(true), CharacterClass("[0-9]"))
        assertEquals(listOf(SanitizerDecl(emptySet(), check)), translated.body.sanitizers)
    }

    @Test
    fun `source kinds replaced and emptied element removed`() {
        val entry =
            loadModel(
                """
                - subject:
                    variable: ${'$'}_GET
                  sources:
                    - sourceKinds: [input, env]
                    - sourceKinds: [env]
                """.trimIndent(),
            )
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(listOf(SourceDecl(setOf(SourceKindId("user-input")))), translated.body.sources)
    }

    @Test
    fun `entry emptied without signature is dropped`() {
        val entry = model("sinks:\n  - port: argument(0)\n    vulnType: text")
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
                    vulnType: text
                """,
            )
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(true, translated.body.isEmpty)
        assertEquals(entry.signature, translated.signature)
    }

    @Test
    fun `value units pass through`() {
        val entry =
            model(
                """
                returns: any
                propagation:
                  - from: argument(0)
                    to: return
                sinks:
                  - port: argument(0)
                    vulnType: html
                """,
            )
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(entry.body.valueUnit(), translated.body.valueUnit())
        val sinks = translated.body.sinks.orEmpty()
        assertEquals(listOf(VulnTypeId("xss")), sinks.map { it.vulnType })
    }

    @Test
    fun `raises passes through`() {
        val entry = model("raises: [ValueError]\nsinks:\n  - port: argument(0)\n    vulnType: text")
        val translated = assertIs<ModelEntry>(mapping.apply(entry))
        assertEquals(setOf(ClassSubject("valueerror")), translated.body.raises)
    }

    @Test
    fun `policy rows translated and emptied rows dropped`() {
        val rows =
            listOf(
                PolicyRow(SourceKindId("input"), setOf(VulnTypeId("sql"), VulnTypeId("text"))),
                PolicyRow(SourceKindId("input"), setOf(VulnTypeId("text"))),
                PolicyRow(SourceKindId("env"), setOf(VulnTypeId("sql"))),
            )
        assertEquals(listOf(PolicyRow(SourceKindId("user-input"), setOf(VulnTypeId("sqli")))), mapping.apply(rows))
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
                    vulnType: sql
                sources:
                  - sourceKinds: [input]
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
    fun `vulnerability type and source kind translate directly`() {
        assertEquals(VulnTypeId("sqli"), mapping.vulnType(VulnTypeId("sql")))
        assertNull(mapping.vulnType(VulnTypeId("text")))
        assertEquals(SourceKindId("user-input"), mapping.sourceKind(SourceKindId("input")))
        assertNull(mapping.sourceKind(SourceKindId("env")))
    }

    @Test
    fun `unlisted name fails`() {
        val entry = model("sinks:\n  - port: argument(0)\n    vulnType: shell")
        val onEntry = assertFailsWith<VocabularyException> { mapping.apply(entry) }
        assertTrue("shell" in onEntry.message.orEmpty(), onEntry.message)
        val strayOrigin = listOf(PolicyRow(SourceKindId("remote"), setOf(VulnTypeId("sql"))))
        val onRow = assertFailsWith<VocabularyException> { mapping.apply(strayOrigin) }
        assertTrue("remote" in onRow.message.orEmpty(), onRow.message)
        val strayCategory = listOf(PolicyRow(SourceKindId("input"), setOf(VulnTypeId("shell"))))
        assertFailsWith<VocabularyException> { mapping.apply(strayCategory) }
        assertFailsWith<VocabularyException> { mapping.vulnType(VulnTypeId("shell")) }
        assertFailsWith<VocabularyException> { mapping.sourceKind(SourceKindId("remote")) }
    }
}
