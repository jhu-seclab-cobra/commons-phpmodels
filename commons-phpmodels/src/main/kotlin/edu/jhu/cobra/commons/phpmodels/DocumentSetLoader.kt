package edu.jhu.cobra.commons.phpmodels

/**
 * Loads one document set under the caller's accumulated vocabulary,
 * optionally through a [CategoryMapping], in the order the model fixes
 * (model-sets.md): manifest, provenance, vocabulary, policy, documents.
 * Composes the single-document loaders; every stream the opener yields is
 * closed here.
 */
public object DocumentSetLoader {
    /** The manifest file name, directly under the set root. */
    public const val MANIFEST: String = "index.txt"

    /** The optional vocabulary file name, directly under the set root. */
    public const val VOCABULARY: String = "vocabulary.yaml"

    /** The optional policy file name, directly under the set root. */
    public const val POLICY: String = "policy.yaml"

    /** The optional provenance file name, directly under the set root. */
    public const val PROVENANCE: String = "provenance.yaml"

    // The manifest comment marker, fixed by the manifest format (model-sets.md).
    private const val COMMENT = '#'

    // The description a mapped set's source names carry: the mapping declares
    // no descriptions, so one documentary text stands for every source name.
    private const val MAPPED_SOURCE_DESCRIPTION = "mapped source name"

    /**
     * Loads the set rooted at [open].
     *
     * Without [mapping]: `vocabulary.yaml`, when present, merges into
     * [context]; `policy.yaml` and every listed document decode against the
     * merged vocabulary. With [mapping]: `vocabulary.yaml` is ignored, every
     * mapping target is verified declared in [context], policy rows and
     * entries are translated, emptied entries dropped, and the returned
     * set's vocabulary is empty. Either way `provenance.yaml`, when present,
     * is decoded and attached to the returned set.
     *
     * @param open Resolves paths relative to the set root.
     * @param context The vocabulary accumulated from earlier sets.
     * @param mapping The translation for a set whose names are not the consumer's.
     * @return The set in the consumer's names.
     * @throws DocumentSetException If the manifest or a listed document is absent, a path is listed twice,
     *   or a listed document is malformed (the decode failure is the cause).
     * @throws VocabularyException If a redeclaration conflicts, a reference or mapping target is undeclared,
     *   or a mapped name is unlisted; a reference failure names the document.
     * @throws IllegalArgumentException If the vocabulary, the policy, the provenance, or a mapping document
     *   is malformed.
     */
    public fun load(
        open: ResourceOpener,
        context: Vocabulary = Vocabulary.EMPTY,
        mapping: CategoryMapping? = null,
    ): DocumentSet {
        val root = SetRoot(open, manifest(open))
        val provenance = root.open(PROVENANCE)?.use(ProvenanceLoader::load)
        val set = if (mapping == null) loadDeclared(root, context) else loadMapped(root, context, mapping)
        return set.copy(provenance = provenance)
    }

    // The opened set root together with its manifest, which is read before
    // any other document so that a missing or malformed manifest fails first.
    private class SetRoot(
        opener: ResourceOpener,
        val paths: List<String>,
    ) : ResourceOpener by opener

    private fun loadDeclared(
        root: SetRoot,
        context: Vocabulary,
    ): DocumentSet {
        val declared = root.open(VOCABULARY)?.use(VocabularyLoader::load) ?: Vocabulary.EMPTY
        val merged = context.merge(declared)
        val policy = root.open(POLICY)?.use { PolicyLoader.load(it, merged) }.orEmpty()
        val documents =
            root.paths.map { path ->
                Document(path, decode(root, path).onEach { verify(merged, it, path) })
            }
        return DocumentSet(declared, policy, documents)
    }

    private fun loadMapped(
        root: SetRoot,
        context: Vocabulary,
        mapping: CategoryMapping,
    ): DocumentSet {
        requireTargetsDeclared(mapping, context)
        // The mapped set's own names are admissible exactly when the mapping
        // lists them, so the mapping's sources serve as the vocabulary its
        // policy decodes against; an unlisted name fails there.
        val sources = mapping.sourceVocabulary()
        val policy = root.open(POLICY)?.use { PolicyLoader.load(it, sources) }.orEmpty()
        val documents =
            root.paths.map { path ->
                val entries = decode(root, path).mapNotNull(mapping::apply).onEach { verify(context, it, path) }
                Document(path, entries)
            }
        return DocumentSet(Vocabulary.EMPTY, mapping.apply(policy), documents)
    }

    private fun requireTargetsDeclared(
        mapping: CategoryMapping,
        context: Vocabulary,
    ) {
        mapping.categories.values.firstUndeclaredIn(context.vulnClasses)?.let { target ->
            throw VocabularyException("Mapping target category '${target.id}' is not declared")
        }
        mapping.origins.values.firstUndeclaredIn(context.origins)?.let { target ->
            throw VocabularyException("Mapping target origin '${target.id}' is not declared")
        }
    }

    // A discarding (null) target names nothing that must be declared.
    private fun <K : Any> Collection<K?>.firstUndeclaredIn(declared: Map<K, *>): K? =
        filterNotNull().firstOrNull { it !in declared }

    private fun CategoryMapping.sourceVocabulary(): Vocabulary =
        Vocabulary(
            vulnClasses = categories.keys.associateWith { VulnClassDecl(it, MAPPED_SOURCE_DESCRIPTION) },
            origins = origins.keys.associateWith { OriginDecl(it, MAPPED_SOURCE_DESCRIPTION) },
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
