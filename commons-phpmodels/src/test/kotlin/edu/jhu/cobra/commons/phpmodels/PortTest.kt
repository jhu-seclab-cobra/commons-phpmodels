package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * String spellings of the [Port] vocabulary.
 *
 * - `parse decodes the three spellings` — `return`, `this`, and `argument(n)`.
 * - `parse rejects other spellings` — bare integers, negatives, blanks.
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
