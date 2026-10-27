package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Handler fact types and their validation, derived from the declared handler
 * design.
 *
 * - `request vocabulary has the 22 members of two slots` — CAST holds seven,
 *   OPERATION fifteen; each member knows its slot.
 * - `compare has no request member` — no member belongs to COMPARE.
 * - `slot and request decode from lowercase names` — closed enums.
 * - `target requires a non-blank symbol` — identity is source-qualified.
 * - `present cast to string requires a dispatch` — only IS_STRING states one.
 * - `other requests take no dispatch` — a dispatch beside any other request.
 * - `compare takes a null request, cast and operation require one`.
 * - `a request belongs to its slot` — on absent facts too.
 * - `an absent fact has no targets; a present fact has at least one`.
 * - `a target may hold an empty result set` — no normal return.
 * - `table rejects a duplicate slot and request`.
 * - `table rejects absent beside present in one slot, whatever the request`.
 * - `a whole-slot absence is the sole fact of its slot`.
 * - `opposite presence across tables is valid` — presence is per table.
 * - `table id is non-blank`.
 * - `positive absence, empty results and missing coverage stay distinct`.
 */
internal class HandlerFactTest {
    private fun target(
        results: Set<ReturnKind> = setOf(ReturnKind.ANY),
        dispatch: StringDispatch? = null,
    ) = HandlerTarget("a.c:handler", results, dispatch)

    private fun present(
        slot: HandlerSlot,
        request: HandlerRequest?,
        dispatch: StringDispatch? = null,
    ) = HandlerFact(slot, request, listOf(target(dispatch = dispatch)), absent = false)

    private fun absent(
        slot: HandlerSlot,
        request: HandlerRequest? = null,
    ) = HandlerFact(slot, request, emptyList(), absent = true)

    @Test
    fun `request vocabulary has the 22 members of two slots`() {
        assertEquals(22, HandlerRequest.entries.size)
        val cast = HandlerRequest.entries.filter { it.slot == HandlerSlot.CAST }.map { it.name }
        assertEquals(
            listOf("IS_STRING", "IS_LONG", "IS_DOUBLE", "_IS_BOOL", "_IS_NUMBER", "IS_ARRAY", "IS_RESOURCE"),
            cast,
        )
        val operation = HandlerRequest.entries.filter { it.slot == HandlerSlot.OPERATION }.map { it.name }
        assertEquals(
            listOf(
                "ZEND_ADD", "ZEND_SUB", "ZEND_MUL", "ZEND_DIV", "ZEND_MOD", "ZEND_POW", "ZEND_SL", "ZEND_SR",
                "ZEND_BW_OR", "ZEND_BW_AND", "ZEND_BW_XOR", "ZEND_BW_NOT", "ZEND_CONCAT", "ZEND_BOOL_XOR",
                "ZEND_BOOL_NOT",
            ),
            operation,
        )
    }

    @Test
    fun `compare has no request member`() {
        assertTrue(HandlerRequest.entries.none { it.slot == HandlerSlot.COMPARE })
        assertEquals(setOf(HandlerSlot.CAST, HandlerSlot.COMPARE, HandlerSlot.OPERATION), HandlerSlot.entries.toSet())
    }

    @Test
    fun `dispatch order has four members`() {
        assertEquals(
            listOf("NONE", "METHOD_ONLY", "METHOD_FIRST", "NATIVE_FIRST"),
            StringDispatch.entries.map { it.name },
        )
    }

    @Test
    fun `target requires a non-blank symbol`() {
        assertFailsWith<IllegalArgumentException> { HandlerTarget(" ", emptySet()) }
    }

    @Test
    fun `present cast to string requires a dispatch`() {
        assertFailsWith<IllegalArgumentException> { present(HandlerSlot.CAST, HandlerRequest.IS_STRING) }
        present(HandlerSlot.CAST, HandlerRequest.IS_STRING, StringDispatch.NONE)
    }

    @Test
    fun `other requests take no dispatch`() {
        assertFailsWith<IllegalArgumentException> {
            present(HandlerSlot.CAST, HandlerRequest.IS_LONG, StringDispatch.NONE)
        }
        assertFailsWith<IllegalArgumentException> {
            present(HandlerSlot.OPERATION, HandlerRequest.ZEND_ADD, StringDispatch.METHOD_ONLY)
        }
        assertFailsWith<IllegalArgumentException> { present(HandlerSlot.COMPARE, null, StringDispatch.NONE) }
    }

