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
 * - `parse rejects other spellings` — bare integers, negatives, blanks.
 * - `parse rejects near-miss argument spellings` — unclosed parenthesis,
 *   empty index, non-digit index, whitespace, off-case keyword, and an index
 *   past the Int range; nothing wraps or trims silently.
 * - `parseArgument rejects the return port` — the narrowing creator admits
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
    fun `parse rejects other spellings`() {
        assertFailsWith<IllegalArgumentException> { Port.parse("0") }
        assertFailsWith<IllegalArgumentException> { Port.parse("argument(-1)") }
        assertFailsWith<IllegalArgumentException> { Port.parse("result") }
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["argument(0", "argument()", "argument(x)", "argument(1 )", "Argument(1)", "argument(2147483648)"],
    )
    fun `parse rejects near-miss argument spellings`(raw: String) {
        assertFailsWith<IllegalArgumentException> { Port.parse(raw) }
    }

    @Test
    fun `parseArgument rejects the return port`() {
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
