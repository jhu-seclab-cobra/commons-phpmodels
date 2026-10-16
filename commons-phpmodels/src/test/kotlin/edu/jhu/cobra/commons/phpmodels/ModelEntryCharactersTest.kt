package edu.jhu.cobra.commons.phpmodels

import java.util.regex.PatternSyntaxException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The characters section: its decode, its place in the value-semantics unit,
 * and the character class membership (model.md, design-bodies.md).
 *
 * - `characters decodes of and within` — `basename` copies bytes of
 *   argument(0) and never a `/`.
 * - `characters is served with the value-semantics unit` — the lookup view
 *   carries the bound; an absent section carries none.
 * - `characters beside a callable signature joins the derived returns` —
 *   the propagation completion covers the whole unit.
 * - `characters without returns is rejected` — explicitly or beside a
 *   signature that declares no propagation.
 * - `characters of a port with no flow into the result is rejected` — a
 *   copied byte is a flow; a flow into another port does not count.
 * - `characters declaring neither field is rejected` — an empty bound
 *   asserts nothing; an empty `of` set likewise.
 * - `characters on a non-callable subject is rejected` — only callable
 *   results have bytes to bound.
 * - `character class holds the bytes its pattern matches` — each byte read
 *   as one Latin-1 character, the high bytes included.
 * - `character classes compare by pattern text` — equality, parse, and
 *   rejection of an invalid pattern.
 */
internal class ModelEntryCharactersTest {
    @Test
    fun `characters decodes of and within`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: basename
                  returns: str
                  propagation:
                    - from: argument(0)
                      to: return
                  characters:
                    of: [argument(0)]
                    within: "[^/]"
                """.trimIndent(),
            )
        assertEquals(
            CharacterBound(of = setOf(Port.Argument(0)), within = CharacterClass("[^/]")),
            entry.body.characters,
        )
    }

    @Test
    fun `characters is served with the value-semantics unit`() {
        val flow = listOf(Propagation(from = Port.Argument(0), to = Port.Return))
        val bound = CharacterBound(of = setOf(Port.Argument(0)))
        val body = ModelBody(returns = ReturnKind.STR, propagation = flow, characters = bound)
        assertEquals(ValueSemantics(ReturnKind.STR, flow, bound), body.valueSemantics())
        assertNull(ModelBody(returns = ReturnKind.STR, propagation = flow).valueSemantics()?.characters)
    }

    @Test
    fun `characters beside a callable signature joins the derived returns`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: substr
                  signature:
                    params:
                      - name: string
                        type: string
                      - name: offset
                        type: int
                    returnType: string
                  propagation:
                    - from: argument(0)
                      to: return
                  characters:
                    of: [argument(0)]
                """.trimIndent(),
            )
        assertEquals(ReturnKind.STR, entry.body.returns)
        assertEquals(setOf(Port.Argument(0)), entry.body.characters?.of)
    }

    @Test
    fun `characters without returns is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ModelBody(characters = CharacterBound(within = CharacterClass("[a-z]")))
        }
        assertFailsWith<IllegalArgumentException> {
            loadModel(
                """
                - subject:
                    function: basename
                  signature:
                    params:
                      - name: path
                        type: string
                    returnType: string
                  characters:
                    within: "[^/]"
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `characters of a port with no flow into the result is rejected`() {
        val intoResult = listOf(Propagation(from = Port.Argument(0), to = Port.Return))
        val intoArgument = listOf(Propagation(from = Port.Argument(1), to = Port.Argument(0)))
        assertFailsWith<IllegalArgumentException> {
            ModelBody(returns = ReturnKind.STR, characters = CharacterBound(of = setOf(Port.Argument(0))))
        }
        assertFailsWith<IllegalArgumentException> {
            ModelBody(
                returns = ReturnKind.STR,
                propagation = intoResult,
                characters = CharacterBound(of = setOf(Port.Argument(0), Port.Argument(1))),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ModelBody(
                returns = ReturnKind.STR,
                propagation = intoArgument,
                characters = CharacterBound(of = setOf(Port.Argument(1))),
            )
        }
    }

    @Test
    fun `characters declaring neither field is rejected`() {
        assertFailsWith<IllegalArgumentException> { CharacterBound() }
        assertFailsWith<IllegalArgumentException> { CharacterBound(of = emptySet()) }
    }

    @Test
    fun `characters on a non-callable subject is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            loadModel(
                """
                - subject:
                    constant: PHP_EOL
                  returns: str
                  characters:
                    within: "[\\r\\n]"
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `character class holds the bytes its pattern matches`() {
        val noSlash = CharacterClass("[^/]")
        assertFalse(noSlash.contains('/'.code))
        assertTrue(noSlash.contains('.'.code))
        assertTrue(noSlash.contains(0xFF))
        val hex = CharacterClass("[0-9a-f]")
        assertTrue(hex.contains('a'.code))
        assertFalse(hex.contains('A'.code))
        assertFalse(hex.contains(0xE9))
    }

    @Test
    fun `character classes compare by pattern text`() {
        assertEquals(CharacterClass("[^/]"), CharacterClass.parse("[^/]"))
        assertEquals(CharacterClass("[^/]").hashCode(), CharacterClass("[^/]").hashCode())
        assertFailsWith<PatternSyntaxException> { CharacterClass("[") }
    }
}
