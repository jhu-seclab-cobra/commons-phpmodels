package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * String spellings of the [Port] vocabulary.
 *
 * - `parse decodes the two spellings` — `return` and `argument(n)`.
 * - `parse rejects other spellings` — bare integers, negatives, blanks.
 * - `parseArgument rejects the return port` — the narrowing creator admits
 *   argument ports only.
 */
internal class PortTest {
    @Test
    fun `parse decodes the two spellings`() {
        assertEquals(Port.Return, Port.parse("return"))
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
    }
}
