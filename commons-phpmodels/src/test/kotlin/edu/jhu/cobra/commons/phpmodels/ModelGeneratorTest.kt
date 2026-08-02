package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Construction rules and subject matching of [ModelGenerator].
 *
 * - `generator requires a name constraint` — a where list without one fails.
 * - `class constraint requires a method find` — a class pattern beside a
 *   function find fails.
 * - `generator requires a non-empty body` — a generator attaches no
 *   signature, so an empty body asserts nothing.
 * - `variable find admits sources only` — a sink section beside a variable
 *   find fails.
 * - `matches requires kind and every constraint` — entire-field match over
 *   the folded identity; declaration-only kinds never match.
 */
internal class ModelGeneratorTest {
    private val sources = ModelBody(sources = listOf(SourceDecl(setOf(ProvenanceId("remote")))))

    @Test
    fun `generator requires a name constraint`() {
        assertFailsWith<IllegalArgumentException> {
            ModelGenerator("g", SubjectKind.METHOD, listOf(ClassConstraint("mysqli")), sources)
        }
    }

    @Test
    fun `class constraint requires a method find`() {
        assertFailsWith<IllegalArgumentException> {
            ModelGenerator("g", SubjectKind.FUNCTION, listOf(NameConstraint("get.*"), ClassConstraint("a")), sources)
        }
    }

    @Test
    fun `generator requires a non-empty body`() {
        assertFailsWith<IllegalArgumentException> {
            ModelGenerator("g", SubjectKind.FUNCTION, listOf(NameConstraint("get.*")), ModelBody())
        }
    }

    @Test
    fun `variable find admits sources only`() {
        assertFailsWith<IllegalArgumentException> {
            ModelGenerator("g", SubjectKind.VARIABLE, listOf(NameConstraint("_get")), ModelBody(ReturnKind.ANY))
        }
    }

    @Test
    fun `matches requires kind and every constraint`() {
        val generator =
            ModelGenerator(
                name = "mysqli-query-methods",
                find = SubjectKind.METHOD,
                where = listOf(NameConstraint("query|execute"), ClassConstraint("mysqli.*")),
                model = sources,
            )
        assertEquals(true, generator.matches(MethodSubject("mysqli_stmt", "execute")))
        assertEquals(false, generator.matches(MethodSubject("pdo", "query")))
        assertEquals(false, generator.matches(FunctionSubject("query")))
        assertEquals(false, generator.matches(MethodSubject("mysqli", "multi_query")))
    }
}
