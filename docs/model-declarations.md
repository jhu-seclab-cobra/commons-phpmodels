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
  - *function*, *method* — parameter list, declared return type. A
    function may also state how php-src compiles a call of it to one opcode:
    the argument counts that select a frameless function, the argument
    counts that always compile to one opcode reading two or more arguments,
    and the parameter whose constant value php-src compiles into a rope. A
    method declaration states none of them.
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
- **Declared Type** — A PHP type declaration in the grammar of the PHP
  manual: a single type, a nullable type (`?T`), a union (`A|B`), an
  intersection of class names (`A&B`), or a union whose members are single
  types or parenthesized intersections (`(A&B)|null`). A single type is a
  keyword type (string, int, float, bool, true, false, array, object,
  callable, iterable, resource, mixed, void, never, null) or a class name.
  Read as a union of alternatives, each an intersection of single types: `?T`
  is `T|null`, a single type is a union of one alternative. Spelled without
  whitespace, as PHP reflection prints it. Stored losslessly; the four-kind returns classification is derived from it at
  load and never stored beside it.
- **Generated Layer** — The lowest configuration layer of a consumer: files
  emitted by an extraction producer from upstream sources. Structurally
  marked by provenance (directory and file header naming source and producer
  version); never hand-edited — a correction is a higher-layer entry
  overriding per (subject, condition, unit).

### Subject Kinds

| Kind | Spelling | Identity | Case |
|------|----------|----------|------|
| function | `strlen` | name | folded |
| class | `mysqli` | name | folded |
| method | `mysqli::query` | (class, name) | both folded |
| class constant | `mysqli::MYSQLI_REPORT_ERROR` | (class, name) | class folded, name sensitive |
| property | `mysqli::$insert_id` | (class, name) | class folded, name sensitive |
| constant | `PHP_EOL` | name | sensitive; root keywords fold |
| predefined variable | `$_GET` | name | folded |

The root keyword constants `TRUE`, `FALSE`, and `NULL` are the one exception
in the constant kind: PHP matches them ASCII-case-insensitively (php-src
`zend_get_special_const`), so every case variant of one keyword, with or
without a leading `\`, is one subject. Every other global constant, and every
class constant even when named `true`, keeps its exact spelling.

The member spelling is PHP's own static-reference grammar: exactly one `::`,
both sides non-empty; a property name carries the `$` prefix after `::`; a
predefined variable carries the `$` prefix. The entry key names the kind —
the spelling encodes identity only, never kind. Leading namespace slashes
are stripped before folding. Any other shape is a load failure.

### Return Classification

Derived at load from the declared return type; never stored in a file that
carries a signature. Each alternative classifies by the table; the type
classifies as the join of its alternatives: the common classification when
every alternative has it, otherwise any. `int|float` is num; `int|false` and
`?string` are any. A consumer needing more than the join reads the
alternatives of the declared type itself.

| Alternative | Classification |
|-------------|----------------|
| string | str |
| int, float | num |
| bool, true, false | bool |
| every other single type, every intersection | any |

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Declaration Entry | PHP declaration | describes | 1:1 | One entry per declaration; members reference their class by identity, never by containment |
| Signature | assertion sections | completes | 1:0..1 | A declared return type supplies the returns classification when propagation is asserted |
| Generated Layer | Configuration Source | is lowest | 1:1 | Every hand-written layer overrides it per (subject, condition, unit) |

## Invariants

- Subject kind ⟺ PHP declaration kind. No declaration nests inside another
  entry; a member's class is half of its identity, spelled with `::`.
- The `::` grammar is closed: exactly one separator, both sides non-empty,
  `$` mandatory for property names and forbidden elsewhere. Violation is a
  load failure, never a guess.
- Case folding is fixed per kind by the table above, applied once at the
  load boundary; PHP's own case rules are the authority. Exception:
  predefined variable names are case-sensitive in PHP but fold here — the
  superglobal set is closed and collision-free under folding, so lookups
  tolerate spelling variance without merging two distinct variables.
- The signature section is one override unit: a higher layer replacing a
  subject's signature replaces it whole.
- A signature never produces a taint statement. Sources, sinks, and
  sanitizers are hand-declared assertions in every layer.
- The declared-type grammar is closed; a spelling outside it is a load
  failure, never read as `mixed`. The PHP composition rules hold: `mixed`,
  `void`, and `never` stand alone; `?` applies to one single type other
  than `null` and `mixed`; an intersection joins class names only. A file
  states the upstream declaration as declared, never a simplified form.
- Assertions attach uniformly: every subject kind admits the sections its
  callability allows — returns/propagation only for function and method;
  sources for any kind that produces a value.
- Completeness boundary: the format stores exactly the fields the six stub
  record kinds of the upstream extraction carry. Additional upstream
  information (attributes, deprecations, conditional types) enters by
  extending the extraction and regenerating — never as a hand-maintained
  addition to generated files.
- Generated files are regenerable at any time from upstream sources; no
  information exists only in a generated file.
- Value-unit coupling — the unit stays one exhaustive statement
  ([model.md](model.md)); the signature changes only where its returns half
  comes from:
  - Signature present, propagation declared → the unit is asserted: returns
    derived from the declared return type, flows and characters exactly as
    declared.
  - Signature present, no propagation → **no value unit**. Existence
    is asserted; value flow stays at the consumer's conservative default in
    which every argument influences the result. Absence of a flow annotation
    in the upstream data is not purity, so a characters section here is a
    load failure: it has no returns to complete.
  - Signature present, explicit `returns` → load failure: one fact, one
    source. A purity assertion (returns with an empty flow set) is a
    hand-written statement and belongs in a higher layer without a signature.
  - No signature (hand-written form) → explicit returns asserts the unit
    exhaustively.


## Cross-Structure Contracts

- **Existence ≡ signature entry.** A consumer's "does this builtin exist"
  question is answered by the presence of a declaration entry, the same
  lookup that serves assertions. No separate declaration store exists.
- **Truth and view.** Files store declared PHP types; consumers see derived
  classifications. Files store the full declaration; a consumer may retain
  only the projection it consumes.

Concept and rationale: [concept.md](concept.md). Assertion axis:
[model.md](model.md). Software structure:
[design-declarations.md](design-declarations.md).
