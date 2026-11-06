package edu.jhu.cobra.commons.phpmodels

import edu.jhu.cobra.commons.value.BoolVal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * PHP name folding is ASCII-only (`zend_str_tolower`).
 *
 * - `ascii letters fold and non-ascii letters stay` — `ÉX` folds to `Éx`, not `éx`.
 * - `subjects keep non-ascii case distinct` — function, class and method identities fold `A`-`Z` only, so `Éx` and
 *   `éx` are two declarations.
 * - `scalar defaults fold ascii only` — `TRUE` reads as true; `NǓLL` is no scalar.
 */
internal class AsciiFoldTest {
    @Test
    fun `ascii letters fold and non-ascii letters stay`() {
        assertEquals("Éx", "ÉX".asciiFold())
        assertEquals("strlen", "StrLen".asciiFold())
        assertEquals("ſ", "ſ".asciiFold())
    }

    @Test
    fun `subjects keep non-ascii case distinct`() {
        assertEquals("Éx", FunctionSubject("ÉX").name)
        assertNotEquals(FunctionSubject("Éx"), FunctionSubject("éx"))
        assertNotEquals(ClassSubject("Éx"), ClassSubject("éx"))
        assertNotEquals(MethodSubject("Éo", "m"), MethodSubject("éo", "m"))
        assertEquals(MethodSubject("Éo", "M"), MethodSubject("Éo", "m"))
    }

    @Test
    fun `scalar defaults fold ascii only`() {
        assertEquals(BoolVal(true), parsePhpScalarLiteral("TRUE"))
        assertNull(parsePhpScalarLiteral("NULL".replace('U', 'ǔ')))
    }
}
