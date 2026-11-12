# commons-phpmodels

[![codecov](https://codecov.io/gh/jhu-seclab-cobra/commons-phpmodels/branch/main/graph/badge.svg)](https://codecov.io/gh/jhu-seclab-cobra/commons-phpmodels)
![Kotlin JVM](https://img.shields.io/badge/Kotlin%20JVM-2.4.20%20%7C%20JVM%2021%2B-blue?logo=kotlin)
[![Release](https://img.shields.io/badge/release-v0.12.0-blue.svg)](https://github.com/jhu-seclab-cobra/commons-phpmodels/releases/tag/v0.12.0)
[![last commit](https://img.shields.io/github/last-commit/jhu-seclab-cobra/commons-phpmodels)](https://github.com/jhu-seclab-cobra/commons-phpmodels/commits/main)
[![JitPack](https://jitpack.io/v/jhu-seclab-cobra/commons-phpmodels.svg)](https://jitpack.io/#jhu-seclab-cobra/commons-phpmodels)
![Repo Size](https://img.shields.io/github/repo-size/jhu-seclab-cobra/commons-phpmodels)
[![license](https://img.shields.io/github/license/jhu-seclab-cobra/commons-phpmodels)](./LICENSE)

> Commons — the shared vocabulary: one declarative model format every Cobra PHP analyzer reads and writes.

Typed decode of declarative PHP models — taint effects, Value unit instances, and declaration signatures — from YAML.

## Install

Requires JDK 21 and a Gradle Kotlin DSL project using Kotlin 2.4.20.

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.jhu-seclab-cobra:commons-phpmodels:0.12.0")
}
```

## Usage

```kotlin
import edu.jhu.cobra.commons.phpmodels.DocumentSetLoader
import edu.jhu.cobra.commons.phpmodels.ResourceOpener
val yaml = "- subject: {function: example}
  returns: str
"
val files = mapOf("index.txt" to "models.yaml
", "models.yaml" to yaml)
val set = DocumentSetLoader.load(ResourceOpener { files[it]?.byteInputStream() })
val entry = set.entries.single()
entry.body.returns
```

## API

Two public loaders: `DocumentSetLoader` decodes one whole set (manifest, vocabulary, policy, provenance, model documents); `VocabularyMappingLoader` decodes a consumer's mapping. Every format violation raises `IllegalArgumentException` at load; nothing malformed crosses the boundary.

| Loader | Signature | Decodes |
|--------|-----------|---------|
| `DocumentSetLoader` | `load(open: ResourceOpener, context: Vocabulary = Vocabulary.EMPTY, mapping: VocabularyMapping? = null): DocumentSet` | One Document Set: `index.txt`, optional `vocabulary.yaml`, `policy.yaml`, and `provenance.yaml`, listed documents; optionally translated through a mapping |
| `VocabularyMappingLoader` | `load(input: InputStream): VocabularyMapping` | A consumer's translation of another set's Vulnerability type and Source kind names |

See [the design index](docs/index.md) for decoded types and their validation contracts.

## Citation

Please cite the associated CoBrA paper when using this library.

```bibtex
@inproceedings{xu2026cobra,
  title     = {CoBrA: Context-, Branch-sensitive Static Analysis for Detecting Taint-style Vulnerabilities in PHP Web Applications},
  author    = {Xu, Yichao and Kang, Mingqing and Thimmaiah, Neil and Gjomemo, Rigel and Venkatakrishnan, V. N. and Cao, Yinzhi},
  booktitle = {Proceedings of the 48th IEEE/ACM International Conference on Software Engineering (ICSE)},
  year      = {2026},
  address   = {Rio de Janeiro, Brazil}
}
```

## Documentation

- [Concepts](docs/concept.md) — problem, scope, terminology, data flow, scenarios
- [Set Provenance Concepts](docs/concept-provenance.md) — producer, verification kind, precedence between sets
- [Domain model](docs/model.md) — model sections, subjects, taint vocabulary, invariants
- [Design](docs/design.md) — type model, loaders, decoder strictness
- [Index](docs/index.md) — all documents, one line each

## For Agents

Agent-consumable documentation index at [docs/llms.txt](docs/llms.txt) (llmstxt.org format).

## License

GPL-2.0 — see [LICENSE](./LICENSE).
