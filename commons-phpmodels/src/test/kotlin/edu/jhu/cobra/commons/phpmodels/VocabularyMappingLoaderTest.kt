package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Decoding of the mapping document shape (design-sets.md, impl.md).
 *
 * - `decodes both axes with ignore as discard` — the literal maps to null,
 *   on the vulnerability type axis and on the source kind axis.
 * - `IGNORE is the discard literal` — the constant the format names.
 * - `empty sections decode to empty tables` — `{}` lists nothing.
 * - `rejects …` — null target, missing section, stray key, and a source
 *   spelled `ignore` fail the decode.
 */
internal class VocabularyMappingLoaderTest {
    private fun load(yaml: String): VocabularyMapping = VocabularyMappingLoader.load(yaml.byteInputStream())

    @Test
    fun `decodes both axes with ignore as discard`() {
        val mapping =
            load(
                """
                vulnTypes:
                  sql: sqli
                  text: ignore
                sourceKinds:
                  input: user-input
                  env: ignore
                """.trimIndent(),
            )
        assertEquals(VulnTypeId("sqli"), mapping.vulnType(VulnTypeId("sql")))
        assertNull(mapping.vulnType(VulnTypeId("text")))
        assertEquals(SourceKindId("user-input"), mapping.sourceKind(SourceKindId("input")))
        assertNull(mapping.sourceKind(SourceKindId("env")))
    }

    @Test
    fun `IGNORE is the discard literal`() {
        assertEquals("ignore", VocabularyMappingLoader.IGNORE)
    }

    @Test
    fun `empty sections decode to empty tables`() {
        assertEquals(VocabularyMapping(emptyMap(), emptyMap()), load("vulnTypes: {}\nsourceKinds: {}\n"))
    }

    @Test
    fun `folds names to lowercase`() {
        val mapping = load("vulnTypes:\n  SQL: SQLi\nsourceKinds: {}\n")
        assertEquals(VulnTypeId("sqli"), mapping.vulnType(VulnTypeId("sql")))
    }

    @Test
    fun `rejects a null target`() {
        assertFailsWith<IllegalArgumentException> { load("vulnTypes:\n  sql:\nsourceKinds: {}\n") }
    }

    @Test
    fun `rejects a missing section`() {
        assertFailsWith<IllegalArgumentException> { load("vulnTypes:\n  sql: sqli\n") }
    }

    @Test
    fun `rejects a stray key`() {
        assertFailsWith<IllegalArgumentException> { load("vulnTypes: {}\nsourceKinds: {}\nstray: {}\n") }
    }

    @Test
    fun `rejects ignore as a source name`() {
        assertFailsWith<IllegalArgumentException> { load("vulnTypes:\n  ignore: sqli\nsourceKinds: {}\n") }
    }
}
