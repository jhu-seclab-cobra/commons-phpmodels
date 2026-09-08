package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The one decoder's strictness (design.md "Loaders", impl.md), checked on
 * plain shapes so each rule is observed without an entry around it.
 *
 * - `decodes utf-8 content` — a non-ASCII scalar round-trips.
 * - `unknown key is rejected with the underlying reason` — the failure is an
 *   [IllegalArgumentException] carrying the decode failure as cause and its
 *   reason in the message.
 * - `enum constant decodes case-insensitively` — the lowercase file
 *   vocabulary maps onto the uppercase constants.
 * - `unknown enum constant is rejected` — case folding admits no new spelling.
 * - `duplicate key is rejected` — a doubled key never decodes last-wins.
 * - `second document is rejected` — one document per stream.
 * - `alias is rejected naming the anchor` — an alias is never substituted.
 * - `malformed syntax is rejected` — a parse failure is a load failure.
 * - `narrow keeps the unknown-key strictness` — the tree-to-value path is
 *   as strict as the stream path.
 */
internal class ModelYamlTest {
    private fun strings(yaml: String): Map<String, String> =
        ModelYaml.decode(yaml.byteInputStream(), jacksonTypeRef<Map<String, String>>())

    private fun kinds(yaml: String): List<ReturnKind> =
        ModelYaml.decode(yaml.byteInputStream(), jacksonTypeRef<List<ReturnKind>>())

    private fun provenance(yaml: String): SetProvenance =
        ModelYaml.decode(yaml.byteInputStream(), jacksonTypeRef<SetProvenance>())

    @Test
    fun `decodes utf-8 content`() {
        assertEquals(mapOf("name" to "café 中"), strings("name: café 中\n"))
    }

    @Test
    fun `unknown key is rejected with the underlying reason`() {
        val failure =
            assertFailsWith<IllegalArgumentException> { provenance("producer: x\nverification: manual\nnote: y\n") }
        assertIs<JsonProcessingException>(failure.cause)
        val message = failure.message.orEmpty()
        assertTrue(message.startsWith("Malformed model document"), message)
        assertTrue("note" in message, message)
    }

    @Test
    fun `enum constant decodes case-insensitively`() {
        assertEquals(listOf(ReturnKind.STR, ReturnKind.NUM, ReturnKind.BOOL), kinds("[str, Num, BOOL]"))
    }

    @Test
    fun `unknown enum constant is rejected`() {
        assertFailsWith<IllegalArgumentException> { kinds("[text]") }
    }

    @Test
    fun `duplicate key is rejected`() {
        assertFailsWith<IllegalArgumentException> { strings("a: 1\na: 2\n") }
    }

    @Test
    fun `second document is rejected`() {
        assertFailsWith<IllegalArgumentException> { strings("a: 1\n---\na: 2\n") }
    }

    @Test
    fun `alias is rejected naming the anchor`() {
        val failure = assertFailsWith<IllegalArgumentException> { strings("a: &v x\nb: *v\n") }
        assertTrue("*v" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `malformed syntax is rejected`() {
        assertFailsWith<IllegalArgumentException> { strings("a: [\n") }
    }

    @Test
    fun `narrow keeps the unknown-key strictness`() {
        val node = ModelYaml.decode("type: string\nstray: 1\n".byteInputStream(), jacksonTypeRef<JsonNode>())
        assertFailsWith<JsonProcessingException> { ModelYaml.narrow(node, SignatureInfo.TypedSignature::class.java) }
        val sound = ModelYaml.decode("type: string\n".byteInputStream(), jacksonTypeRef<JsonNode>())
        assertEquals(
            SignatureInfo.TypedSignature(DeclaredType("string")),
            ModelYaml.narrow(sound, SignatureInfo.TypedSignature::class.java),
        )
    }
}
