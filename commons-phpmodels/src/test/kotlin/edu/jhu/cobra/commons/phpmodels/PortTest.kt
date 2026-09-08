package edu.jhu.cobra.commons.phpmodels

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * String spellings of the [Port] vocabulary.
 *
 * - `parse decodes the three spellings` — `return`, `this`, and `argument(n)`.
 * - `parse admits the zero and maximum positions` — the lower bound and the
 *   largest Int position.
 * - `parse rejects other spellings` — bare integers, negatives, blanks.
 * - `parse rejects near-miss argument spellings` — unclosed parenthesis,
 *   empty index, non-digit index, whitespace, off-case keyword, and an index
 *   past the Int range; nothing wraps or trims silently.
 * - `argument rejects a negative position` — the constructor bound.
 * - `ports spell themselves as parsed` — toString is the parse spelling.
 * - `parseArgument admits arguments only` — the narrowing creator admits
 *   argument ports only.
 * - `parseInput rejects the return port` — the input-narrowing creator admits
 *   arguments and the receiver only.
 */
internal class PortTest {
    @Test
    fun `parse decodes the three spellings`() {
        assertEquals(Port.Return, Port.parse("return"))
        assertEquals(Port.Receiver, Port.parse("this"))
        assertEquals(Port.Argument(2), Port.parse("argument(2)"))
    }

    @Test
    fun `parse admits the zero and maximum positions`() {
        assertEquals(Port.Argument(0), Port.parse("argument(0)"))
        assertEquals(Port.Argument(Int.MAX_VALUE), Port.parse("argument(2147483647)"))
    }

    @Test
    fun `parse rejects other spellings`() {
        assertFailsWith<IllegalArgumentException> { Port.parse("0") }
        assertFailsWith<IllegalArgumentException> { Port.parse("argument(-1)") }
        assertFailsWith<IllegalArgumentException> { Port.parse("result") }
        assertFailsWith<IllegalArgumentException> { Port.parse("") }
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["argument(0", "argument()", "argument(x)", "argument(1 )", "Argument(1)", "argument(2147483648)"],
    )
    fun `parse rejects near-miss argument spellings`(raw: String) {
        assertFailsWith<IllegalArgumentException> { Port.parse(raw) }
    }

    @Test
    fun `argument rejects a negative position`() {
        assertFailsWith<IllegalArgumentException> { Port.Argument(-1) }
    }

    @Test
    fun `ports spell themselves as parsed`() {
        for (spelling in listOf("return", "this", "argument(0)", "argument(12)")) {
            assertEquals(spelling, Port.parse(spelling).toString())
        }
    }

    @Test
    fun `parseArgument admits arguments only`() {
        assertEquals(Port.Argument(3), Port.Argument.parseArgument("argument(3)"))
        assertFailsWith<IllegalArgumentException> { Port.Argument.parseArgument("return") }
        assertFailsWith<IllegalArgumentException> { Port.Argument.parseArgument("this") }
    }

    @Test
    fun `parseInput rejects the return port`() {
        assertEquals(Port.Receiver, Port.Input.parseInput("this"))
        assertEquals(Port.Argument(0), Port.Input.parseInput("argument(0)"))
        assertFailsWith<IllegalArgumentException> { Port.Input.parseInput("return") }
    }
}
