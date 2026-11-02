package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Typed selector ports and operation-fact decoding through the model boundary. */
internal class DynamicInvocationModelTest {
    @Test
    fun `callee is an input and a sink port`() {
        assertEquals(Port.Callee, Port.Input.parseInput("callee"))
        assertEquals(Port.Callee, Port.Sink.parseSink("callee"))
        val entry = load("operation: dynamic_invocation", "callee")
        assertEquals("dynamic_invocation", entry.subject.name)
        assertEquals("callee", entry.body.sinks!!.single().port.toString())
    }

    @Test
    fun `a sink rejects receiver return and callback ports`() {
        listOf("this", "return", "callback(argument(0)).return").forEach {
            assertFailsWith<IllegalArgumentException> { load("function: f", it) }
        }
    }

    @Test
    fun `callee facts belong to the operation and arguments to declarations`() {
        assertFailsWith<IllegalArgumentException> { load("function: f", "callee") }
        assertFailsWith<IllegalArgumentException> { load("operation: dynamic_invocation", "argument(0)") }
        assertFailsWith<IllegalArgumentException> { load("operation: unknown", "callee") }
    }

    @Test
    fun `operation rejects target behavior and signature sections`() {
        listOf("returns: str", "signature: {params: [], return: string}", "when: [x]").forEach { extra ->
            val yaml = "- subject: {operation: dynamic_invocation}\n" +
                "  sinks: [{port: callee, vulnType: callablei}]\n  $extra"
            assertFailsWith<IllegalArgumentException> { ModelLoader.load(yaml.byteInputStream()) }
        }
    }

    private fun load(subject: String, port: String): ModelEntry = ModelLoader.load(
        "- subject: { $subject }\n  sinks: [{port: $port, vulnType: callablei}]".byteInputStream(),
    ).single()
}
