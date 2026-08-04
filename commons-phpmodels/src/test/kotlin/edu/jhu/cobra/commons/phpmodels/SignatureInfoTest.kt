package edu.jhu.cobra.commons.phpmodels

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Declared-type vocabulary and signature field validation.
 *
 * - `declared type accepts keywords and class names` — keyword spellings and
 *   namespaced class names construct.
 * - `declared type rejects other spellings` — punctuation and blanks fail.
 * - `declared type derives its return classification` — the four-kind
 *   derivation table.
 * - `class signature folds parent and interfaces` — inheritance edges compare
 *   case-insensitively.
 * - `class signature rejects blank inheritance names` — a blank parent or
 *   interface fails.
 * - `parameter rejects a blank name` — parameter identity is non-blank.
 * - `class signatures compare by folded fields` — equality and hash code
 *   over classifier, folded parent, and folded interfaces.
 * - `class signature spells its fields` — the toString form.
 */
internal class SignatureInfoTest {
    @Test
    fun `declared type accepts keywords and class names`() {
        DeclaredType("string")
        DeclaredType("mysqli")
        DeclaredType("\\Foo\\Bar")
    }

    @Test
    fun `declared type rejects other spellings`() {
        assertFailsWith<IllegalArgumentException> { DeclaredType("") }
        assertFailsWith<IllegalArgumentException> { DeclaredType("int|string") }
        assertFailsWith<IllegalArgumentException> { DeclaredType("?int") }
    }

    @ParameterizedTest
    @CsvSource("string, STR", "int, NUM", "float, NUM", "bool, BOOL", "array, ANY", "mysqli, ANY")
    fun `declared type derives its return classification`(
        raw: String,
        expected: ReturnKind,
    ) {
        assertEquals(expected, DeclaredType(raw).toReturnKind())
    }

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
}
