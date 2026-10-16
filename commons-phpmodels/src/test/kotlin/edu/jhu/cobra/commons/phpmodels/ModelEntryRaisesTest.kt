package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * The raises section: decoding of its class-name elements and its subject-kind
 * admissibility (design-entries.md).
 *
 * - `raises decodes bare class names folded` — each element is a bare scalar
 *   decoded by the class subject creator: lowercased, leading slash stripped,
 *   with no wrapper key.
 * - `empty raises is a statement` — `raises: []` decodes to an empty set and
 *   the body is not empty.
 * - `absent raises asserts nothing` — the section stays null.
 * - `raises on a method entry decodes` — every callable kind admits it.
 * - `raises on a non-callable subject is rejected` — constant, property, and
 *   class subjects throw nothing.
 * - `raises element outside the class-name spelling is rejected` — a member
 *   spelling is no class.
 * - `raises element in wrapper form is rejected` — the element is a bare
 *   scalar, never a one-key subject mapping.
 */
internal class ModelEntryRaisesTest {
    @Test
    fun `raises decodes bare class names folded`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: json_decode
                  raises: [ValueError, \JsonException]
                """.trimIndent(),
            )
        assertEquals(setOf(ClassSubject("valueerror"), ClassSubject("jsonexception")), entry.body.raises)
    }

    @Test
    fun `empty raises is a statement`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: strlen
                  raises: []
                """.trimIndent(),
            )
        assertEquals(emptySet(), entry.body.raises)
        assertFalse(entry.body.isEmpty)
    }

    @Test
    fun `absent raises asserts nothing`() {
        val entry =
            loadModel(
                """
                - subject:
                    function: strlen
                  sinks:
                    - port: argument(0)
                      category: sqli
                """.trimIndent(),
            )
        assertEquals(null, entry.body.raises)
    }

    @Test
    fun `raises on a method entry decodes`() {
        val entry =
            loadModel(
                """
                - subject:
                    method: mysqli::query
                  raises: [mysqli_sql_exception]
                """.trimIndent(),
            )
        assertEquals(setOf(ClassSubject("mysqli_sql_exception")), entry.body.raises)
    }

    @Test
    fun `raises on a non-callable subject is rejected`() {
        listOf("constant: PHP_EOL", "property: mysqli::${'$'}insert_id", "class: mysqli").forEach { subject ->
            assertFailsWith<IllegalArgumentException>(subject) {
                load("- subject:\n    $subject\n  raises: [ValueError]\n")
            }
        }
    }

    @Test
    fun `raises element outside the class-name spelling is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load("- subject:\n    function: strlen\n  raises: [Foo::bar]\n")
        }
    }

    @Test
    fun `raises element in wrapper form is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            load("- subject:\n    function: strlen\n  raises:\n    - class: ValueError\n")
        }
    }
}
