package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Signature field validation.
 *
 * - `class signature folds parent and interfaces` — inheritance edges compare
 *   case-insensitively.
 * - `class signature rejects blank inheritance names` — a blank parent or
 *   interface fails.
 * - `parameter rejects a blank name` — parameter identity is non-blank.
 * - `parameter flags default to false` — optional, by-reference, and
 *   variadic are opt-in.
 * - `typed signature value defaults to null` — a constant without a literal.
 * - `property signature static defaults to false` — instance is the default.
 * - `callable signature params default to empty` — a nullary callable.
 * - `callable signature one-opcode facts default to empty` — no count and no
 *   rope format unless declared.
 * - `callable signature rejects an arity outside its parameters` — a count
 *   below the required parameters or above the list (no variadic).
 * - `variadic signature admits a special arity beyond the list` — the
 *   variadic tail collects every remaining position.
 * - `frameless arities admit counts one to three only` — a count above 3 is
 *   rejected, count 1 is kept.
 * - `special arities admit counts of two or more only` — count 1 is rejected.
 * - `rope format names a plain declared parameter` — undeclared, by-reference
 *   and variadic parameters are rejected.
 * - `callable signature counts the arguments it admits` — the parameter
 *   count, unbounded with a variadic last parameter.
 * - `class signatures compare by folded fields` — equality and hash code
 *   over classifier, folded parent, and folded interfaces.
 * - `class signature spells its fields` — the toString form.
 */
internal class SignatureInfoTest {
    @Test
    fun `class signature folds parent and interfaces`() {
        val signature = SignatureInfo.ClassSignature(Classifier.CLASS, "Base", listOf("Traversable"))
        assertEquals("base", signature.parent)
        assertEquals(listOf("traversable"), signature.interfaces)
    }

    @Test
    fun `class signature rejects blank inheritance names`() {
        assertFailsWith<IllegalArgumentException> { SignatureInfo.ClassSignature(Classifier.CLASS, " ") }
        assertFailsWith<IllegalArgumentException> {
            SignatureInfo.ClassSignature(Classifier.INTERFACE, interfaces = listOf(" "))
        }
    }

