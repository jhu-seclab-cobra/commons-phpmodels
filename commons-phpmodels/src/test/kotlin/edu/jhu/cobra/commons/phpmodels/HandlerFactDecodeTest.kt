package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * YAML decode of the handler facts, over the fixtures in `handlers/`.
 *
 * - `valid fixture decodes tables, facts and targets` — keys are field names,
 *   enums are lowercase.
 * - `partial tables keep positive absence beside missing coverage`.
 * - `omitted final status and handlers decode as null and empty`.
 * - `rejected fixtures` — each malformed fixture raises
 *   IllegalArgumentException at the load boundary: unknown key, unknown enum,
 *   unknown request, bad dispatch, missing results, missing string dispatch,
 *   duplicate fact, duplicate table id, mixed presence, null element, alias,
 *   duplicate key, a second document.
 */
internal class HandlerFactDecodeTest {
    private fun fixture(name: String): List<ModelEntry> {
        val stream =
            checkNotNull(javaClass.getResourceAsStream("/handlers/$name")) { "Missing fixture $name" }
        return stream.use(ModelLoader::load)
    }

    private fun classSignature(entry: ModelEntry) = assertIs<SignatureInfo.ClassSignature>(entry.signature)

    @Test
    fun `valid fixture decodes tables, facts and targets`() {
        val signature = classSignature(fixture("valid.yaml").single())
        assertEquals(true, signature.isFinal)
        assertEquals(2, signature.handlers.size)
        val first = signature.handlers[0]
        assertEquals("ext/ffi/ffi.c:zend_ffi_cdata_handlers", first.id)
        val cast = first.facts.first { it.request == HandlerRequest.IS_STRING }
        assertEquals(HandlerSlot.CAST, cast.slot)
        assertEquals(StringDispatch.NATIVE_FIRST, cast.targets.single().stringDispatch)
        assertEquals(setOf(ReturnKind.STR), cast.targets.single().results)
        val compare = first.facts.first { it.slot == HandlerSlot.COMPARE }
        assertNull(compare.request)
        assertNull(compare.targets.single().stringDispatch)
        val sub = first.facts.first { it.request == HandlerRequest.ZEND_SUB }
        assertEquals(setOf(ReturnKind.NUM, ReturnKind.ANY), sub.targets.single().results)
        val second = signature.handlers[1]
        val noString = second.facts.first { it.slot == HandlerSlot.CAST }
        assertTrue(noString.targets.single().results.isEmpty())
        assertEquals(StringDispatch.NONE, noString.targets.single().stringDispatch)
        val operation = second.facts.first { it.slot == HandlerSlot.OPERATION }
        assertTrue(operation.absent)
        assertNull(operation.request)
    }

    @Test
    fun `partial tables keep positive absence beside missing coverage`() {
        val signature = classSignature(fixture("partial-tables.yaml").single())
        assertEquals(false, signature.isFinal)
        val standard = signature.handlers[0]
        assertEquals(
            StringDispatch.METHOD_ONLY,
            standard.facts.first { it.request == HandlerRequest.IS_STRING }.targets.single().stringDispatch,
        )
        val truth = standard.facts.first { it.request == HandlerRequest._IS_BOOL }
        assertEquals(setOf(ReturnKind.BOOL), truth.targets.single().results)
        val unresolved = signature.handlers[1]
        assertTrue(unresolved.facts.isEmpty())
    }

    @Test
    fun `omitted final status and handlers decode as null and empty`() {
        val entries =
            ModelLoader.load("- subject:\n    class: plain\n  signature:\n    classifier: class\n".byteInputStream())
        val signature = classSignature(entries.single())
        assertNull(signature.isFinal)
        assertTrue(signature.handlers.isEmpty())
    }

    @Test
    fun `rejected fixtures raise at the load boundary`() {
        val rejected =
            listOf(
                "unknown-key.yaml", "unknown-enum.yaml", "bad-request.yaml", "bad-dispatch.yaml",
                "missing-results.yaml", "missing-string-dispatch.yaml", "duplicate-fact.yaml",
                "duplicate-table.yaml", "mixed-presence.yaml", "null-element.yaml", "alias.yaml",
                "duplicate-key.yaml", "multi-document.yaml",
            )
        for (name in rejected) {
            assertFailsWith<IllegalArgumentException>(name) { fixture(name) }
        }
    }
}
