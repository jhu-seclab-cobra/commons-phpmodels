# PHP Models — Jackson YAML APIs

## APIs

- [@jackson-yaml] `YAMLMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().registerKotlinModule().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)` — `jacksonObjectMapper(...)` takes a `KotlinModule.Builder.() -> Unit` in 2.22, not a `JsonFactory`; passing `YAMLFactory()` to it does not compile.

- [@jackson] `FAIL_ON_UNKNOWN_PROPERTIES` defaults to enabled — still set explicitly: rejection of stray keys is a stated design rule, enabled explicitly by ModelYaml.

- [@jackson] `ACCEPT_CASE_INSENSITIVE_ENUMS` is off by default; enabled so files keep the lowercase vocabulary (`str`, `bool`, `any`). An unrecognized constant still fails (`InvalidFormatException`) — case insensitivity widens spelling only.

- [@jackson] `StreamReadFeature.STRICT_DUPLICATE_DETECTION` is off by default; without it a doubled YAML key decodes last-wins, silently dropping the earlier value. Enabled on the mapper builder; a doubled key throws `JsonParseException` at parse.

- [@jackson] `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` is unusable with this format: the check runs after buffered inner binds (deduction, `@JsonUnwrapped`, `treeToValue`), failing valid single-document loads with "Trailing token … after value (bound as `java.lang.String`)".

- [@jackson] `mapper.createParser(input).use { readValue(parser, shape); parser.nextToken() }` — the root-level trailing check that works: `nextToken()` is non-null when a second `---` document follows; without the check `readValue(InputStream, …)` silently drops every document after the first.

- [@jackson] A scalar alias (`key: *a`) decodes to the anchor's *name* (`"a"`), not the anchored value — anchor resolution exists only for object identity, never for scalars. No mapper feature rejects it; `ModelYaml` scans the SnakeYAML event stream and fails on any `AliasEvent` before decoding.

- [@snakeyaml] `ParserImpl(StreamReader(content), LoaderOptions())` then `getEvent()` until `StreamEndEvent` — the pre-decode event scan. `YAMLException` from the scan is wrapped like a decode failure.

- [@jackson] Unquoted condition and accepting-value scalars carry YAML 1.1 semantics (with a `List<JsonNode>` creator): `no` decodes as boolean `false`, `017` as integer 15, `0x1F` as 31, `.inf` fails at parse ("Malformed numeric value"), `_` and `x` as text, `null` and `~` as a null node, `99999999999999999999` as an integral node with `canConvertToLong() == false`. Quoting forces the string shape; `ArgPatternTest` pins the readings the format relies on.

- [@jackson] A creator parameter typed `List<JsonNode>` under `@JsonCreator(mode = DELEGATING)` receives a YAML sequence element by element; a mapping (`when: {a: 1}`) or a bare scalar (`when: true`) in that position fails with `MismatchedInputException` ("Cannot deserialize value of type `ArrayList<JsonNode>` from Object value" / "from Boolean value"), so the sequence shape needs no check in the creator.

- [@commons-value] `IPrimitiveVal` is a sealed interface over `StrVal(core: String)`, `IntVal(core: Long)`, `FloatVal(core: Double)` (data classes), `BoolVal` (private constructor; `BoolVal(value)` companion `invoke` returns the `T`/`F` singleton), `NullVal` (data object), and the `Unsure` enum. Equality is structural per class and never across classes: `IntVal(1) != FloatVal(1.0)`. `ArgPattern` stores `IPrimitiveVal?` and compares with `==`.

- [@jackson-kotlin] `Map<String, String>` with a null-valued key (`html:`) decodes to a map holding `null` — jackson-module-kotlin enforces non-nullability on creator parameters only, never on collection elements or map values (`KotlinFeature.StrictNullChecks` is off). `VocabularyMappingLoader` decodes into `Map<String, String?>` and rejects the null itself.

- [@jackson] A missing or empty-valued top-level section (`vulnTypes:` with no mapping) fails as a missing non-nullable creator parameter (`MismatchedInputException`); an empty vocabulary section is spelled `vulnTypes: []`; mapping sections use `{}`.

