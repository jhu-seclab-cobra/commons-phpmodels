package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * End-to-end decode of model documents through [ModelLoader].
 *
 * - `flat model decodes subject sections and guard` — subject, returns,
 *   propagation, sinks, and when-guard of one callable entry.
 * - `entries of every subject kind decode` — the seven one-key subject
 *   spellings route to their subtypes.
 * - `generator entry decodes beside flat models` — deduction routes the
 *   name/find/where/model form.
 * - `mixed-case category and color references intern lowercased` — the
 *   Jackson decode path folds identity tokens like the interning path does.
 * - `signature narrows per subject kind` — callable, class, typed (with its
 *   literal value), and property signatures selected by the entry's subject.
 * - `signature-only entry asserts existence` — an empty body beside a
 *   signature loads.
 * - `propagation beside a callable signature derives returns` — the
 *   value-semantics unit completes from the declared return type.
 * - `signature without propagation derives no returns` — absence of a flow
 *   annotation is not purity.
 * - `explicit returns beside a callable signature is rejected` — one fact,
 *   one source.
 * - `entry asserting nothing is rejected` — no signature and no section.
 * - `variable entry declaring a sink is rejected` — sources-only kinds.
 * - `class entry declaring sources is rejected` — a class asserts nothing
 *   besides its signature.
 * - `guard on a non-callable subject is rejected` — guards are callable-only.
 * - `variable signature is rejected` — superglobals are hand-declared.
 * - `unknown subject kind is rejected` — closed wrapper-key set.
 * - `stray key inside a signature is rejected` — strictness survives the
 *   narrowing.
 * - `propagation without returns is rejected` — the unit is asserted whole.
 */
internal class ModelLoaderTest {
    private fun load(yaml: String): List<ModelEntry> = ModelLoader.load(yaml.byteInputStream())

    private fun loadModel(yaml: String): SubjectModel = assertIs<SubjectModel>(load(yaml).single())

    @Test
    fun `flat model decodes subject sections and guard`() {
        val model =
            loadModel(
                """
                - subject:
                    function: Settype
                  when:
                    port: argument(1)
                    is: string
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
        assertEquals(WhenGuard(Port.Argument(1), GuardValue.StrValue("string")), model.guard)
        assertEquals(ReturnKind.STR, model.body.returns)
        assertEquals(listOf(Propagation(from = Port.Argument(0), to = Port.Return)), model.body.propagation)
        assertEquals(listOf(SinkPoint(Port.Argument(0), VulnClassId("sqli"))), model.body.sinks)
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
        val subjects = entries.map { assertIs<SubjectModel>(it).subject }
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
    fun `generator entry decodes beside flat models`() {
        val entries =
            load(
                """
                - subject:
                    function: getenv
                  sources:
                    - provenance: [environment]
                - name: superglobal-arrays
                  find: variable
                  where:
                    - constraint: name
                      pattern: _(get|post|cookie)
                  model:
                    sources:
                      - provenance: [remote]
                """.trimIndent(),
            )
        val generator = assertIs<ModelGenerator>(entries[1])
        assertEquals("superglobal-arrays", generator.name)
        assertEquals(SubjectKind.VARIABLE, generator.find)
        assertEquals(true, generator.matches(VariableSubject("_get")))
        assertEquals(false, generator.matches(VariableSubject("_server")))
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
            listOf(SinkPoint(Port.Argument(0), VulnClassId("sqli"))),
            assertIs<SubjectModel>(entries[0]).body.sinks,
        )
        assertEquals(
            listOf(SourceDecl(setOf(ProvenanceId("remote")))),
            assertIs<SubjectModel>(entries[1]).body.sources,
        )
    }

    @Test
    fun `signature narrows per subject kind`() {
        val entries =
            load(
                """
                - subject:
                    function: substr
                  signature:
                    params:
                      - name: string
                        type: string
                      - name: offset
                        type: int
                    returnType: string
                - subject:
                    class: mysqli
                  signature:
                    classifier: class
                    parent: Base
                    interfaces: [Traversable]
                - subject:
                    constant: PHP_EOL
                  signature:
                    type: string
                    value: "\n"
                - subject:
                    property: mysqli::${'$'}insert_id
                  signature:
                    type: string
                    visibility: public
                    static: false
                """.trimIndent(),
            )
        val callable = assertIs<SignatureInfo.CallableSignature>(assertIs<SubjectModel>(entries[0]).signature)
        assertEquals(2, callable.params.size)
        assertEquals(DeclaredType("string"), callable.returnType)
        val classSig = assertIs<SignatureInfo.ClassSignature>(assertIs<SubjectModel>(entries[1]).signature)
        assertEquals(Classifier.CLASS, classSig.classifier)
        assertEquals("base", classSig.parent)
        assertEquals(listOf("traversable"), classSig.interfaces)
        val typed = assertIs<SignatureInfo.TypedSignature>(assertIs<SubjectModel>(entries[2]).signature)
        assertEquals("\n", typed.value)
        val property = assertIs<SignatureInfo.PropertySignature>(assertIs<SubjectModel>(entries[3]).signature)
        assertEquals(Visibility.PUBLIC, property.visibility)
    }

    @Test
    fun `signature-only entry asserts existence`() {
        val model =
            loadModel(
                """
                - subject:
                    function: strlen
                  signature:
                    returnType: int
                """.trimIndent(),
            )
        assertEquals(true, model.body.isEmpty)
        assertNull(model.body.valueSemantics())
    }

    @Test
    fun `propagation beside a callable signature derives returns`() {
        val model =
            loadModel(
                """
                - subject:
                    function: substr
                  signature:
                    returnType: string
                  propagation:
                    - from: argument(0)
                      to: return
                """.trimIndent(),
            )
        assertEquals(ReturnKind.STR, model.body.returns)
    }

    @Test
    fun `signature without propagation derives no returns`() {
        val model =
            loadModel(
                """
                - subject:
                    function: substr
                  signature:
                    returnType: string
                """.trimIndent(),
            )
        assertNull(model.body.returns)
    }

    @Test
    fun `explicit returns beside a callable signature is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: substr
                  signature:
                    returnType: string
                  returns: str
                """.trimIndent(),
            )
        }
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
    fun `variable entry declaring a sink is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    variable: ${'$'}_GET
                  sinks:
                    - port: argument(0)
                      category: sqli
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `class entry declaring sources is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    class: mysqli
                  signature:
                    classifier: class
                  sources:
                    - provenance: [remote]
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `guard on a non-callable subject is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    constant: PHP_EOL
                  when:
                    port: argument(0)
                    is: true
                  signature:
                    type: string
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `variable signature is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    variable: ${'$'}_GET
                  signature:
                    type: array
                  sources:
                    - provenance: [remote]
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
    fun `stray key inside a signature is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    constant: PHP_EOL
                  signature:
                    type: string
                    stray: 1
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `propagation without returns is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  propagation:
                    - from: argument(0)
                      to: return
                """.trimIndent(),
            )
        }
    }
}
