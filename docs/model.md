# PHP Models — Domain Model

Semantics of the assertion axis: sections, subjects, ports, and the
two-axis taint vocabulary of Source kind entries and Vulnerability type entries. Declaration axis: [model-declarations.md](model-declarations.md).
Conditional entries: [model-conditions.md](model-conditions.md). Document Set instances and
Vocabulary Mapping: [model-sets.md](model-sets.md).

## Entities

- **[Source kind](glossary/glossary-vocabulary.md#source-kind)** — Supplies a declared category for source effects. Existence
  condition: declared in the vocabulary. Identity is its declared name,
  lowercased. Carries a human description.
- **[Vulnerability type](glossary/glossary-vocabulary.md#vulnerability-type)** — Supplies a declared sink sensitivity. Existence
  condition: declared in the vocabulary. Identity is its declared name,
  lowercased. Sink-side only. Its optional Tokenizer language obeys
  [model-tokenizer.md](model-tokenizer.md).
- **[Vocabulary](glossary/glossary-vocabulary.md#vocabulary)** — Validates the names Models and Policy rows reference. Every Source kind or Vulnerability type
  referenced by a policy row or a model must belong to it. Sole authority for what names exist.
- **[Matching Subject](glossary/glossary-models.md#matching-subject)** — The PHP declaration or library operation a model identifies. Existence
  condition: a declared subject kind together with exactly the identity fields that kind requires. Seven kinds with PHP-native spellings:
  [model-declarations.md](model-declarations.md). Callable declaration kinds are
  *function* and *method*; they admit calls, ports, and conditions. The operation
  kind names `dynamic_invocation`, admits only unconditional callee sinks and no signature.
- **[Port](glossary/glossary-models.md#port)** — An explicitly named location in a call to a callable subject:
  *argument(n)* with n ≥ 0, *callee* — the dynamic invocation selector — *this* — the receiver of a call to a method —
  *return*, or a *callback port*: *callback(argument(n)).parameter(m)*
  with m ≥ 0 — the m-th argument the subject passes when it calls the
  callable supplied at *argument(n)* — or *callback(argument(n)).return* —
  the value that call returns. The closed set of positional references; no
  other location exists. The *input ports* — arguments, the selector, the receiver, and
  callback returns — are the ports values enter the subject through; *return* and callback parameters are never input ports. A subject calls
  the callables its value unit explicitly declares as invoked. A callable type alone
  states no invocation. Callback ports describe input and return flows; the invocation
  declaration supplies the callback inputs, including an element of each array in an argument tail.
- **[Model](glossary/glossary-models.md#model)** — The single declarative statement attached to one Matching Subject. Composed of sections; existence condition: at least one section
  present. An entry may carry a Condition, making it one alternative of
  the subject's model ([model-conditions.md](model-conditions.md)). Section kinds:
  - *returns* — the classification of the call result.
  - *propagation* — a set of declared flows, each from one input port to a
    different port of the same call. Together with *returns* it
    forms the **[Value unit](glossary/glossary-models.md#value-unit)**: declaring *returns* asserts the flow
    set exhaustively (absent propagation means no flow); *propagation*
    without *returns* does not exist. Optional *callbacks* declarations belong to the same whole unit; absence declares no immediate invocation. Each declaration selects a callable argument and a supply of its input values. A unit holds a non-empty set of
    classifications; its join: [model-conditions.md](model-conditions.md).
  - *characters*  — the bytes the call result may
    hold: only bytes of the listed argument ports (*of*), only bytes of one
    Character Class (*within*), or both, each bound narrowing the other.
    The bytes of an argument are those of the strings the call converts it
    to: a scalar's string form, an object's `__toString` result, and for an
    array the call reads element by element, each element's conversion, a
    nested array converting to `Array` (PHP 8.5.11 gives `Arrayb` for
    `str_replace(["a"], [["x"]], "ab")`). Third part of the
    Value unit: it requires *returns*, and its absence states no
    bound. `basename` declares *of* `argument(0)` *within* every byte but
    `/`: PHP 8.5.11 gives `passwd` for `basename("../../etc/passwd")`.
  - *sources* — each element produces a non-empty set of Source kind entries at
    one production site. The site defaults per subject kind — the call
    result for a callable, the variable's value for a predefined variable —
    and a callable's element may instead name one argument port as its site
    (a by-reference out-parameter). An element may restrict production to
    the array elements whose key satisfies one of its Key Patterns; absent
    key patterns mean the whole value.
  - *sinks* — each element names one argument or callee port consumed under one
    Vulnerability type.
  - *sanitizers* — each element neutralizes Vulnerability type entries in the call
    result, which still carries its input value, or is a *check* : one argument port and the result value accepting it,
    with the Vulnerability type entries the accepted argument is free of, the Character
    Class its bytes belong to, or the type set it holds; a type set rules out the accepting value for an argument of any other type.
  - *throws* — the set of exception classes the subject's own
    implementation may throw. It excludes three kinds of throw, each decided
    elsewhere: binding the arguments to the declared parameters (the
    argument count, the declared parameter types), user code the subject
    calls (a callback, a magic method, a user error handler), and the
    conversion of a value between PHP types inside the implementation (an
    object without `__toString` read as a string), decided by the converted
    value. The empty set states that the implementation throws none.
  - *signature* — the descriptive section:
    [model-declarations.md](model-declarations.md).
- **[Key Pattern](glossary/glossary-models.md#key-pattern)** — A regular expression over one array key at a source's
  production site. Satisfied only when the entire key matches,
  case-sensitively — array keys are runtime data, not identifiers, so no
  case folding applies.
- **[Character Class](glossary/glossary-models.md#character-class)** — A regular expression matched against one byte read as one
  Latin-1 character; the class holds every byte it matches: `[^/]`, `[0-9a-f]`.
- **[Policy](glossary/glossary-vocabulary.md#policy)** — Supplies the enabled source-to-sink category relation. One instance serves the whole analysis.
  Rows sharing a Source kind accumulate by union.
- **[Override Unit](glossary/glossary-models.md#override-unit)** — The granularity at which one declaration replaces
  another for the same subject: the Value unit (returns together
  with propagation, characters and callbacks), sources, sinks, sanitizers, throws, or the signature. A
  declaration replaces a unit whole, never merges inside one. How layered
  configuration sources apply this granularity is consumer behavior; the
  unit boundaries are fixed here.

An absent sources, sinks or sanitizers Section asserts nothing about that aspect.
Propagation describes argument flow independently of these Sections.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Model | Matching Subject | identifies | 1:1 | The model states this subject's semantics and taint effects |
| Propagation | Port × Port | flows | N:— | Source kind entries at the from-port reach the to-port; an undeclared pair carries nothing |
| Model (sources) | Source kind | produces | M:N | The source introduces these Source kind entries at its production site |
| Model (characters) | Port, Character Class | bounds | N:1 | Every byte of the call result is a byte of a listed port's value and a member of the class |
| Model (sinks) | Port × Vulnerability type | consumes under | N:M | The named argument or callee port is sensitive to that Vulnerability type |
| Model (sanitizers) | Vulnerability type | neutralizes | M:N | These Vulnerability type entries removed from the carried value, or, for a check, from the argument a call returning the accepting value accepted |
| Model (throws) | class | may throw | M:N | A call's own implementation may throw an instance of this exact class; a class outside the set is never thrown by it |
| Policy | Source kind × Vulnerability type | enables | M:N | This Source kind reaching a sink of this Vulnerability type is a vulnerability |
| Model / Policy | Vocabulary | references | N:1 | Every named Source kind and Vulnerability type must be declared in the vocabulary |

## State Model

### Vocabulary Lifecycle

| State | Trigger | Target |
|-------|---------|--------|
| Undeclared | — | a referenced name that is not declared → load failure |
| Declared | vocabulary parsed | normalized, referenceable |
| Referenced | policy row or model names it | validated against Declared |

What a Source kind set does along a value's flow — production, propagation,
sanitization, the vulnerability verdict — is the consuming analysis's state
model, driven by the declarations defined here.

## Invariants

- Each model identifies exactly one subject, and declares at least one section. A
  field outside a section's closed set is a load failure, not an ignored key.
- Each model declares exactly one subject kind. The subject kind fixes the
  identity fields exactly.
- The port set is closed: *argument(n)* with n ≥ 0, *callee*, *this*, *return*,
  *callback(argument(n)).parameter(m)*, or *callback(argument(n)).return*
  with n, m ≥ 0. Any other port name, and any bare integer position, is a
  load failure.
- The receiver port *this* exists only in a call to a method subject: a
  model for any other subject naming it, in any section, is a load failure.
- A propagation's from-port is an input port; its to-port is any different
  port. *return* as a from-port, and from equals to, are load failures.
- A sink names an argument of a callable declaration or the callee input of
  the dynamic invocation operation; neither the receiver nor the return is a sink.
  A callee port in declaration behavior or operation behavior besides callee sinks is a load failure.
- A callbacks section requires returns, is non-empty when declared, and is admitted only for callable subjects. Its declarations are replaced with the whole Value unit and unioned when candidate units join.
- A propagation section requires a returns section in the same declaration.
  Propagation without returns is a load failure. Exception: a declaration
  carrying a signature derives its returns half from the declared return
  type ([model-declarations.md](model-declarations.md)).
- A characters section requires a returns section, as propagation does,
  and declares *of*, *within*, or both. An *of* list is non-empty and
  names argument ports only, each the from-port of a declared flow into
  *return*: a byte copied into the result is a flow into it. The call
  converts an argument to a string, whereas a method reads its receiver's
  state without converting it, so the receiver has no bytes to name. A
  *within* class is a regular expression that compiles and matches at
  least one byte.
- Every regular expression of a model, a Key Pattern or a Character Class,
  follows the one dialect [impl.md](impl.md) names.
- The *returns*, *propagation*, *characters*, and *throws* sections are declared only for
  callable subjects. A class subject declares no assertion section. Every other non-callable
  kind — constant, class constant, property, predefined variable — admits only *sources*.
- A sources section produces a non-empty Source kind set. A sinks section
  is non-empty and each element names one argument port and one
  Vulnerability type. A sanitizers element names a non-empty Vulnerability type set, or a check: one argument port, a scalar accepting value, and
  Vulnerability type entries, a class, or a type set other than `void`. A throws
  section is the one section a declared empty value is a statement of: no
  class, distinct from the absent section, which asserts nothing. Each
  throws element is a class name, spelled and folded as a class subject.
- A source element's explicit production site is an argument port of a
  callable subject: explicit *return* is a load failure (the default
  already states it), and a non-callable subject admits no explicit site.
- A declared key-pattern set is non-empty and each pattern is a valid
  regular expression. No rule ties key patterns to a declared type — the
  format does not require the production site to be a declared array.
- No model declares an anchor. Where a query starts is a property of the
  consuming analysis, never of a model.
- Every closed-vocabulary field admits only its declared members. An
  unrecognized value is a load failure, never a widening default.
- Every Source kind and Vulnerability type referenced by any policy row or
  model is declared in the vocabulary. An undeclared reference is a load
  failure.
- Policy enables relates only declared Source kind entries to declared Vulnerability type entries.

## Cross-Structure Contracts

- **Superglobal ≡ sources section over a variable subject.** An
  external-input superglobal is an ordinary model whose sources section
  produces the user-input Source kind, differing from a function source
  only in its declared subject kind.
- **Normalized identity.** Source kind and Vulnerability type identifiers compare by lowercased name; the loader verifies membership in the Vocabulary.

Concept and rationale: [concept.md](concept.md). Software structure:
[design.md](design.md), [design-subjects.md](design-subjects.md).
