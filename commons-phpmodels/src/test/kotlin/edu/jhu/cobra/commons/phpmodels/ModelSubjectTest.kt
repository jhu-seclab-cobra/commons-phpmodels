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
 * - `dollar prefix requires a non-empty name` — a bare `$`, alone or after
 *   an owner, fails.
 * - `blank identity is rejected` — a blank name fails construction.
 * - `identity containing whitespace is rejected` — no PHP identifier
 *   contains whitespace; inner spaces and trailing newlines fail.
 * - `constructors fold like the creators` — direct construction folds the
 *   same identity fields the spelling creators fold.
 * - `direct construction rejects spelling characters` — `::`, `$`, or a
 *   leading `\` in an identity field fails, so construction and the spelling
 *   creators yield one identity.
 * - `subjects of different kinds never compare equal` — same identity under
 *   two kinds stays distinct.
 * - `equal subjects agree on hash code` — a subject is usable as a lookup
 *   key.
 * - `subjects spell themselves in kind-prefixed form` — the toString
 *   spelling of every kind.
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
    fun `dollar prefix requires a non-empty name`() {
        assertFailsWith<IllegalArgumentException> { PropertySubject.parse("mysqli::\$") }
        assertFailsWith<IllegalArgumentException> { VariableSubject.parse("\$") }
    }

    @Test
    fun `blank identity is rejected`() {
        assertFailsWith<IllegalArgumentException> { FunctionSubject(" ") }
        assertFailsWith<IllegalArgumentException> { MethodSubject("mysqli", " ") }
        assertFailsWith<IllegalArgumentException> { ConstantSubject("") }
    }

    @Test
    fun `identity containing whitespace is rejected`() {
        assertFailsWith<IllegalArgumentException> { FunctionSubject("strlen\n") }
        assertFailsWith<IllegalArgumentException> { MethodSubject("mysqli", "que ry") }
        assertFailsWith<IllegalArgumentException> { ConstantSubject("PHP EOL") }
    }

    @Test
    fun `constructors fold like the creators`() {
        assertEquals(FunctionSubject.parse("strlen"), FunctionSubject("STRLEN"))
        assertEquals(MethodSubject.parse("mysqli::query"), MethodSubject("MySQLi", "Query"))
        assertEquals(ClassConstantSubject.parse("mysqli::REPORT_ERROR"), ClassConstantSubject("MYSQLI", "REPORT_ERROR"))
        assertEquals(VariableSubject.parse("\$_get"), VariableSubject("_GET"))
    }

    @Test
    fun `direct construction rejects spelling characters`() {
        assertFailsWith<IllegalArgumentException> { FunctionSubject("a::b") }
        assertFailsWith<IllegalArgumentException> { FunctionSubject("\\strlen") }
        assertFailsWith<IllegalArgumentException> { ConstantSubject("\$FOO") }
        assertFailsWith<IllegalArgumentException> { MethodSubject("my\$qli", "query") }
        assertFailsWith<IllegalArgumentException> { MethodSubject("mysqli", "b::c") }
        assertFailsWith<IllegalArgumentException> { PropertySubject("mysqli", "\$insert_id") }
        assertFailsWith<IllegalArgumentException> { VariableSubject("\$_get") }
    }

    @Test
    fun `subjects of different kinds never compare equal`() {
        assertNotEquals<ModelSubject>(FunctionSubject("query"), ClassSubject("query"))
        assertNotEquals<ModelSubject>(ConstantSubject("query"), FunctionSubject("query"))
        assertNotEquals<ModelSubject>(MethodSubject("mysqli", "query"), PropertySubject("mysqli", "query"))
        assertNotEquals<ModelSubject>(FunctionSubject("query"), VariableSubject("query"))
    }

    @Test
    fun `equal subjects agree on hash code`() {
        assertEquals(FunctionSubject("STRLEN").hashCode(), FunctionSubject.parse("strlen").hashCode())
        assertEquals(MethodSubject("MySQLi", "Query").hashCode(), MethodSubject.parse("mysqli::query").hashCode())
        assertEquals(
            PropertySubject("MYSQLI", "insert_id").hashCode(),
            PropertySubject.parse("mysqli::\$insert_id").hashCode(),
        )
    }

    @Test
    fun `subjects spell themselves in kind-prefixed form`() {
        assertEquals("function strlen", FunctionSubject("strlen").toString())
        assertEquals("class mysqli", ClassSubject("mysqli").toString())
        assertEquals("method mysqli::query", MethodSubject("mysqli", "query").toString())
        assertEquals("class constant mysqli::REPORT_ERROR", ClassConstantSubject("mysqli", "REPORT_ERROR").toString())
        assertEquals("property mysqli::\$insert_id", PropertySubject("mysqli", "insert_id").toString())
        assertEquals("constant PHP_EOL", ConstantSubject("PHP_EOL").toString())
        assertEquals("variable \$_get", VariableSubject("_GET").toString())
    }
}
