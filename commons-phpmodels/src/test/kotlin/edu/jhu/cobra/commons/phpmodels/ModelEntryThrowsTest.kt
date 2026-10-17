package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * The throws section: decoding of its class-name elements and its subject-kind
 * admissibility (design-entries.md).
 *
 * - `throws decodes bare class names folded` — each element is a bare scalar
 *   decoded by the class subject creator: lowercased, leading slash stripped,
 *   with no wrapper key.
 * - `empty throws is a statement` — `throws: []` decodes to an empty set and
 *   the body is not empty.
 * - `absent throws asserts nothing` — the section stays null.
 * - `throws on a method entry decodes` — every callable kind admits it.
 * - `throws on a non-callable subject is rejected` — constant, property, and
 *   class subjects throw nothing.
 * - `throws element outside the class-name spelling is rejected` — a member
 *   spelling is no class.
 * - `throws element in wrapper form is rejected` — the element is a bare
 *   scalar, never a one-key subject mapping.
 */
internal class ModelEntryThrowsTest {
    @Test
    fun `throws decodes bare class names folded`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: json_decode
                  throws: [ValueError, \JsonException]
                """.trimIndent(),
            )
        assertEquals(setOf(ClassSubject("valueerror"), ClassSubject("jsonexception")), entry.body.throws)
    }

    @Test
    fun `empty throws is a statement`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: strlen
                  throws: []
                """.trimIndent(),
            )
        assertEquals(emptySet(), entry.body.throws)
        assertFalse(entry.body.isEmpty)
    }

    @Test
    fun `absent throws asserts nothing`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: strlen
                  sinks:
                    - port: argument(0)
                      vulnType: sqli
                """.trimIndent(),
            )
        assertEquals(null, entry.body.throws)
    }

    @Test
    fun `throws on a method entry decodes`() {
        val entry =
            loadModel(
                """
                - subject:
                    method: mysqli::query
                  throws: [mysqli_sql_exception]
                """.trimIndent(),
            )
        assertEquals(setOf(ClassSubject("mysqli_sql_exception")), entry.body.throws)
    }

    @Test
    fun `throws on a non-callable subject is rejected`() {
        listOf("constant: PHP_EOL", "property: mysqli::${'$'}insert_id", "class: mysqli").forEach { subject ->
            assertFailsWith<IllegalArgumentException>(subject) {
                load("- subject:\n    $subject\n  throws: [ValueError]\n")
            }
        }
    }

    @Test
    fun `throws element outside the class-name spelling is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load("- subject:\n    function: strlen\n  throws: [Foo::bar]\n")
        }
    }

    @Test
    fun `throws element in wrapper form is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load("- subject:\n    function: strlen\n  throws:\n    - class: ValueError\n")
        }
    }
}
