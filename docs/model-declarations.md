# PHP Models — Declaration Entries

The declaration axis of the model format: entries that describe what a PHP
declaration looks like, alongside the assertion axis defined in
[model.md](model.md). One entry per PHP declaration; subject kind ⟺ PHP
declaration kind, with no nesting of one declaration inside another.

## Entities

- **Declaration Entry** — An entry whose subject names one PHP declaration
  and whose signature section describes it. Existence condition: a subject
  plus at least one section; a signature section alone is a complete entry —
  it states "this declaration exists", which is the existence statement a
  consumer consults for unresolved names.
- **Signature** — The descriptive section of an entry: what the declaration
  looks like, never what an analysis believes about it. Content per subject
  kind:
  - *function*, *method* — parameter list, declared return type.
  - *class* — classifier (class, interface, trait, enum), parent, interfaces.
  - *constant*, *class constant* — declared type, literal value. The value
    is the constant's spelled literal, stored losslessly; absent when the
    declaring source does not state one (hand-written entries). Consumers
    that materialize built-in constants read it; it never influences the
    returns classification.
  - *property* — declared type, visibility, static.
  - *predefined variable* — no signature; superglobals are hand-declared.
- **Parameter** — One declared parameter: name, declared type, optional,
  by-reference, variadic. Position is list order; position 0 is the first
  declared parameter.
- **Declared Type** — A PHP type name from the closed type vocabulary
  (string, int, float, bool, array, object, callable, resource, mixed, void,
  null, iterable, or a class name). Stored losslessly; the four-kind returns
  classification is derived from it at load and never stored beside it.
- **Generated Layer** — The lowest configuration layer of a consumer: files
  emitted by an extraction producer from upstream stub sources. Structurally
  marked by provenance (directory and file header naming source and producer
  version); never hand-edited — a correction is a higher-layer entry
  overriding per (subject, guard, unit).

## Subject Kinds

| Kind | Spelling | Identity | Case |
|------|----------|----------|------|
| function | `strlen` | name | folded |
| class | `mysqli` | name | folded |
| method | `mysqli::query` | (class, name) | both folded |
| class constant | `mysqli::MYSQLI_REPORT_ERROR` | (class, name) | class folded, name sensitive |
| property | `mysqli::$insert_id` | (class, name) | class folded, name sensitive |
| constant | `PHP_EOL` | name | sensitive |
| predefined variable | `$_GET` | name | folded |

The member spelling is PHP's own static-reference grammar: exactly one `::`,
both sides non-empty; a property name carries the `$` prefix after `::`; a
predefined variable carries the `$` prefix. The entry key names the kind —
the spelling encodes identity only, never kind. Leading namespace slashes
are stripped before folding. Any other shape is a load failure.

## Return Classification

Derived at load from the declared return type; never stored in a file that
carries a signature.

| Declared return type | Classification |
|----------------------|----------------|
| string | str |
| int, float | num |
| bool | bool |
| every other type | any |

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Declaration Entry | PHP declaration | describes | 1:1 | One entry per declaration; members reference their class by identity, never by containment |
| Signature | assertion sections | completes | 1:0..1 | A declared return type supplies the returns classification when propagation is asserted |
| Generated Layer | Configuration Source | is lowest | 1:1 | Every hand-written layer overrides it per (subject, guard, unit) |

## Value-Semantics Coupling

The value-semantics unit stays one exhaustive statement
([model.md](model.md)); the signature changes only where its returns half
comes from:

- Signature present, propagation declared → the unit is asserted: returns
  derived from the declared return type, flows exactly as declared.
- Signature present, no propagation → **no value-semantics unit**. Existence
  is asserted; value flow stays at the consumer's conservative default in
  which every argument influences the result. Absence of a flow annotation
  in the upstream data is not purity.
- Signature present, explicit `returns` → load failure: one fact, one
  source. A purity assertion (returns with an empty flow set) is a
  hand-written statement and belongs in a higher layer without a signature.
- No signature (hand-written form) → unchanged: explicit returns asserts the
  unit exhaustively.

## Invariants

- Subject kind ⟺ PHP declaration kind. No declaration nests inside another
  entry; a member's class is half of its identity, spelled with `::`.
- The `::` grammar is closed: exactly one separator, both sides non-empty,
  `$` mandatory for property names and forbidden elsewhere. Violation is a
  load failure, never a guess.
- Case folding is fixed per kind by the table above, applied once at the
  load boundary; PHP's own case rules are the authority.
- The signature section is one override unit: a higher layer replacing a
  subject's signature replaces it whole.
- A signature never produces a taint statement. Sources, sinks, and
  sanitizers are hand-declared assertions in every layer.
- The type vocabulary for declared types is closed; an unknown type name is
  a load failure, never a widening default.
- Assertions attach uniformly: every subject kind admits the sections its
  callability allows — returns/propagation only for function and method;
  sources for any kind that produces a value.
- Completeness boundary: the format stores exactly the fields the six stub
  record kinds of the upstream extraction carry today. Additional upstream
  information (attributes, deprecations, conditional types) enters by
  extending the extraction and regenerating — never as a hand-maintained
  addition to generated files.
- Generated files are regenerable at any time from upstream sources; no
  information exists only in a generated file.

## Cross-Structure Contracts

- **Existence ≡ signature entry.** A consumer's "does this builtin exist"
  question is answered by the presence of a declaration entry, the same
  lookup that serves assertions. No separate declaration store exists.
- **Layer 0 entries are explicit models.** They take the explicit side of
  every precedence rule in [model.md](model.md): a generator's answer yields
  to a layer-0 entry for the same subject and unit, and every higher layer
  overrides layer 0 by ordinary layer order.
- **Truth and view.** Files store declared PHP types; consumers see derived
  classifications. Files store the full declaration; a consumer may retain
  only the projection it consumes.

Concept and rationale: [concept.md](concept.md). Assertion axis:
[model.md](model.md). Software structure:
[design-declarations.md](design-declarations.md).
