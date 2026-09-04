# PHP Models — Domain Model

Semantics of the assertion axis: sections, subjects, ports, and the two-axis
color model. Declaration axis: [model-declarations.md](model-declarations.md).
Guarded branches: [model-guards.md](model-guards.md). Document sets and
category mapping: [model-sets.md](model-sets.md).

## Entities

- **Origin Color** — A declared category of untrusted provenance. Existence
  condition: declared in the vocabulary. Identity is its declared name,
  lowercased. Carries a human description.
- **Danger Category** — A declared category of vulnerability. Existence
  condition: declared in the vocabulary. Identity is its declared name,
  lowercased. Sink-side only.
- **Vocabulary** — The two closed sets: all declared Origin Colors and all
  declared Danger Categories. Every color or category referenced by a policy
  row or a model must belong to it. Sole authority for what names exist.
- **Matching Subject** — The PHP declaration a model identifies. Existence
  condition: a declared subject kind together with exactly the identity
  fields that kind requires. Seven kinds with PHP-native spellings:
  [model-declarations.md](model-declarations.md). Callable kinds are
  *function* and *method*; only they admit calls, ports, and guards.
- **Port** — An explicitly named location in a call to a callable subject:
  *argument(n)* with n ≥ 0, *this* — the receiver of a call to a method —
  or *return*. The closed set of positional references; no other location
  exists. The *input ports* — arguments and the receiver — are the ports a
  call supplies values through; *return* is never an input port.
- **Model** — The single declarative statement attached to one Matching
  Subject. Composed of sections; existence condition: at least one section
  present. An entry may carry a When Guard, making it one branch of the
  subject's model ([model-guards.md](model-guards.md)). Section kinds:
  - *returns* — the classification of the call result.
  - *propagation* — a set of declared flows, each from one input port to a
    different port of the same call. Together with *returns* it
    forms the **value-semantics unit**: declaring *returns* asserts the flow
    set exhaustively (absent propagation means no flow); *propagation*
    without *returns* does not exist.
  - *sources* — each element produces a non-empty set of Origin Colors at
    one production site. The site defaults per subject kind — the call
    result for a callable, the variable's value for a predefined variable —
    and a callable's element may instead name one argument port as its site
    (a by-reference out-parameter). An element may restrict production to
    the array elements whose key satisfies one of its Key Patterns; absent
    key patterns mean the whole value.
  - *sinks* — each element names one argument port consumed under one Danger
    Category.
  - *sanitizers* — the subject neutralizes one or more Danger Categories; it
    still carries its input value to its result.
  - *signature* — the descriptive section:
    [model-declarations.md](model-declarations.md).
- **Name Constraint** — A regular expression over one identity field of a
  subject kind. Satisfied only when the entire case-folded field matches; a
  partial match does not satisfy.
- **Key Pattern** — A regular expression over one array key at a source's
  production site. Satisfied only when the entire key matches,
  case-sensitively — array keys are runtime data, not identifiers, so no
  case folding applies.
- **Model Generator** — One declaration denoting one model per satisfying
  subject: a generator name, the subject kind to find, a constraint set every
  generated subject satisfies, and one model body. The name is the
  generator's identity — unique within one load, and the handle by which
  load failures and generated models are traced back to their generator. A
  generated model is the same statement as a written one; no consumer
  distinguishes them.
- **Policy** — The global relation from each Origin Color to the set of
  Danger Categories it can trigger. One instance for the whole analysis.
  Rows sharing an origin accumulate (union of enabled categories).
- **Override Unit** — The granularity at which one declaration replaces
  another for the same subject: the value-semantics unit (returns together
  with propagation), sources, sinks, sanitizers, or the signature. A
  declaration replaces a unit whole, never merges inside one. How layered
  configuration sources apply this granularity is consumer behavior; the
  unit boundaries are fixed here.

