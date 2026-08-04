package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Validation and derived views of [Propagation] and [ModelBody].
 *
 * - `propagation accepts either synonym spelling per side` — from/input and
 *   to/output decode to one pair.
 * - `propagation rejects a doubled or missing side` — synonym pairs are
 *   exclusive and one side is required.
 * - `propagation rejects a self-targeting flow` — to must differ from from.
 * - `empty body is constructible` — a signature-only entry carries one.
 * - `propagation requires returns` — the value-semantics unit is whole.
 * - `declared empty section is rejected` — an empty list is a mistake, not
 *   an absent section.
 * - `declaresOnlySources reflects the section set` — true for sources alone.
 * - `valueSemantics completes absent propagation to the empty list` — no
 *   flow, not unknown flow; null when returns is undeclared.
 * - `equal propagations agree on hash code and spelling` — equality over the
 *   resolved pair, hash-code agreement, and the toString form.
 */
internal class ModelBodyTest {
    @Test
    fun `propagation accepts either synonym spelling per side`() {
        val plain = Propagation(from = Port.Argument(0), to = Port.Return)
        val synonym = Propagation(input = Port.Argument(0), output = Port.Return)
        assertEquals(plain, synonym)
    }

    @Test
    fun `propagation rejects a doubled or missing side`() {
        assertFailsWith<IllegalArgumentException> {
            Propagation(from = Port.Argument(0), input = Port.Argument(1), to = Port.Return)
        }
        assertFailsWith<IllegalArgumentException> { Propagation(from = Port.Argument(0)) }
    }

    @Test
    fun `propagation rejects a self-targeting flow`() {
        assertFailsWith<IllegalArgumentException> {
            Propagation(from = Port.Argument(0), to = Port.Argument(0))
        }
    }

    @Test
    fun `equal propagations agree on hash code and spelling`() {
        val plain = Propagation(from = Port.Argument(0), to = Port.Return)
        val synonym = Propagation(input = Port.Argument(0), output = Port.Return)
        assertEquals(plain.hashCode(), synonym.hashCode())
        assertEquals("Propagation(from=argument(0), to=return)", plain.toString())
    }

    @Test
    fun `empty body is constructible`() {
        assertEquals(true, ModelBody().isEmpty)
    }

    @Test
    fun `propagation requires returns`() {
        assertFailsWith<IllegalArgumentException> {
            ModelBody(propagation = listOf(Propagation(from = Port.Argument(0), to = Port.Return)))
        }
    }

    @Test
    fun `declared empty section is rejected`() {
        assertFailsWith<IllegalArgumentException> { ModelBody(sources = emptyList()) }
        assertFailsWith<IllegalArgumentException> { ModelBody(returns = ReturnKind.ANY, propagation = emptyList()) }
    }

    @Test
    fun `declaresOnlySources reflects the section set`() {
        val sources = listOf(SourceDecl(setOf(ProvenanceId("remote"))))
        assertEquals(true, ModelBody(sources = sources).declaresOnlySources)
        assertEquals(false, ModelBody(returns = ReturnKind.ANY, sources = sources).declaresOnlySources)
        assertEquals(false, ModelBody().declaresOnlySources)
    }

    @Test
    fun `valueSemantics completes absent propagation to the empty list`() {
        assertEquals(
            ValueSemantics(ReturnKind.STR, emptyList()),
            ModelBody(returns = ReturnKind.STR).valueSemantics(),
        )
        assertNull(ModelBody(sources = listOf(SourceDecl(setOf(ProvenanceId("remote"))))).valueSemantics())
    }
}
