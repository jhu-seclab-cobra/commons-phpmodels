package edu.jhu.cobra.commons.phpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The tokenizer language a vulnerability type's vocabulary entry holds.
 *
 * - `a vocabulary entry without a tokenizer holds none` — the field is optional.
 * - `a source kind entry never holds a tokenizer` — the strict decode rejects the key.
 * - `a language runs from its start state over bytes` — `run` is the transition function over a string.
 * - `the first matching rule decides, and an unmatched byte takes otherwise or stays` — rule order and the default.
 * - `a tracked state branches on the word spelled since entry` — `ifWord`, case folding, prefix and extension.
 * - `states lists every state including the tracked prefixes` — the set an unknown text expands to.
 * - `step and run read each character as one Latin-1 byte` — a value above 255 is rejected.
 * - `a malformed language is rejected` — unknown names, misplaced `ifWord`, tracked start or target, bad words.
 * - `equal languages compare equal and a differing redeclaration conflicts` — merge compares the language.
 */
internal class TokenizerLanguageTest {
    private val linkLanguage =
        """
        vulnTypes:
          - name: link
            description: Link injection
            tokenizer:
              start: text
              targets: [value]
              words:
                link-attribute: [href, src]
              states:
                text:
                  rules:
                    - {chars: '[<]', next: tag}
                tag:
                  rules:
                    - {chars: '[ ]', next: name-start}
                    - {chars: '[>]', next: text}
                name-start:
                  rules:
                    - {chars: '[ ]', next: name-start}
                    - {chars: '[>]', next: text}
                  otherwise: name
                name:
                  track: link-attribute
                  rules:
                    - {chars: '[=]', next: value, ifWord: true}
                    - {chars: '[=]', next: other-value}
                    - {chars: '[>]', next: text}
                other-value:
                  rules:
                    - {chars: '[>]', next: text}
                value:
                  rules:
                    - {chars: '[>]', next: text}
        sourceKinds: []
        """.trimIndent()

    private fun load(yaml: String): Vocabulary = VocabularyLoader.load(yaml.byteInputStream())

    private fun linkType(yaml: String = linkLanguage): TokenizerLanguage =
        assertNotNull(load(yaml).vulnTypes.getValue(VulnTypeId("link")).tokenizer)

    private fun TokenizerLanguage.after(text: String): String = run(start, text).name

    @Test
    fun `a vocabulary entry without a tokenizer holds none`() {
        val vocabulary = load("vulnTypes:\n  - name: sqli\n    description: SQL injection\nsourceKinds: []\n")
        assertNull(vocabulary.vulnTypes.getValue(VulnTypeId("sqli")).tokenizer)
    }

    @Test
    fun `a source kind entry never holds a tokenizer`() {
        val yaml =
            """
            vulnTypes: []
            sourceKinds:
              - name: remote
                description: Remote input
                tokenizer: {start: text, targets: [text], states: {text: {}}}
            """.trimIndent()
        assertFailsWith<IllegalArgumentException> { load(yaml) }
    }

    @Test
    fun `a language runs from its start state over bytes`() {
        val language = linkType()
        assertEquals("text", language.start.name)
        assertEquals(setOf("value"), language.targets.map { it.name }.toSet())
        assertEquals("text", language.after("plain text"))
        assertEquals("tag", language.after("a <"))
        assertEquals("text", language.after("<>"))
        assertEquals("text", language.after(""))
    }

    @Test
    fun `the first matching rule decides, and an unmatched byte takes otherwise or stays`() {
        val language = linkType()
        assertEquals("tag", language.after("<x"))
        assertEquals("name-start", language.after("< "))
        assertEquals("name-start", language.after("<  "))
        assertEquals("name", language.after("< z"))
        assertEquals("name", language.after("< zz"))
        assertEquals("text", language.after("< z>"))
    }

    @Test
    fun `a tracked state branches on the word spelled since entry`() {
        val language = linkType()
        assertEquals("value", language.after("< href="))
        assertEquals("value", language.after("< SRC="))
        assertEquals("value", language.after("< HrEf="))
        assertEquals("other-value", language.after("< hre="))
        assertEquals("other-value", language.after("< hrefx="))
        assertEquals("other-value", language.after("< xhref="))
        assertEquals("other-value", language.after("< srcs="))
        assertEquals("other-value", language.after("< a="))
        assertEquals("value", language.after("< a> < src="))
        assertEquals("name:hre", language.after("< hre"))
    }

    @Test
    fun `states lists every state including the tracked prefixes`() {
        val language = linkType()
        val names = language.states.map { it.name }.toSet()
        val untracked = setOf("text", "tag", "name-start", "other-value", "value")
        val prefixes = setOf("name:h", "name:hr", "name:hre", "name:href", "name:s", "name:sr", "name:src")
        assertEquals(untracked + "name" + prefixes, names)
        assertTrue(language.start in language.states)
        assertTrue(language.targets.all { it in language.states })
    }

    @Test
    fun `step and run read each character as one Latin-1 byte`() {
        val language = linkType()
        assertEquals("text", language.after("éÿ"))
        assertFailsWith<IllegalArgumentException> { language.run(language.start, "Ā") }
        assertFailsWith<IllegalArgumentException> { language.step(language.start, 256) }
        assertFailsWith<IllegalArgumentException> { language.step(language.start, -1) }
    }

    @Test
    fun `a malformed language is rejected`() {
        fun broken(
            from: String,
            to: String,
            expected: String,
        ) {
            val failure = assertFailsWith<IllegalArgumentException> { load(linkLanguage.replace(from, to)) }
            assertTrue(expected in failure.message.orEmpty(), failure.message)
        }
        broken("start: text", "start: nowhere", "nowhere")
        broken("targets: [value]", "targets: [nowhere]", "nowhere")
        broken("targets: [value]", "targets: [name]", "name")
        broken("start: text", "start: name", "name")
        broken("{chars: '[<]', next: tag}", "{chars: '[<]', next: nowhere}", "nowhere")
        broken("otherwise: name", "otherwise: nowhere", "nowhere")
        broken("{chars: '[<]', next: tag}", "{chars: '[<]', next: tag, ifWord: true}", "ifWord")
        broken("track: link-attribute", "track: missing", "missing")
        broken("[href, src]", "[]", "link-attribute")
        broken("[href, src]", "[href, '']", "link-attribute")
        broken("[href, src]", "['hĀ']", "link-attribute")
        broken("name-start:\n", "Name_Start:\n", "Name_Start")
    }

    @Test
    fun `equal languages compare equal and a differing redeclaration conflicts`() {
        val one = load(linkLanguage)
        val same = load(linkLanguage)
        assertEquals(one.vulnTypes.getValue(VulnTypeId("link")), same.vulnTypes.getValue(VulnTypeId("link")))
        assertEquals(one, one.merge(same))
        val other = load(linkLanguage.replace("[href, src]", "[href]"))
        assertNotEquals(one.vulnTypes, other.vulnTypes)
        val failure = assertFailsWith<VocabularyException> { one.merge(other) }
        assertTrue("link" in failure.message.orEmpty(), failure.message)
        val bare = load("vulnTypes:\n  - name: link\n    description: Link injection\nsourceKinds: []\n")
        assertFailsWith<VocabularyException> { one.merge(bare) }
    }
}