    @Test
    fun `parameter rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> { ParameterInfo(" ", DeclaredType("string")) }
    }

    @Test
    fun `class signatures compare by folded fields`() {
        val folded = SignatureInfo.ClassSignature(Classifier.CLASS, "Base", listOf("Traversable"))
        val spelled = SignatureInfo.ClassSignature(Classifier.CLASS, "base", listOf("traversable"))
        assertEquals(spelled, folded)
        assertEquals(spelled.hashCode(), folded.hashCode())
        assertNotEquals(SignatureInfo.ClassSignature(Classifier.CLASS, "other", listOf("traversable")), folded)
        assertNotEquals(
            SignatureInfo.ClassSignature(Classifier.INTERFACE),
            SignatureInfo.ClassSignature(Classifier.CLASS),
        )
    }

    @Test
    fun `class signature spells its fields`() {
        assertEquals(
            "ClassSignature(classifier=CLASS, parent=base, interfaces=[traversable])",
            SignatureInfo.ClassSignature(Classifier.CLASS, "Base", listOf("Traversable")).toString(),
        )
    }

    @Test
    fun `parameter flags default to false`() {
        val parameter = ParameterInfo("value", DeclaredType("string"))
        assertEquals(false, parameter.optional)
        assertEquals(false, parameter.byRef)
        assertEquals(false, parameter.variadic)
    }

    @Test
    fun `typed signature value defaults to null`() {
        assertEquals(null, SignatureInfo.TypedSignature(DeclaredType("int")).value)
    }

    @Test
    fun `property signature static defaults to false`() {
        assertEquals(false, SignatureInfo.PropertySignature(DeclaredType("int"), Visibility.PRIVATE).static)
    }

    @Test
    fun `callable signature params default to empty`() {
        assertEquals(emptyList(), SignatureInfo.CallableSignature(returnType = DeclaredType("void")).params)
    }

    @Test
    fun `callable signature one-opcode facts default to empty`() {
        val signature = SignatureInfo.CallableSignature(returnType = DeclaredType("void"))
        assertEquals(emptySet(), signature.framelessArities)
        assertEquals(emptySet(), signature.specialArities)
        assertEquals(null, signature.ropeFormat)
    }

    @Test
    fun `callable signature rejects an arity outside its parameters`() {
        val params = listOf(ParameterInfo("a", DeclaredType("string")), ParameterInfo("b", DeclaredType("int"), true))
        val admitted = SignatureInfo.CallableSignature(params, DeclaredType("int"), setOf(1, 2), setOf(2))
        assertEquals(setOf(1, 2), admitted.framelessArities)
        assertEquals(setOf(2), admitted.specialArities)
        assertFailsWith<IllegalArgumentException> {
            SignatureInfo.CallableSignature(params, DeclaredType("int"), setOf(0))
        }
        assertFailsWith<IllegalArgumentException> {
            SignatureInfo.CallableSignature(params, DeclaredType("int"), setOf(3))
        }
        assertFailsWith<IllegalArgumentException> {
            SignatureInfo.CallableSignature(params, DeclaredType("int"), specialArities = setOf(3))
        }
    }

    @Test
    fun `variadic signature admits a special arity beyond the list`() {
        val params = listOf(ParameterInfo("values", DeclaredType("mixed"), optional = true, variadic = true))
        val signature = SignatureInfo.CallableSignature(params, DeclaredType("int"), specialArities = setOf(5))
        assertEquals(setOf(5), signature.specialArities)
    }

    @Test
    fun `frameless arities admit counts one to three only`() {
        val params = List(5) { ParameterInfo("p$it", DeclaredType("int"), optional = true) }
        val admitted = SignatureInfo.CallableSignature(params.take(3), DeclaredType("int"), setOf(1, 3))
        assertEquals(setOf(1, 3), admitted.framelessArities)
        assertFailsWith<IllegalArgumentException> {
            SignatureInfo.CallableSignature(params, DeclaredType("int"), setOf(4))
        }
    }

    @Test
    fun `special arities admit counts of two or more only`() {
        val params = List(3) { ParameterInfo("p$it", DeclaredType("int"), optional = true) }
        assertFailsWith<IllegalArgumentException> {
            SignatureInfo.CallableSignature(params, DeclaredType("int"), specialArities = setOf(1))
        }
        val admitted = SignatureInfo.CallableSignature(params, DeclaredType("int"), specialArities = setOf(2))
        assertEquals(setOf(2), admitted.specialArities)
    }

    @Test
    fun `rope format names a plain declared parameter`() {
        val params =
            listOf(
                ParameterInfo("format", DeclaredType("string")),
                ParameterInfo("ref", DeclaredType("string"), byRef = true),
                ParameterInfo("values", DeclaredType("mixed"), optional = true, variadic = true),
            )
        val admitted = SignatureInfo.CallableSignature(params, DeclaredType("string"), ropeFormat = "format")
        assertEquals("format", admitted.ropeFormat)
        for (bad in listOf("missing", "ref", "values")) {
            assertFailsWith<IllegalArgumentException> {
                SignatureInfo.CallableSignature(params, DeclaredType("string"), ropeFormat = bad)
            }
        }
    }

    @Test
    fun `callable signature counts the arguments it admits`() {
        val fixed = listOf(ParameterInfo("a", DeclaredType("string")), ParameterInfo("b", DeclaredType("int"), true))
        assertEquals(2, SignatureInfo.CallableSignature(fixed, DeclaredType("int")).maxArguments)
        assertEquals(0, SignatureInfo.CallableSignature(returnType = DeclaredType("void")).maxArguments)
        val variadic = listOf(ParameterInfo("values", DeclaredType("mixed"), optional = true, variadic = true))
        assertEquals(Int.MAX_VALUE, SignatureInfo.CallableSignature(variadic, DeclaredType("int")).maxArguments)
    }
}
