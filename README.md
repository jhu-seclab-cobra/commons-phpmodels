# COBRA.COMMONS.PHPMODELS

> Commons — the shared vocabulary: one declarative model format every Cobra PHP analyzer reads and writes.

Typed decode of declarative PHP models — taint effects, value semantics, and declaration signatures — from YAML.

![Kotlin JVM](https://img.shields.io/badge/Kotlin%20JVM-2.2.21%20%7C%20JVM%2021-blue?logo=kotlin)
[![Release](https://img.shields.io/badge/release-v0.2.1-blue.svg)](https://github.com/jhu-seclab-cobra/commons-phpmodels/releases)
[![last commit](https://img.shields.io/github/last-commit/jhu-seclab-cobra/commons-phpmodels)](https://github.com/jhu-seclab-cobra/commons-phpmodels/commits/main)
[![license](https://img.shields.io/badge/license-GPL--2.0-blue.svg)](./LICENSE)

## Install

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.jhu-seclab-cobra:commons-phpmodels:0.2.1")
}
```

## Usage

```kotlin
import edu.jhu.cobra.commons.phpmodels.ModelLoader
import edu.jhu.cobra.commons.phpmodels.SubjectModel

val document =
    """
    - subject:
        function: substr
      signature:
        params:
          - name: string
            type: string
        returnType: string
      propagation:
        - from: argument(0)
          to: return
    """.trimIndent()

val model = ModelLoader.load(document.byteInputStream()).single() as SubjectModel
model.subject          // FunctionSubject(name=substr)
model.body.returns     // ReturnKind.STR — derived from the declared return type
model.body.propagation // [Propagation(from=argument(0), to=return)]
```

## API

Three loaders decode one document each; `DocumentSetLoader` composes them over a whole set. Every format violation raises `IllegalArgumentException` at load; nothing malformed crosses the boundary.

| Loader | Signature | Decodes |
|--------|-----------|---------|
| `ModelLoader` | `load(input: InputStream): List<ModelEntry>` | Model documents: flat entries and generators |
| `VocabularyLoader` | `load(input: InputStream): Vocabulary` | The two-axis vocabulary: danger categories, origin colors |
| `PolicyLoader` | `load(input: InputStream, vocabulary: Vocabulary): List<PolicyRow>` | The origin → categories taint policy |
| `DocumentSetLoader` | `load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY, mapping: CategoryMapping? = null): DocumentSet` | One document set: `index.txt`, optional `vocabulary.yaml` and `policy.yaml`, listed documents; optionally translated through a mapping |
| `CategoryMappingLoader` | `load(input: InputStream): CategoryMapping` | A consumer's translation of another set's category and color names |

Decoded types: `ModelEntry` (`SubjectModel`, `ModelGenerator`), `ModelSubject` (seven PHP declaration kinds), `ModelBody` (returns, propagation, sources, sinks, sanitizers), `SignatureInfo` (callable, class, typed, property), `WhenGuard`, `Port`, `ReturnKind`, `Vocabulary`, `TaintPolicy`, `DocumentSet`, `CategoryMapping`. Full type specifications: [docs/design.md](docs/design.md), [docs/design-sets.md](docs/design-sets.md).

## Background

Cobra analyzers describe PHP built-ins and user code with declarative models in the style of [Mariana Trench](https://mariana-tren.ch/) and [Pysa](https://pyre-check.org/docs/pysa-basics/): one YAML entry per PHP declaration, asserting taint effects, value semantics, and signatures. This library owns the format — types, strict decoding, and validation — so every producer and consumer shares one definition. Consumers own layer ordering, branch selection, subject resolution, and every query.

## Documentation

- [Concepts](docs/concept.md) — problem, scope, terminology, data flow, scenarios
- [Domain model](docs/model.md) — model sections, subjects, color model, invariants
- [Design](docs/design.md) — type model, loaders, decoder strictness
- [Index](docs/index.md) — all documents, one line each

## For Agents

Agent-consumable documentation index at `docs/llms.txt` ([llmstxt.org](https://llmstxt.org) format).

## Citation

```bibtex
@inproceedings{xu2026cobra,
  title     = {CoBrA: Context-, Branch-sensitive Static Analysis for Detecting Taint-style Vulnerabilities in PHP Web Applications},
  author    = {Xu, Yichao and Kang, Mingqing and Thimmaiah, Neil and Gjomemo, Rigel and Venkatakrishnan, V. N. and Cao, Yinzhi},
  booktitle = {Proceedings of the 48th IEEE/ACM International Conference on Software Engineering (ICSE)},
  year      = {2026},
  address   = {Rio de Janeiro, Brazil}
}
```

## License

GPL-2.0 — see [LICENSE](./LICENSE).
