package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * The value unit holds the set of returns kinds a call may have, and the join
 * of two units is their union (model.md, Model; design-bodies.md, ValueUnit).
 * `print_r($v, $flag)` returns the string when `$flag` is true and `true`
 * otherwise (PHP 8.5.11): a call whose flag is unknown has the kinds
 * {STR, BOOL}.
 *
 * - `a unit built from one kind holds that kind alone` — the kind-set view of
 *   a single declared returns.
 * - `a unit holds a set of kinds` — two kinds are one unit, not `ANY`.
 * - `a unit with no kind is rejected` — the set is non-empty.
 * - `join unions the returns kinds` — `STR` and `BOOL` give `{STR, BOOL}`, and
 *   a kind joined with itself stays one.
 * - `join unions the flows of both units` — each flow once, the first unit's
 *   order first.
 * - `join keeps a character bound both units declare identically` — and drops
 *   it when the bounds differ or one unit has none.
 * - `join is commutative on the kind set and idempotent` — the join of a unit
 *   with itself is the unit.
 */
internal class ValueUnitTest {
    private val argumentToReturn = Propagation(from = Port.Argument(0), to = Port.Return)
    private val receiverToReturn = Propagation(from = Port.Receiver, to = Port.Return)

    @Test
    fun `a unit built from one kind holds that kind alone`() {
        assertEquals(setOf(ReturnKind.STR), ValueUnit(ReturnKind.STR, emptyList()).returns)
        assertEquals(ValueUnit(setOf(ReturnKind.NUM), emptyList()), ValueUnit(ReturnKind.NUM, emptyList()))
    }

    @Test
    fun `a unit holds a set of kinds`() {
        val unit = ValueUnit(setOf(ReturnKind.STR, ReturnKind.BOOL), listOf(argumentToReturn))
        assertEquals(setOf(ReturnKind.STR, ReturnKind.BOOL), unit.returns)
    }

    @Test
    fun `a unit with no kind is rejected`() {
        assertFailsWith<IllegalArgumentException> { ValueUnit(emptySet(), emptyList()) }
    }

    @Test
    fun `join unions the returns kinds`() {
        val text = ValueUnit(ReturnKind.STR, emptyList())
        val flag = ValueUnit(ReturnKind.BOOL, emptyList())
        assertEquals(setOf(ReturnKind.STR, ReturnKind.BOOL), text.join(flag).returns)
        assertEquals(setOf(ReturnKind.STR), text.join(text).returns)
        val number = ValueUnit(ReturnKind.NUM, emptyList())
        assertEquals(setOf(ReturnKind.STR, ReturnKind.BOOL, ReturnKind.NUM), text.join(flag).join(number).returns)
    }

    @Test
    fun `join unions the flows of both units`() {
        val left = ValueUnit(ReturnKind.STR, listOf(argumentToReturn))
        val right = ValueUnit(ReturnKind.BOOL, listOf(receiverToReturn, argumentToReturn))
        assertEquals(listOf(argumentToReturn, receiverToReturn), left.join(right).propagation)
        val none = ValueUnit(ReturnKind.STR, emptyList())
        assertEquals(emptyList(), none.join(none).propagation)
    }

    @Test
    fun `join keeps a character bound both units declare identically`() {
        val bound = CharacterBound(within = CharacterClass("[a-z]"))
        val other = CharacterBound(within = CharacterClass("[0-9]"))

        fun unit(characters: CharacterBound?) = ValueUnit(ReturnKind.STR, emptyList(), characters)
        assertEquals(bound, unit(bound).join(unit(bound)).characters)
        assertNull(unit(bound).join(unit(other)).characters)
        assertNull(unit(bound).join(unit(null)).characters)
        assertNull(unit(null).join(unit(bound)).characters)
        assertNull(unit(null).join(unit(null)).characters)
    }

    @Test
    fun `join is commutative on the kind set and idempotent`() {
        val left = ValueUnit(ReturnKind.STR, listOf(argumentToReturn))
        val right = ValueUnit(ReturnKind.BOOL, listOf(receiverToReturn))
        assertEquals(left.join(right).returns, right.join(left).returns)
        assertEquals(left, left.join(left))
    }
}
