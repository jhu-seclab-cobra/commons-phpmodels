/**
 * Callback declaration loading and validation.
 * - `a declared array input supply survives loading and vocabulary mapping`: reads the entire declaration.
 * - `malformed callback declarations fail loading`: rejects invalid tags, fields and positions.
 * - `the remaining input supplies and selectors decode`: preserves the generic declaration data.
 * - `signature bounds reject undeclared invocation inputs`: checks the last declared port and both out-of-range ports.
 */
package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class CallbackInvocationTest {
    @Test
    fun `the remaining input supplies and selectors decode`() {
        val cases = listOf(
            "{kind: trailing, distance: 1}" to "{kind: joined-inputs, count: 2}",
            "{kind: elements, position: 0}" to "{kind: array-value, position: 1}",
            "{kind: argument, position: 1}" to "{kind: unpacked, position: 2}",
        )
        val expected = listOf(
            CallbackInvocation(CallbackSelector.Trailing(distance = 1), CallbackSupply.JoinedInputs(count = 2)),
            CallbackInvocation(CallbackSelector.Elements(position = 0), CallbackSupply.ArrayValue(position = 1)),
            CallbackInvocation(CallbackSelector.Argument(position = 1), CallbackSupply.Unpacked(position = 2)),
        )
        for ((index, row) in cases.withIndex()) {
            val yaml = "- subject: {function: f}\n  returns: any\n" +
                "  callbacks: [{callable: ${row.first}, supply: ${row.second}}]"
            assertEquals(listOf(expected[index]), loadModel(yaml).body.valueUnit()?.callbacks)
        }
        for (invalid in listOf("{kind: trailing, distance: 0}", "{kind: elements, position: -1}")) {
            assertFailsWith<IllegalArgumentException> {
                loadModel("- subject: {function: f}\n  returns: any\n" +
                    "  callbacks: [{callable: $invalid, supply: {kind: joined-inputs, count: 2}}]")
            }
        }
        for (invalid in listOf("{kind: joined-inputs, count: 0}", "{kind: array-value, position: -1}",
            "{kind: unpacked, position: -1}")) {
            assertFailsWith<IllegalArgumentException> {
                loadModel("- subject: {function: f}\n  returns: any\n" +
                    "  callbacks: [{callable: {kind: argument, position: 0}, supply: $invalid}]")
            }
        }
    }

    @Test
    fun `signature bounds reject undeclared invocation inputs`() {
        val header = "- subject: {function: f}\n  signature:\n" +
            "    params: [{name: callback, type: callable}, {name: input, type: array}]\n" +
            "    returnType: mixed\n"
        for (position in listOf(0, 1)) {
            val model = loadModel(header + "  callbacks: [{callable: {kind: argument, position: $position}," +
                " supply: {kind: array-elements, start: 1}}]")
            assertEquals(setOf(ReturnKind.ANY), model.body.valueUnit()?.returns)
        }
        for (position in listOf(2, 3)) {
            assertFailsWith<IllegalArgumentException> {
                loadModel(header + "  callbacks: [{callable: {kind: argument, position: $position}," +
                    " supply: {kind: array-elements, start: 1}}]")
            }
            assertFailsWith<IllegalArgumentException> {
                loadModel(header + "  callbacks: [{callable: {kind: argument, position: 0}," +
                    " supply: {kind: unpacked, position: $position}}]")
            }
        }
        assertFailsWith<IllegalArgumentException> {
            loadModel(header + "  callbacks: [{callable: {kind: trailing, distance: 3}," +
                " supply: {kind: joined-inputs, count: 2}}]")
        }
    }

    private val declaration = "{callable: {kind: argument, position: 0}, supply: {kind: array-elements, start: 1}}"

    @Test
    fun `a declared array input supply survives loading and vocabulary mapping`() {
        val model = loadModel("- subject: {function: f}\n  returns: any\n  callbacks: [$declaration]")
        val expected = CallbackInvocation(
            CallbackSelector.Argument(position = 0), CallbackSupply.ArrayElements(start = 1),
        )
        assertEquals(listOf(expected), model.body.valueUnit()?.callbacks)
        assertEquals(model, VocabularyMapping(emptyMap(), emptyMap()).apply(model))
    }

    @Test
    fun `malformed callback declarations fail loading`() {
        val invalid = listOf(declaration.replace("argument,", "absent,"),
            declaration.replace("position: 0", "position: -1"), declaration.replace("start: 1", "start: -1"),
            declaration.replace("position: 0", "extra: 0"), declaration.replace("array-elements", "absent"),
            declaration.replace(", position: 0", ""), declaration.replace("position: 0", "position: null"),
            declaration.replace(", start: 1", ""))
        for (row in invalid) {
            assertFailsWith<IllegalArgumentException> {
                loadModel("- subject: {function: f}\n  returns: any\n  callbacks: [$row]")
            }
        }
        assertFailsWith<IllegalArgumentException> {
            loadModel("- subject: {function: f}\n  callbacks: [$declaration]")
        }
        assertFailsWith<IllegalArgumentException> {
            loadModel("- subject: {function: f}\n  returns: any\n  callbacks: []")
        }
        assertFailsWith<IllegalArgumentException> {
            loadModel("- subject: {function: f}\n  returns: any\n  callbacks: [null]")
        }
        assertFailsWith<IllegalArgumentException> {
            loadModel("- subject: {constant: C}\n  returns: any\n  callbacks: [$declaration]")
        }
    }
}
