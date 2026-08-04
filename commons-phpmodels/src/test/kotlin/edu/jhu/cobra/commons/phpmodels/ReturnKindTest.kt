package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Truth table of the [ReturnKind.join] lattice operation.
 *
 * - `join of a kind with itself is identity` — every diagonal entry of the
 *   truth table returns the kind unchanged.
 * - `join of distinct kinds is ANY` — every off-diagonal entry collapses to
 *   the top classification.
 */
internal class ReturnKindTest {
    @Test
    fun `join of a kind with itself is identity`() {
        for (kind in ReturnKind.entries) {
            assertEquals(kind, kind.join(kind), "join($kind, $kind)")
        }
    }

    @Test
    fun `join of distinct kinds is ANY`() {
        for (left in ReturnKind.entries) {
            for (right in ReturnKind.entries) {
                if (left == right) continue
                assertEquals(ReturnKind.ANY, left.join(right), "join($left, $right)")
            }
        }
    }
}
