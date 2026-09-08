package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.phpmodels.SignatureInfo.CallableSignature
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.ClassSignature
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.PropertySignature
import edu.jhu.cobra.commons.phpmodels.SignatureInfo.TypedSignature
import edu.jhu.cobra.commons.value.BoolVal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * The construction invariants of [ModelEntry] on the direct path
 * (design-entries.md): the same rules the decode path enforces, checked
 * without a document. Decode-path forms: [ModelEntryAdmissibilityTest],
 * [ModelEntryArityTest], [ModelEntryWrittenPortTest], [ModelEntrySignatureTest].
 *
 * - `entry asserting nothing is rejected` — no signature and an empty body.
 * - `signature-only entry is admitted` — existence is an assertion.
 * - `signature subtype must match the subject kind` — a class signature on
 *   a function, a callable signature on a class, a typed signature on a
 *   property, and any signature on a variable are rejected.
 * - `matching signature subtype is admitted per kind` — the five kinds that
 *   admit a signature accept their subtype.
 * - `receiver port on a non-method subject is rejected` — on either side of
 *   a propagation pair.
 * - `condition on a non-callable subject is rejected` — conditions apply to
 *   callable subjects only.
 * - `explicit source site on a non-callable subject is rejected` — sites
 *   apply to callable subjects only.
 * - `non-callable kinds admit a sources section` — constant, class constant,
 *   property, and variable declare sources.
 * - `non-callable kind rejects returns` — value semantics belong to callables.
 * - `class subject rejects every assertion section` — a class asserts
 *   nothing besides its signature.
 * - `entries compare by value` — equality over subject, condition,
 *   signature, and body.
 */
internal class ModelEntryTest {
    private val sources = ModelBody(sources = listOf(SourceDecl(setOf(OriginId("remote")))))
    private val returns = ModelBody(returns = ReturnKind.ANY)
    private val callable = CallableSignature(returnType = DeclaredType("string"))
    private val classSignature = ClassSignature(Classifier.CLASS)
    private val typed = TypedSignature(DeclaredType("string"))
    private val property = PropertySignature(DeclaredType("string"), Visibility.PUBLIC)

    private fun flow(
        from: Port.Input,
        to: Port,
    ): ModelBody = ModelBody(returns = ReturnKind.ANY, propagation = listOf(Propagation(from = from, to = to)))

    @Test
    fun `entry asserting nothing is rejected`() {
        assertFailsWith<IllegalArgumentException> { ModelEntry(FunctionSubject("f")) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(FunctionSubject("f"), body = ModelBody()) }
    }

    @Test
    fun `signature-only entry is admitted`() {
        assertEquals(true, ModelEntry(FunctionSubject("f"), signature = callable).body.isEmpty)
    }

    @Test
    fun `signature subtype must match the subject kind`() {
        assertFailsWith<IllegalArgumentException> { ModelEntry(FunctionSubject("f"), signature = classSignature) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(ClassSubject("c"), signature = callable) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(PropertySubject("c", "p"), signature = typed) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(ConstantSubject("K"), signature = property) }
        assertFailsWith<IllegalArgumentException> {
            ModelEntry(VariableSubject("_GET"), signature = typed, body = sources)
        }
    }

    @Test
    fun `matching signature subtype is admitted per kind`() {
        assertEquals(callable, ModelEntry(MethodSubject("c", "m"), signature = callable).signature)
        assertEquals(classSignature, ModelEntry(ClassSubject("c"), signature = classSignature).signature)
        assertEquals(typed, ModelEntry(ConstantSubject("K"), signature = typed).signature)
        assertEquals(typed, ModelEntry(ClassConstantSubject("c", "K"), signature = typed).signature)
        assertEquals(property, ModelEntry(PropertySubject("c", "p"), signature = property).signature)
    }

    @Test
    fun `receiver port on a non-method subject is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ModelEntry(FunctionSubject("f"), body = flow(Port.Receiver, Port.Return))
        }
        assertFailsWith<IllegalArgumentException> {
            ModelEntry(FunctionSubject("f"), body = flow(Port.Argument(0), Port.Receiver))
        }
        val method = ModelEntry(MethodSubject("c", "m"), body = flow(Port.Receiver, Port.Return))
        val pair = method.body.propagation?.single()
        assertEquals(Port.Receiver, pair?.from)
    }

    @Test
    fun `condition on a non-callable subject is rejected`() {
        val condition = ArgPattern(listOf(BoolVal(true)))
        assertFailsWith<IllegalArgumentException> { ModelEntry(ConstantSubject("K"), condition, typed) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(VariableSubject("_GET"), condition, body = sources) }
    }

    @Test
    fun `explicit source site on a non-callable subject is rejected`() {
        val sited = ModelBody(sources = listOf(SourceDecl(setOf(OriginId("remote")), at = Port.Argument(0))))
        assertFailsWith<IllegalArgumentException> { ModelEntry(VariableSubject("_GET"), body = sited) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(PropertySubject("c", "p"), body = sited) }
    }

    @Test
    fun `non-callable kinds admit a sources section`() {
        val subjects =
            listOf(
                ConstantSubject("K"),
                ClassConstantSubject("c", "K"),
                PropertySubject("c", "p"),
                VariableSubject("_GET"),
            )
        for (subject in subjects) {
            assertEquals(sources, ModelEntry(subject, body = sources).body)
        }
    }

    @Test
    fun `non-callable kind rejects returns`() {
        assertFailsWith<IllegalArgumentException> { ModelEntry(ConstantSubject("K"), body = returns) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(PropertySubject("c", "p"), body = returns) }
        assertFailsWith<IllegalArgumentException> { ModelEntry(VariableSubject("_GET"), body = returns) }
    }

    @Test
    fun `class subject rejects every assertion section`() {
        val sinks = ModelBody(sinks = listOf(SinkDecl(Port.Argument(0), VulnClassId("sqli"))))
        val sanitizers = ModelBody(sanitizers = listOf(SanitizerDecl(setOf(VulnClassId("sqli")))))
        for (body in listOf(sources, returns, sinks, sanitizers)) {
            assertFailsWith<IllegalArgumentException> {
                ModelEntry(ClassSubject("c"), signature = classSignature, body = body)
            }
        }
    }

    @Test
    fun `entries compare by value`() {
        assertEquals(ModelEntry(FunctionSubject("F"), body = returns), ModelEntry(FunctionSubject("f"), body = returns))
        val f = ModelEntry(FunctionSubject("f"), body = returns)
        assertNotEquals(f, ModelEntry(FunctionSubject("g"), body = returns))
        assertNotEquals(
            ModelEntry(FunctionSubject("f"), body = returns),
            ModelEntry(FunctionSubject("f"), ArgPattern(listOf(BoolVal(true))), body = returns),
        )
    }
}
