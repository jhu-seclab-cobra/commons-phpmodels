package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Document-stream and YAML-text strictness of [ModelLoader]: what the loader
 * accepts at the document level, and the closed vocabularies it enforces
 * outside the subject and port grammars. Entry forms: [ModelLoaderTest].
 *
 * - `empty stream is rejected` — no document is a load failure, not an empty
 *   list.
 * - `root mapping is rejected` — the document root is a sequence of entries.
 * - `empty sequence loads as the empty list` — `[]` pins the current
 *   behavior; the docs do not fix this case.
 * - `merge key is rejected` — `<<` is a key outside the entry's closed field
 *   set.
 * - `unknown returns vocabulary is rejected` — `returns` admits the closed
 *   kind set only.
 * - `unknown find kind is rejected` — `find` admits the closed generator
 *   kind set only.
 * - `unknown constraint discriminator is rejected` — the `where` list admits
 *   the closed constraint set only.
 * - `two entries for one subject load as branches` — the loader never
 *   deduplicates; entries are branches in declaration order.
 * - `duplicate generator names load` — name uniqueness is the caller's
 *   check, not the loader's.
 * - `alias value is rejected` — an alias never substitutes an anchored
 *   value silently.
 * - `block scalar spelling is rejected` — a literal block scalar carries a
 *   trailing newline that is not part of any identity spelling.
 */
internal class ModelLoaderDocumentTest {
    @Test
    fun `empty stream is rejected`() {
        assertFailsWith<IllegalArgumentException> { load("") }
    }

    @Test
    fun `root mapping is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                subject:
                  function: strlen
                returns: num
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `empty sequence loads as the empty list`() {
        assertEquals(emptyList(), load("[]"))
    }

    @Test
    fun `merge key is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  returns: num
                  <<:
                    returns: str
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `unknown returns vocabulary is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: strlen
                  returns: text
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `unknown find kind is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - name: traversables
                  find: class
                  where:
                    - constraint: name
                      pattern: .*iterator
                  model:
                    returns: any
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `unknown constraint discriminator is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - name: mysqli-queries
                  find: method
                  where:
                    - constraint: owner
                      pattern: mysqli.*
                  model:
                    returns: any
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `two entries for one subject load as branches`() {
        val entries =
            load(
                """
                - subject:
                    function: json_decode
                  returns: str
                - subject:
                    function: json_decode
                  when:
                    port: argument(1)
                    is: true
                  returns: any
                """.trimIndent(),
            )
        assertEquals(2, entries.size)
        assertEquals(
            List(2) { FunctionSubject("json_decode") },
            entries.map { assertIs<SubjectModel>(it).subject },
        )
    }

    @Test
    fun `duplicate generator names load`() {
        val entries =
            load(
                """
                - name: superglobals
                  find: variable
                  where:
                    - constraint: name
                      pattern: _get
                  model:
                    sources:
                      - provenance: [remote]
                - name: superglobals
                  find: variable
                  where:
                    - constraint: name
                      pattern: _post
                  model:
                    sources:
                      - provenance: [remote]
                """.trimIndent(),
            )
        assertEquals(
            List(2) { "superglobals" },
            entries.map { assertIs<ModelGenerator>(it).name },
        )
    }

    @Test
    fun `alias value is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: &esc addslashes
                  returns: str
                - subject:
                    function: *esc
                  returns: str
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `block scalar spelling is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load(
                """
                - subject:
                    function: |
                      strlen
                  returns: num
                """.trimIndent(),
            )
        }
    }
}
