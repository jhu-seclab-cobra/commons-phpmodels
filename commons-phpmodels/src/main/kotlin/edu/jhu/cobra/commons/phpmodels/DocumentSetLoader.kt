package edu.jhu.cobra.commons.phpmodels

/**
 * Loads one document set under the caller's accumulated vocabulary,
 * optionally through a [CategoryMapping], in the order the model fixes
 * (model-sets.md): manifest, vocabulary, policy, documents. Composes the
 * three single-document loaders; every stream the opener yields is closed
 * here.
 */
public object DocumentSetLoader {
    /** The manifest file name, directly under the set root. */
    public const val MANIFEST: String = "index.txt"

    /** The optional vocabulary file name, directly under the set root. */
    public const val VOCABULARY: String = "vocabulary.yaml"

    /** The optional policy file name, directly under the set root. */
    public const val POLICY: String = "policy.yaml"

    private const val COMMENT = '#'

    /**
     * Loads the set rooted at [open].
     *
     * Without [mapping]: `vocabulary.yaml`, when present, merges into
     * [context]; `policy.yaml` and every listed document decode against the
     * merged vocabulary. With [mapping]: `vocabulary.yaml` is ignored, every
     * mapping target is verified declared in [context], policy rows and
     * entries are translated, emptied entries dropped, and the returned
     * set's vocabulary is empty.
     *
     * @param open Resolves paths relative to the set root.
     * @param context The vocabulary accumulated from earlier sets.
     * @param mapping The translation for a set whose names are not the consumer's.
     * @return The set in the consumer's names.
     * @throws DocumentSetException If the manifest or a listed document is absent, a path is listed twice,
     *   or a listed document is malformed (the decode failure is the cause).
     * @throws VocabularyException If a redeclaration conflicts, a reference or mapping target is undeclared,
     *   or a mapped name is unlisted; a reference failure names the document.
     * @throws IllegalArgumentException If the vocabulary, the policy, or a mapping document is malformed.
     */
    public fun load(
        open: ResourceOpener,
        context: Vocabulary = Vocabulary.EMPTY,
        mapping: CategoryMapping? = null,
    ): DocumentSet {
        val paths = manifest(open)
        return if (mapping == null) loadDeclared(open, paths, context) else loadMapped(open, paths, context, mapping)
    }

    private fun loadDeclared(
        open: ResourceOpener,
        paths: List<String>,
        context: Vocabulary,
    ): DocumentSet {
        val declared = open.open(VOCABULARY)?.use(VocabularyLoader::load) ?: Vocabulary.EMPTY
        val merged = context.merge(declared)
        val policy = open.open(POLICY)?.use { PolicyLoader.load(it, merged) }.orEmpty()
        val documents = paths.map { path -> Document(path, decode(open, path).onEach { verify(merged, it, path) }) }
        return DocumentSet(declared, policy, documents)
    }

    private fun loadMapped(
        open: ResourceOpener,
        paths: List<String>,
        context: Vocabulary,
        mapping: CategoryMapping,
    ): DocumentSet {
        requireTargetsDeclared(mapping, context)
        // The mapped set's own names are admissible exactly when the mapping
        // lists them, so the mapping's sources serve as the vocabulary its
        // policy decodes against; an unlisted name fails there.
        val sources = mapping.sourceVocabulary()
        val policy = open.open(POLICY)?.use { PolicyLoader.load(it, sources) }.orEmpty()
        val documents =
            paths.map { path ->
                val entries = decode(open, path).mapNotNull(mapping::apply).onEach { verify(context, it, path) }
                Document(path, entries)
            }
        return DocumentSet(Vocabulary.EMPTY, mapping.apply(policy), documents)
    }

    private fun requireTargetsDeclared(
        mapping: CategoryMapping,
        context: Vocabulary,
    ) {
        mapping.categories.values.filterNotNull().forEach { target ->
            if (target !in context.vulnClasses) {
                throw VocabularyException("Mapping target category '${target.id}' is not declared")
            }
        }
        mapping.provenances.values.filterNotNull().forEach { target ->
            if (target !in context.provenances) {
                throw VocabularyException("Mapping target provenance '${target.id}' is not declared")
            }
        }
    }

    private fun CategoryMapping.sourceVocabulary(): Vocabulary =
        Vocabulary(
            vulnClasses = categories.keys.associateWith { VulnClassDecl(it, "mapped source name") },
            provenances = provenances.keys.associateWith { ProvenanceDecl(it, "mapped source name") },
        )

    // A set lists many documents; a failure inside one names it.
    private fun decode(
        open: ResourceOpener,
        path: String,
    ): List<ModelEntry> {
        val input = open.open(path) ?: throw DocumentSetException(path, "listed document is absent")
        return try {
            input.use(ModelLoader::load)
        } catch (failure: IllegalArgumentException) {
            throw DocumentSetException(path, "listed document is malformed: ${failure.message}", failure)
        }
    }

    private fun verify(
        vocabulary: Vocabulary,
        entry: ModelEntry,
        path: String,
    ) {
        try {
            vocabulary.verify(entry)
        } catch (failure: VocabularyException) {
            throw VocabularyException("${failure.message} ('$path')", failure)
        }
    }

    // A manifest line is a path; a blank line or a line starting with '#' is
    // not an entry. A path listed twice would mount one document twice.
    private fun manifest(open: ResourceOpener): List<String> {
        val input = open.open(MANIFEST) ?: throw DocumentSetException(MANIFEST, "manifest is absent")
        val lines = input.use { it.bufferedReader(Charsets.UTF_8).readLines() }
        val paths = LinkedHashSet<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith(COMMENT)) continue
            if (!paths.add(line)) throw DocumentSetException(line, "document is listed twice in the manifest")
        }
        return paths.toList()
    }
}