- [@jackson] `enum class Verification` decodes from the lowercase scalars `generated`/`manual` through `ACCEPT_CASE_INSENSITIVE_ENUMS`, the same path as `ReturnKind`; `ProvenanceLoaderTest` pins that `reviewed` fails.

- [@jackson] Mapping keys keep their scalar text: `no:` and `017:` decode as the strings `"no"` and `"017"`, so the YAML 1.1 boolean/octal readings that affect guard values never touch a name key.

- [@jackson] `ModelYaml.decode` reads the stream to its end and does not close it; `DocumentSetLoader` opens every stream through `use` so the opener's resources are released regardless of the decode outcome.

## Libraries

- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.2 — YAML backend; catalog alias `jackson-dataformat-yaml`.
- com.fasterxml.jackson.module:jackson-module-kotlin:2.22.2 — constructor binding, non-null creator parameters and value-class unwrapping; catalog alias `jackson-module-kotlin`.
- Jackson databind and annotations are transitive dependencies governed by the catalog's Jackson version.
- org.yaml:snakeyaml:2.7 — event scan rejecting aliases; declared directly because ModelYaml uses its parser; catalog alias `snakeyaml`.
- com.github.jhu-seclab-cobra:commons-value:0.1.1 — public argument scalar types; catalog alias `cobra-commons-value` and API scope.

## Developer instructions

- Recheck these APIs through the module tests after a Jackson dependency change.
- `JacksonSubjectProbeTest` checks wrapper routing, subtype opt-out, signature narrowing and unwrapped section binding.
- `ModelLoaderTest` checks factories used as creators; `VocabularyMappingLoaderTest` checks null map values and missing sections.

## Design-specific