A subject whose model has no taint section has no taint effect; the absence
is the statement. No section expresses "carries its argument" as a role —
argument flow is the propagation section, and having no taint effect is not
itself an effect.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Model | Matching Subject | identifies | 1:1 | The model states this subject's semantics and taint effects |
| Propagation | Port × Port | flows | N:— | Colors at the from-port reach the to-port; an undeclared pair carries nothing |
| Model (sources) | Origin Color | produces | M:N | The source introduces these colors at its production site |
| Model (sinks) | Port × Danger Category | consumes under | N:M | The named argument port is sensitive to that category |
| Model (sanitizers) | Danger Category | neutralizes | M:N | Danger removed for these categories from the carried value |
| Policy | Origin Color × Danger Category | enables | M:N | This color reaching a sink of this category is a vulnerability |
| Model Generator | Name Constraint | constrains by | 1:N | A subject satisfies the generator only when every constraint holds |
| Model Generator | Model | generates | 1:N | One model per satisfying subject, carrying the generator's body |
| Model / Policy | Vocabulary | references | N:1 | Every named color/category must be declared in the vocabulary |

## State Model

### Vocabulary Lifecycle

| State | Trigger | Target |
|-------|---------|--------|
| Undeclared | — | a referenced name that is not declared → load failure |
| Declared | vocabulary parsed | interned, referenceable |
| Referenced | policy row or model names it | validated against Declared |

What a color set does along a value's flow — production, propagation,
sanitization, the vulnerability verdict — is the consuming analysis's state
model, driven by the declarations defined here.

## Invariants

- Each model identifies exactly one subject, and declares at least one
  section. A field outside a section's closed set is a load failure, not an
  ignored key.
- Each model declares exactly one subject kind. The subject kind fixes the
  identity fields exactly.
- The port set is closed: *argument(n)* with n ≥ 0, *this*, or *return*.
  Any other port name, and any bare integer position, is a load failure.
- The receiver port *this* exists only in a call to a method subject: a
  model for any other subject naming it, in any section, is a load failure.
- A propagation's from-port is an input port; its to-port is any different
  port. *return* as a from-port, and from equals to, are load failures.
- A sink element and a When Guard name argument ports only; neither admits
  the receiver or the return port.
- A propagation section requires a returns section in the same declaration.
  Propagation without returns is a load failure. Exception: a declaration
  carrying a signature derives its returns half from the declared return
  type ([model-declarations.md](model-declarations.md)).
- The *returns* and *propagation* sections are declared only for callable
  subjects. A class subject declares no assertion section. Every other
  non-callable kind — constant, class constant, property, predefined
  variable — admits only the *sources* section.
- A sources section produces a non-empty color set. A sinks section is
  non-empty and each element names one argument port and one category. A
  sanitizers section neutralizes a non-empty category set.
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
- Every Origin Color and Danger Category referenced by any policy row or
  model is declared in the vocabulary. An undeclared reference is a load
  failure.
- Policy enables relates only declared colors to declared categories.
- Each Model Generator declares a generator name, exactly one subject kind
  to find, at least a name constraint, and one model body satisfying every
  model invariant. The body is validated at decode, before any subject is
  matched. A generator no subject satisfies generates nothing and is not a
  failure.
- The constraint set is closed: a name constraint for every findable kind, a
  class constraint only for the method kind. Any other constraint is a load
  failure. A constraint pattern is matched against the entire case-folded
  identity field; a pattern that is not a valid regular expression is a load
  failure.
- Generator name uniqueness spans one whole load across layers; it is the
  loading consumer's check, because no single document sees every layer.

## Cross-Structure Contracts

- **Superglobal ≡ sources section over a variable subject.** An
  external-input superglobal is an ordinary model whose sources section
  produces the user-input Origin Color, differing from a function source
  only in its declared subject kind.
- **Generated ≡ written.** A generator denotes the family of models over all
  subjects satisfying its constraints; whether that family is materialized
  eagerly or resolved per lookup is not observable to consumers of the
  format.
- **Interned identity.** After load, colors and categories are referenced by
  interned identity, not by raw string, so a name mismatch cannot occur past
  the load boundary.

Concept and rationale: [concept.md](concept.md). Software structure:
[design.md](design.md), [design-generators.md](design-generators.md).
