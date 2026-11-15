/**
 * Callback declaration loading and validation.
 * - `a declared array input supply survives loading and vocabulary mapping`: reads the entire declaration.
 * - `malformed callback declarations fail loading`: rejects invalid tags, fields and positions.
 */
package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class CallbackInvocationTest {
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
            declaration.replace(", position: 0", ""), declaration.replace(", start: 1", ""))
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
            loadModel("- subject: {constant: C}\n  returns: any\n  callbacks: [$declaration]")
        }
    }
}
