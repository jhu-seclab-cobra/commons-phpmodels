package edu.jhu.cobra.commons.phpmodels

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Declared-type grammar, its union read, and the returns classification.
 *
 * - `declared type reads keywords and class names as one alternative` — a
 *   single type, keyword lowercased, class name folded without its leading
 *   slash.
 * - `nullable type reads as the type or null` — `?T` is `T|null`.
 * - `union reads one alternative per member` — `A|B` keeps member order.
 * - `intersection reads as one alternative` — bare and parenthesized
 *   intersections hold their class names.
 * - `declared type rejects spellings outside the grammar` — each PHP
 *   composition rule php 8.5 rejects at compile time, and malformed text.
 * - `declared type derives its return classification` — the per-alternative
 *   table joined over the alternatives.
 * - `isVoid is true exactly for the void keyword` — the no-result marker the
 *   propagation-into-return rule reads.
 * - `declared type spells its raw text` — toString is the declared spelling.
 */
internal class DeclaredTypeTest {
    @Test
    fun `declared type reads keywords and class names as one alternative`() {
        assertEquals(listOf(setOf("string")), DeclaredType("String").alternatives)
        assertEquals(listOf(setOf("foo\\bar")), DeclaredType("\\Foo\\Bar").alternatives)
    }

    @Test
    fun `nullable type reads as the type or null`() {
        assertEquals(listOf(setOf("mysqli"), setOf("null")), DeclaredType("?mysqli").alternatives)
    }

    @Test
    fun `union reads one alternative per member`() {
        assertEquals(listOf(setOf("int"), setOf("false")), DeclaredType("int|false").alternatives)
    }

    @Test
    fun `intersection reads as one alternative`() {
        assertEquals(listOf(setOf("countable", "traversable")), DeclaredType("Countable&Traversable").alternatives)
        assertEquals(
            listOf(setOf("countable", "traversable"), setOf("null")),
            DeclaredType("(Countable&Traversable)|null").alternatives,
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", "int | string", "?mixed", "?null", "?void", "?never", "?int|string", "int|void",
            "mixed|null", "(A&B)", "A&B|null", "(A)|null", "A&int", "\\int", "int|", "1abc",
        ],
    )
    fun `declared type rejects spellings outside the grammar`(raw: String) {
        assertFailsWith<IllegalArgumentException> { DeclaredType(raw) }
    }

    @ParameterizedTest
    @CsvSource(
        "string, STR",
        "int, NUM",
        "float, NUM",
        "int|float, NUM",
        "bool, BOOL",
        "true, BOOL",
        "false, BOOL",
        "true|false, BOOL",
        "int|false, ANY",
        "?string, ANY",
        "array, ANY",
        "mysqli, ANY",
        "A&B, ANY",
        "void, ANY",
        "never, ANY",
        "self, ANY",
        "static, ANY",
        "mixed, ANY",
        "null, ANY",
    )
    fun `declared type derives its return classification`(
        raw: String,
        expected: ReturnKind,
    ) {
        assertEquals(expected, DeclaredType(raw).toReturnKind())
    }

    @Test
    fun `isVoid is true exactly for the void keyword`() {
        assertEquals(true, DeclaredType("VOID").isVoid)
        assertEquals(false, DeclaredType("string").isVoid)
        assertEquals(false, DeclaredType("never").isVoid)
    }

    @Test
    fun `declared type spells its raw text`() {
        assertEquals("(A&B)|null", DeclaredType("(A&B)|null").toString())
    }
}
