package edu.jhu.cobra.commons.phpmodels

// Shared decode entry points of the ModelLoader test files.

/** Decodes one YAML document into its entry list. */
internal fun load(yaml: String): List<ModelEntry> = ModelLoader.load(yaml.byteInputStream())

/** Decodes one YAML document expected to hold a single entry. */
internal fun loadModel(yaml: String): ModelEntry = load(yaml).single()
