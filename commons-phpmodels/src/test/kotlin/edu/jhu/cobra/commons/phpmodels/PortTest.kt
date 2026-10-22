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
 *   arguments, the receiver and a callback return only.
 * - `parse decodes the callback spellings` — `callback(argument(n)).parameter(m)`
 *   and `callback(argument(n)).return`.
 * - `parseInput admits the callback return only` — the callback parameter is a
 *   to-side port.
 * - `parse rejects near-miss callback spellings` — a missing member, a bare or
 *   negative position, nesting, whitespace, off-case keyword and an Int overflow.
 * - `callback ports reject a negative position` — the constructor bounds.
 * - `callback ports spell themselves as parsed` — toString is the parse spelling.
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

    @Test
    fun `parse decodes the callback spellings`() {
        assertEquals(Port.CallbackParameter(0, 1), Port.parse("callback(argument(0)).parameter(1)"))
        assertEquals(Port.CallbackReturn(2), Port.parse("callback(argument(2)).return"))
        assertEquals(
            Port.CallbackParameter(Int.MAX_VALUE, Int.MAX_VALUE),
            Port.parse("callback(argument(2147483647)).parameter(2147483647)"),
        )
    }

    @Test
    fun `parseInput admits the callback return only`() {
        assertEquals(Port.CallbackReturn(0), Port.Input.parseInput("callback(argument(0)).return"))
        assertFailsWith<IllegalArgumentException> {
            Port.Input.parseInput("callback(argument(0)).parameter(1)")
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "callback(argument(0))",
            "callback(argument(0)).parameter",
            "callback(argument(0)).parameter()",
            "callback(argument(0)).parameter(-1)",
            "callback(argument(0)).parameter(2147483648)",
            "callback(0).parameter(1)",
            "callback(this).return",
            "callback(return).return",
            "callback(argument(0)).Return",
            "Callback(argument(0)).return",
            "callback(argument(0)) .return",
            "callback(argument(-1)).return",
            "callback(argument(2147483648)).return",
            "callback(callback(argument(0)).return).return",
            "callback(argument(0)).return.parameter(1)",
        ],
    )
    fun `parse rejects near-miss callback spellings`(raw: String) {
        assertFailsWith<IllegalArgumentException> { Port.parse(raw) }
    }

    @Test
    fun `callback ports reject a negative position`() {
        assertFailsWith<IllegalArgumentException> { Port.CallbackParameter(-1, 0) }
        assertFailsWith<IllegalArgumentException> { Port.CallbackParameter(0, -1) }
        assertFailsWith<IllegalArgumentException> { Port.CallbackReturn(-1) }
    }

    @Test
    fun `callback ports spell themselves as parsed`() {
        for (spelling in listOf("callback(argument(0)).parameter(1)", "callback(argument(3)).return")) {
            assertEquals(spelling, Port.parse(spelling).toString())
        }
    }
}
