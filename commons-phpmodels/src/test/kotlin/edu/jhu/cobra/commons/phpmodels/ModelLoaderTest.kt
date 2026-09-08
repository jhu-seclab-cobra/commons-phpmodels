package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.value.FloatVal
import edu.jhu.cobra.commons.value.StrVal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * End-to-end decode of model documents through [ModelLoader]: the entry forms
 * and the document strictness. Signature handling:
 * [ModelLoaderSignatureTest]; subject-kind admissibility:
 * [ModelLoaderAdmissibilityTest]; signature-fixed port semantics:
 * [ModelLoaderPortBoundsTest].
 *
 * - `entry decodes subject sections and condition` — subject, returns,
 *   propagation, sinks, and when-condition of one callable entry.
 * - `entries of every subject kind decode` — the seven one-key subject
 *   spellings route to their subtypes.
 * - `sanitizers section decodes to its category sets` — the fifth assertion
 *   section round-trips through the loader.
 * - `generator keys are rejected` — the retired name/find/where/model form
 *   fails as unknown keys.
 * - `mixed-case category and color references intern lowercased` — the
 *   Jackson decode path folds identity tokens like the interning path does.
 * - `entry asserting nothing is rejected` — no signature and no section.
 * - `unknown subject kind is rejected` — closed wrapper-key set.
 * - `condition integer beyond Long range reads as a float` — the compared
 *   value never truncates silently; it widens to the float shape.
 * - `second document in one stream is rejected` — entries after a `---`
 *   separator never drop silently.
 * - `duplicate key in one mapping is rejected` — a doubled key never decodes
 *   last-wins, at the entry level and inside a propagation pair.
 */
internal class ModelLoaderTest {
    @Test
    fun `entry decodes subject sections and condition`() {
        val model =
            loadModel(
                """
                - subject:
                    function: Settype
                  when: [_, string]
                  returns: str
                  propagation:
                    - from: argument(0)
                      to: return
                  sinks:
                    - port: argument(0)
                      category: sqli
                """.trimIndent(),
            )
        assertEquals(FunctionSubject("settype"), model.subject)
        assertEquals(ArgPattern(listOf(null, StrVal("string"))), model.condition)
        assertEquals(ReturnKind.STR, model.body.returns)
        assertEquals(listOf(Propagation(from = Port.Argument(0), to = Port.Return)), model.body.propagation)
        assertEquals(listOf(SinkDecl(Port.Argument(0), VulnClassId("sqli"))), model.body.sinks)
    }

    @Test
    fun `entries of every subject kind decode`() {
        val entries =
            load(
                """
                - subject:
                    function: strlen
                  returns: num
                - subject:
                    class: mysqli
                  signature:
                    classifier: class
                - subject:
                    method: mysqli::query
                  returns: any
                - subject:
                    class_constant: mysqli::MYSQLI_REPORT_ERROR
                  signature:
                    type: int
                - subject:
                    property: mysqli::${'$'}insert_id
                  signature:
                    type: string
                    visibility: public
                - subject:
                    constant: PHP_EOL
                  signature:
                    type: string
                - subject:
                    variable: ${'$'}_GET
                  sources:
                    - provenance: [remote]
                """.trimIndent(),
            )
        val subjects = entries.map { assertIs<ModelEntry>(it).subject }
        assertEquals(
            listOf(
                FunctionSubject("strlen"),
                ClassSubject("mysqli"),
                MethodSubject("mysqli", "query"),
                ClassConstantSubject("mysqli", "MYSQLI_REPORT_ERROR"),
                PropertySubject("mysqli", "insert_id"),
                ConstantSubject("PHP_EOL"),
                VariableSubject("_get"),
            ),
            subjects,
        )
    }

    @Test
    fun `sanitizers section decodes to its category sets`() {
        val model =
            loadModel(
                """
                - subject:
                    function: mysqli_real_escape_string
                  sanitizers:
                    - categories: [sqli, xss]
                """.trimIndent(),
            )
        assertEquals(
            listOf(SanitizerDecl(setOf(VulnClassId("sqli"), VulnClassId("xss")))),
            model.body.sanitizers,
        )
    }

    @Test
    fun `generator keys are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  name: string-length
                  find: function
                  where:
                    - constraint: name
                      pattern: strlen
                  model:
                    returns: num
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `mixed-case category and color references intern lowercased`() {
        val entries =
            load(
                """
                - subject:
                    method: mysqli::query
                  sinks:
                    - port: argument(0)
                      category: SQLI
                - subject:
                    variable: ${'$'}_GET
                  sources:
                    - provenance: [Remote]
                """.trimIndent(),
            )
        assertEquals(
            listOf(SinkDecl(Port.Argument(0), VulnClassId("sqli"))),
            assertIs<ModelEntry>(entries[0]).body.sinks,
        )
        assertEquals(
            listOf(SourceDecl(setOf(OriginId("remote")))),
            assertIs<ModelEntry>(entries[1]).body.sources,
        )
    }

    @Test
    fun `entry asserting nothing is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `unknown subject kind is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    trait: foo
                  returns: any
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `condition integer beyond Long range reads as a float`() {
        val model =
            loadModel(
                """
                - subject:
                    function: strlen
                  when: [99999999999999999999999999]
                  returns: num
                """.trimIndent(),
            )
        assertEquals(ArgPattern(listOf(FloatVal(1.0e26))), model.condition)
    }

    @Test
    fun `second document in one stream is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  returns: num
                ---
                - subject:
                    function: substr
                  returns: str
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `duplicate key in one mapping is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  returns: str
                  returns: num
                """.trimIndent(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: substr
                  returns: str
                  propagation:
                    - from: argument(0)
                      from: argument(1)
                      to: return
                """.trimIndent(),
            )
        }
    }
}
