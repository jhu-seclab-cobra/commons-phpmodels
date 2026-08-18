package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Signature handling of decoded entries: subtype narrowing per subject kind
 * and the completion of the value-semantics unit from a declared return type.
 *
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
 * - `propagation without returns is rejected` — the unit is asserted whole.
 * - `variable signature is rejected` — superglobals are hand-declared.
 * - `stray key inside a signature is rejected` — strictness survives the
 *   narrowing.
 */
internal class ModelLoaderSignatureTest {
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
                    params:
                      - name: string
                        type: string
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
}
