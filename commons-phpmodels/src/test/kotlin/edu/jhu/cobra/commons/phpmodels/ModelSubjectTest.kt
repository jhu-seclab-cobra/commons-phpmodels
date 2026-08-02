package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Spelling grammar and folded identity of the seven [ModelSubject] kinds.
 *
 * - `folded kinds fold their identity` — function, class, method, and
 *   variable names compare case-insensitively.
 * - `sensitive kinds keep their case` — constant, class-constant, and
 *   property names compare exactly; owners still fold.
 * - `leading namespace slash is stripped` — `\mysqli::query` equals
 *   `mysqli::query`.
 * - `member spelling requires exactly one separator` — zero or two `::` fail.
 * - `member spelling requires non-empty sides` — a blank owner or name fails.
 * - `property spelling requires the dollar prefix` — `class::name` under the
 *   property kind fails; the stored name drops the `$`.
 * - `dollar is forbidden outside property and variable spellings` — a `$`
 *   inside a function or method name fails.
 * - `variable spelling requires the dollar prefix` — a bare name fails; the
 *   stored name drops the `$`.
 * - `blank identity is rejected` — a blank name fails construction.
 */
internal class ModelSubjectTest {
    @Test
    fun `folded kinds fold their identity`() {
        assertEquals(FunctionSubject.parse("STRLEN"), FunctionSubject.parse("strlen"))
        assertEquals(ClassSubject.parse("MySQLi"), ClassSubject.parse("mysqli"))
        assertEquals(MethodSubject.parse("MySQLi::Query"), MethodSubject.parse("mysqli::query"))
        assertEquals(VariableSubject.parse("\$_GET"), VariableSubject.parse("\$_get"))
    }

    @Test
    fun `sensitive kinds keep their case`() {
        assertNotEquals(ConstantSubject.parse("PHP_EOL"), ConstantSubject.parse("php_eol"))
        assertNotEquals(
            ClassConstantSubject.parse("mysqli::REPORT_ERROR"),
            ClassConstantSubject.parse("mysqli::report_error"),
        )
        assertNotEquals(
            PropertySubject.parse("mysqli::\$insert_id"),
            PropertySubject.parse("mysqli::\$INSERT_ID"),
        )
        assertEquals(
            ClassConstantSubject.parse("MYSQLI::REPORT_ERROR"),
            ClassConstantSubject.parse("mysqli::REPORT_ERROR"),
        )
    }

    @Test
    fun `leading namespace slash is stripped`() {
        assertEquals(FunctionSubject.parse("\\strlen"), FunctionSubject.parse("strlen"))
        assertEquals(MethodSubject.parse("\\mysqli::query"), MethodSubject.parse("mysqli::query"))
    }

    @Test
    fun `member spelling requires exactly one separator`() {
        assertFailsWith<IllegalArgumentException> { MethodSubject.parse("query") }
        assertFailsWith<IllegalArgumentException> { MethodSubject.parse("a::b::c") }
    }

    @Test
    fun `member spelling requires non-empty sides`() {
        assertFailsWith<IllegalArgumentException> { MethodSubject.parse("::query") }
        assertFailsWith<IllegalArgumentException> { MethodSubject.parse("mysqli::") }
    }

    @Test
    fun `property spelling requires the dollar prefix`() {
        assertFailsWith<IllegalArgumentException> { PropertySubject.parse("mysqli::insert_id") }
        assertEquals("insert_id", PropertySubject.parse("mysqli::\$insert_id").name)
    }

    @Test
    fun `dollar is forbidden outside property and variable spellings`() {
        assertFailsWith<IllegalArgumentException> { FunctionSubject.parse("\$strlen") }
        assertFailsWith<IllegalArgumentException> { MethodSubject.parse("mysqli::\$query") }
        assertFailsWith<IllegalArgumentException> { ClassConstantSubject.parse("mysqli::\$REPORT") }
    }

    @Test
    fun `variable spelling requires the dollar prefix`() {
        assertFailsWith<IllegalArgumentException> { VariableSubject.parse("_GET") }
        assertEquals("_get", VariableSubject.parse("\$_GET").name)
    }

    @Test
    fun `blank identity is rejected`() {
        assertFailsWith<IllegalArgumentException> { FunctionSubject(" ") }
        assertFailsWith<IllegalArgumentException> { MethodSubject("mysqli", " ") }
        assertFailsWith<IllegalArgumentException> { ConstantSubject("") }
    }
}