    @Test
    fun `compare takes a null request, cast and operation require one`() {
        present(HandlerSlot.COMPARE, null)
        assertFailsWith<IllegalArgumentException> { present(HandlerSlot.CAST, null) }
        assertFailsWith<IllegalArgumentException> { present(HandlerSlot.OPERATION, null) }
    }

    @Test
    fun `a request belongs to its slot`() {
        assertFailsWith<IllegalArgumentException> { present(HandlerSlot.OPERATION, HandlerRequest.IS_LONG) }
        assertFailsWith<IllegalArgumentException> { absent(HandlerSlot.CAST, HandlerRequest.ZEND_ADD) }
        assertFailsWith<IllegalArgumentException> { absent(HandlerSlot.COMPARE, HandlerRequest.IS_LONG) }
    }

    @Test
    fun `an absent whole slot permits a null request`() {
        absent(HandlerSlot.CAST)
        absent(HandlerSlot.OPERATION)
        absent(HandlerSlot.COMPARE)
        absent(HandlerSlot.OPERATION, HandlerRequest.ZEND_ADD)
    }

    @Test
    fun `an absent fact has no targets and a present fact has one`() {
        assertFailsWith<IllegalArgumentException> {
            HandlerFact(HandlerSlot.COMPARE, null, listOf(target()), absent = true)
        }
        assertFailsWith<IllegalArgumentException> {
            HandlerFact(HandlerSlot.COMPARE, null, emptyList(), absent = false)
        }
    }

    @Test
    fun `a target may hold an empty result set`() {
        val fact = HandlerFact(HandlerSlot.COMPARE, null, listOf(target(emptySet())), absent = false)
        assertTrue(fact.targets.single().results.isEmpty())
    }

    @Test
    fun `table rejects a duplicate slot and request`() {
        val add = present(HandlerSlot.OPERATION, HandlerRequest.ZEND_ADD)
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(add, add)) }
        val compare = present(HandlerSlot.COMPARE, null)
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(compare, compare)) }
    }

    @Test
    fun `table rejects absent beside present in one slot in both orders`() {
        val absentAll = absent(HandlerSlot.OPERATION)
        val addPresent = present(HandlerSlot.OPERATION, HandlerRequest.ZEND_ADD)
        val subAbsent = absent(HandlerSlot.OPERATION, HandlerRequest.ZEND_SUB)
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(absentAll, addPresent)) }
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(addPresent, absentAll)) }
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(subAbsent, addPresent)) }
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(addPresent, subAbsent)) }
    }

    @Test
    fun `a whole-slot absence is the sole fact of its slot`() {
        val absentAll = absent(HandlerSlot.OPERATION)
        val absentAdd = absent(HandlerSlot.OPERATION, HandlerRequest.ZEND_ADD)
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(absentAll, absentAdd)) }
        assertFailsWith<IllegalArgumentException> { HandlerTable("a.c:t", listOf(absentAdd, absentAll)) }
        HandlerTable("a.c:t", listOf(absentAll, present(HandlerSlot.COMPARE, null)))
    }

    @Test
    fun `opposite presence across tables is valid`() {
        val signature =
            SignatureInfo.ClassSignature(
                Classifier.CLASS,
                handlers =
                    listOf(
                        HandlerTable("a.c:one", listOf(absent(HandlerSlot.OPERATION))),
                        HandlerTable("a.c:two", listOf(present(HandlerSlot.OPERATION, HandlerRequest.ZEND_ADD))),
                    ),
            )
        assertEquals(2, signature.handlers.size)
    }

    @Test
    fun `table id is non-blank`() {
        assertFailsWith<IllegalArgumentException> { HandlerTable(" ", emptyList()) }
    }

    @Test
    fun `positive absence, empty results and missing coverage stay distinct`() {
        val missing = HandlerTable("a.c:missing", emptyList())
        val emptyResult = present(HandlerSlot.COMPARE, null).copy(targets = listOf(target(emptySet())))
        val positive = absent(HandlerSlot.COMPARE)
        assertNull(missing.facts.firstOrNull { it.slot == HandlerSlot.COMPARE })
        assertTrue(positive.absent && positive.targets.isEmpty())
        assertTrue(!emptyResult.absent && emptyResult.targets.single().results.isEmpty())
    }
}
