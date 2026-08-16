# PHP Models — Verified Jackson YAML APIs

## APIs

**[jackson]** `YAMLMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().registerKotlinModule().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)`
— `jacksonObjectMapper(...)` takes a `KotlinModule.Builder.() -> Unit` in
2.19, not a `JsonFactory`; passing `YAMLFactory()` to it does not compile.

**[jackson]** `FAIL_ON_UNKNOWN_PROPERTIES` defaults to enabled — still set
explicitly: rejection of stray keys is a stated design rule, not an
inherited default a future release may flip.

**[jackson]** `ACCEPT_CASE_INSENSITIVE_ENUMS` is off by default; enabled so
files keep the lowercase vocabulary (`str`, `bool`, `any`). An unrecognized
constant still fails (`InvalidFormatException`) — case insensitivity widens
spelling only.

**[jackson]** `StreamReadFeature.STRICT_DUPLICATE_DETECTION` is off by
default; without it a doubled YAML key decodes last-wins, silently dropping
the earlier value. Enabled on the mapper builder; a doubled key throws
`JsonParseException` at parse.

## Libraries

- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.0 —
  `YAMLFactory`, the YAML backend. Catalog alias `jackson-dataformat-yaml`.
- com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0 — constructor
  binding, non-nullable enforcement, `value class` unwrapping. Catalog alias
  `jackson-module-kotlin`.
- `jackson-databind` and `jackson-annotations` arrive transitively; neither
  is declared, so one version property governs the whole set.

## Developer instructions

- Findings established against Jackson 2.19.0. Re-verify after any Jackson
  version bump by running the module's test suite — a behavior change fails
  the pinning tests.
- `JacksonSubjectProbeTest` pins the one-key subject mapping, the
  entry-level signature narrowing, and the `@JsonUnwrapped` section
  gathering, decoding throwaway replicas of the [design.md](design.md)
  hierarchies.
- The production loader tests pin the `invoke`-as-creator row:
  `ModelLoaderTest` decodes class signatures and propagations through the
  factories.

## Design-specific

| Contract | Behavior | Failure type |
|----------|----------|--------------|
| Sealed interface + `@JsonTypeInfo(Id.DEDUCTION)` + `@JsonSubTypes` over disjoint field sets | Routes `subject`-bearing entries and `name`/`find`/`where`/`model` entries with no `type` tag | — |
| Entry mixing both forms' fields | Deduction picks one form; the other form's fields fail as unknown properties | `UnrecognizedPropertyException` |
| Entry with a field of neither form | Deduced form rejects the stray key | `UnrecognizedPropertyException` |
| Unknown discriminator value | Rejected | `InvalidTypeIdException` |
| Missing non-nullable constructor parameter | Rejected | `MismatchedInputException` |
| `@JvmInline value class` over `String` | Decodes from the bare scalar; no custom deserializer | — |
| `init { require(...) }` violation | Rejected | `ValueInstantiationException` |
| Unknown enum constant | Rejected — never widens to a default | `InvalidFormatException` |
| A nested `@JsonTypeInfo` level (`constraint`) on list elements | Decodes each element by its own discriminator | — |
| `Regex(pattern)` in a property initializer, invalid pattern | Rejected at decode — `PatternSyntaxException` wrapped | `ValueInstantiationException` |
| `@JsonCreator @JvmStatic fun parse(raw: String)` on a sealed interface companion | Decodes string scalars (`"return"` / `"argument(n)"`) to the sealed subtypes | — |
| Field typed as the *subtype* (`Port.Argument`) | Does **not** consult the supertype's creator — the subtype needs its own `@JsonCreator` companion factory | `MismatchedInputException` without it |
| Malformed port string (creator throws `IllegalArgumentException`) | Rejected at decode | `ValueInstantiationException` |
| Synonym port-pair spellings | All four spellings (`from`/`input`/`to`/`output`) as nullable creator parameters; a require-exactly-one check per side | `ValueInstantiationException` on a doubled or missing side |
| `@JsonAlias` for the synonym pair | Unusable: a mapping naming both spellings decodes silently, later key overwriting the earlier | — (silent) |
| `@JsonCreator` companion factory with `@JsonProperty("is")` | The keyword config key binds through the creator-parameter rename | — |
| Creator parameter typed `JsonNode` | Receives the raw tree; `isBoolean`/`isIntegralNumber`/`isTextual` narrow the guard scalar shapes, any other shape throws in the creator | `ValueInstantiationException` |
| Optional `when` field (`@param:JsonProperty("when")`, nullable, defaulted) on the flat model form | Decodes when present, stays null when absent; deduction routing unaffected | — |
| `when` key on the generator form | Rejected — the field belongs to the flat form only | `UnrecognizedPropertyException` |
| Sealed interface + `@JsonTypeInfo(Id.NAME, As.WRAPPER_OBJECT)` + `@JsonSubTypes` | One-key mapping (`function: strlen`) routes the wrapper key to the named subtype's delegating string creator | — |
| Unknown wrapper key (`trait: foo`) | Rejected | `InvalidTypeIdException` |
| `require(...)` inside a wrapper-routed delegating creator | Rejected at decode | `ValueInstantiationException` |
| `init { require(...) }` in a `@JvmInline value class` | Rejected at decode — the value-class unwrapping path wraps the `IllegalArgumentException` in a plain `JsonMappingException`, **not** `ValueInstantiationException` | `JsonMappingException` |
| Creator parameters `(kind: String, signature: JsonNode)` + `treeToValue(node, subtype)` inside the creator | Narrows the signature mapping by the sibling kind, keeping the mapper's strictness | — |
| Stray key inside a `treeToValue`-narrowed node | Rejected — the inner failure propagates unwrapped, not re-wrapped as `ValueInstantiationException` | `UnrecognizedPropertyException` |
| `@JsonUnwrapped` parameter on a companion `@JsonCreator` (2.19, databind #1467) | Gathers the flat sibling fields into the holder type beside the named creator parameters; unwrapped properties count in a `Id.DEDUCTION` fingerprint, so routing is unaffected | — |
| Stray key on a form with an `@JsonUnwrapped` creator parameter | Silently absorbed — the unwrapped path funnels unknown keys past `FAIL_ON_UNKNOWN_PROPERTIES`; a throwing `@JsonAnySetter` on the holder restores rejection | `JsonMappingException` (plain, wrapping the setter's `IllegalArgumentException` — not `UnrecognizedPropertyException`) |
| Companion `operator fun invoke` as `@JsonCreator` on a private-constructor `data class` (`@ConsistentCopyVisibility`) | Property-based binding through the factory with defaulted parameters; call sites keep constructor syntax while normalization lives in the factory | — |
| Doubled key in one mapping (`STRICT_DUPLICATE_DETECTION`) | Rejected at parse — never decodes last-wins, including a doubled synonym spelling (`from:` twice) the `exactlyOne` check cannot see | `JsonParseException` |

- All binding failure types extend `JsonMappingException`; the parse-level
  `JsonParseException` sits beside them. Both extend
  `JsonProcessingException`, so one catch in `ModelYaml.decode` covers every
  decode failure, which lets a caller present a single
  `IllegalArgumentException` for "malformed entry".
- Three failure types differ from the `ValueInstantiationException` pattern
  — a value-class `init` failure, a stray key under `treeToValue`, and a
  stray key rejected by the unwrapped holder's any-setter — but all extend
  `JsonProcessingException`, so the single-catch contract in `ModelYaml`
  holds.