- One entry form (`ModelEntry` data class, no `@JsonTypeInfo`); Every entry binds through the one companion creator; an unknown key (`name`, `find`, `where`, `model`) is a stray key; `JsonMappingException` (the any-setter entry).
- Unknown discriminator value; Rejected; `InvalidTypeIdException`.
- Missing non-nullable constructor parameter; Rejected; `MismatchedInputException`.
- `@JvmInline value class` over `String`; Decodes from the bare scalar; no custom deserializer.
- `init { require(...) }` violation; Rejected; `ValueInstantiationException`.
- Unknown enum constant; Rejected — never widens to a default; `InvalidFormatException`.
- `Regex(pattern)` in a property initializer, invalid pattern; Rejected at decode — `PatternSyntaxException` wrapped; `ValueInstantiationException`.
- Pattern dialect of every model regular expression (`KeyPattern`, `CharacterClass`); `kotlin.text.Regex` compiles `java.util.regex.Pattern` syntax; a POSIX bracket expression is a union of literal characters there, not a class: `[[:alpha:]]` matches exactly `:`, `a`, `h`, `l`, `p`; — (silent; `\p{Alpha}` is the class).
- `@JsonCreator @JvmStatic fun parse(raw: String)` on a sealed interface companion; Decodes string scalars (`"return"` / `"argument(n)"`) to the sealed subtypes.
- Field typed as the *subtype* (`Port.Argument`); Does **not** consult the supertype's creator — the subtype needs its own `@JsonCreator` companion factory; `MismatchedInputException` without it.
- Malformed port string (creator throws `IllegalArgumentException`); Rejected at decode; `ValueInstantiationException`.
- Synonym port-pair spellings; All four spellings (`from`/`input`/`to`/`output`) as nullable creator parameters; a require-exactly-one check per side; `ValueInstantiationException` on a doubled or missing side.
- `@JsonAlias` for the synonym pair; Unusable: a mapping naming both spellings decodes silently, later key overwriting the earlier; the invalid form is not rejected by that external API.
- Creator parameter typed `JsonNode`; Receives the raw tree; `isBoolean`/`isIntegralNumber`/`isTextual` narrow the scalar shapes of a condition element and of a check's accepting value through one reader, any other shape throws in the creator; `ValueInstantiationException`.
- Non-null `JsonNode` creator parameter (`ArgumentCheck.accepts`); An explicit YAML `null` arrives as a `NullNode` and reads as the null scalar; an absent key fails as a missing creator parameter (`ArgumentCheckTest`); `MismatchedInputException`.
- Integral node wider than `Long`; `isIntegralNumber` is true for a `BigInteger` node and `longValue()` silently wraps — `canConvertToLong()` gates the narrowing; out-of-range throws in the creator; `ValueInstantiationException`.
- Optional `when` field (`@JsonProperty("when")`, nullable `ArgPattern`) on the entry creator; Decodes a sequence when present, stays null when absent.
- `ArgPattern` `init` failure (empty sequence, all wildcards, nested sequence or mapping element); Rejected at decode; `ValueInstantiationException`.
- Sealed interface + `@JsonTypeInfo(Id.NAME, As.WRAPPER_OBJECT)` + `@JsonSubTypes`; One-key mapping (`function: strlen`) routes the wrapper key to the named subtype's delegating string creator.
- Unknown wrapper key (`trait: foo`); Rejected; `InvalidTypeIdException`.
- `@JsonTypeInfo(use = Id.NONE)` on a sealed subtype of the wrapper-routed interface; A position typed by the subtype (`Set<ClassSubject>`) decodes the bare scalar through the subtype's delegating creator; a position typed by the interface keeps the wrapper routing.
- `@JsonTypeInfo(use = Id.NONE)` on the property instead (param, field, or getter target); Unusable: does not override the type info the element class inherits; a bare element fails; `MismatchedInputException`.
- `require(...)` inside a wrapper-routed delegating creator; Rejected at decode; `ValueInstantiationException`.
- `init { require(...) }` in a `@JvmInline value class`; Rejected at decode — the value-class unwrapping path wraps the `IllegalArgumentException` in a plain `JsonMappingException`, **not** `ValueInstantiationException`; `JsonMappingException`.
- Creator parameters `(subject: ModelSubject, signature: JsonNode?)` + `ModelYaml.narrow(node, subtype)` (`treeToValue`) inside the creator; Narrows the signature mapping by the sibling kind, keeping the mapper's strictness.
- Stray key inside a `treeToValue`-narrowed node; Rejected — the inner failure propagates unwrapped, not re-wrapped as `ValueInstantiationException`; `UnrecognizedPropertyException`.
- `@JsonUnwrapped` parameter on a companion `@JsonCreator` ; Gathers the flat sibling fields into the holder type beside the named creator parameters.
- Nullable `JsonNode` field of the `@JsonUnwrapped` holder (`SectionFields.effects`); An absent key leaves the field null, an explicit YAML `null` or `~` arrives as a `NullNode`, so omission and an explicit null stay distinct and the holder rejects the null node with every value other than the empty sequence (`ModelEntryEffectsTest`); `ValueInstantiationException`.
- Stray key on a form with an `@JsonUnwrapped` creator parameter; Silently absorbed — the unwrapped path funnels unknown keys past `FAIL_ON_UNKNOWN_PROPERTIES`; a throwing `@JsonAnySetter` on the holder restores rejection; `JsonMappingException` (plain, wrapping the setter's `IllegalArgumentException` — not `UnrecognizedPropertyException`).
- Companion `operator fun invoke` as `@JsonCreator` on a private-constructor `data class` (`@ConsistentCopyVisibility`); Property-based binding through the factory with defaulted parameters; call sites keep constructor syntax while normalization lives in the factory.
- Doubled key in one mapping (`STRICT_DUPLICATE_DETECTION`); Rejected at parse — never decodes last-wins, including a doubled synonym spelling (`from:` twice) the `exactlyOne` check cannot see; `JsonParseException`.
- Second `---` document in one stream; Rejected by the root-level parser-exhaustion check in `ModelYaml.decode` — entries after the first document never drop silently; `IllegalArgumentException` (direct, not wrapped).

- Binding failure types extend `JsonMappingException`; parse-level `JsonParseException` is a separate subtype. Both extend `JsonProcessingException`, so one catch in `ModelYaml.decode` covers every decode failure, which lets a caller present a single `IllegalArgumentException` for "malformed entry".
- Three failures differ from `ValueInstantiationException`: a value-class `init` failure, a stray key under `treeToValue`, and a stray key rejected by the unwrapped holder's any-setter — but all extend `JsonProcessingException`, so the single-catch contract in `ModelYaml` holds.
