# PHP Models — Declaration Handler Types

The handler values of [ClassSignature](design-declarations.md#signatureinfo-sealed).
Meanings: [Effective Handler Facts](model-declarations-handlers.md).

## Design Overview

- **Classes:** public immutable `HandlerTable`, `HandlerFact`, `HandlerTarget`.
- **Enums:** public `HandlerSlot`, `HandlerRequest`, `StringDispatch`.
- **Relationships:** the signature holds table alternatives; a table holds facts;
  a present fact holds targets with their own result kinds and dispatch.
- **Dependency roles:** data holders in `edu.jhu.cobra.commons.phpmodels`.
- **Exceptions:** `IllegalArgumentException` at the existing decode boundary.

## Class / Type Specifications

### HandlerTable

`id: String`, `facts: List<HandlerFact>`.
The id identifies source provenance: relative source path and table symbol,
with a deterministic source-branch suffix for different effective contents.
An unresolved reachable table uses its selection site's source identity and
has no facts. The id is opaque to consumers. A table with partial coverage
omits its unestablished requests; list order gives no priority.

**Validation (`init`):** id non-blank; no two facts share a slot and request,
and no slot has both an absent fact and a present fact, including different
requests. A whole-slot absence is the sole fact of its slot. ClassSignature
rejects duplicate table ids. Null collection elements are rejected.

### HandlerFact

`slot: HandlerSlot`, `request: HandlerRequest?`, `targets: List<HandlerTarget>`,
`absent: Boolean`. No separate serialized or stored result-kind union exists.

**Validation (`init`):** every non-null request belongs to the slot, on absent
facts too. Present CAST and OPERATION facts require a request; COMPARE
requires null. An absent whole slot permits null. An absent fact has no
targets, hence no result kinds; a present fact has at least one target.
Each target's result set may be empty. Null collection elements are rejected.

### HandlerTarget

`symbol: String`, `results: Set<ReturnKind>`,
`stringDispatch: StringDispatch? = null`.
Symbol and results are required, including explicit empty results. Bounds
summarize the registered owner; a permitted descendant method supplies its own
ordinary call result. A string
request requires explicit dispatch; other requests omit it or state null.
The symbol is a source-qualified C function identity. Each target retains its own results and dispatch together.

**Validation (`init` and owning fact):** symbol non-blank; every present
CAST/IS_STRING target requires a non-null dispatch; all other requests
require null. Unknown result kinds and null collection elements are rejected.

### HandlerSlot, HandlerRequest, StringDispatch

Closed enums decode case-insensitively from their lowercase names; unknown
names reject. These sets are protocol constants, never run settings.
`HandlerSlot` has CAST, COMPARE and OPERATION.

`HandlerRequest` in `HandlerRequest.kt` owns the consumed request vocabulary.
Every member has an immutable `slot: HandlerSlot`.

| Slot | Members |
|------|---------|
| CAST | IS_STRING, IS_LONG, IS_DOUBLE, _IS_BOOL, _IS_NUMBER, IS_ARRAY, IS_RESOURCE |
| OPERATION | ZEND_ADD, ZEND_SUB, ZEND_MUL, ZEND_DIV, ZEND_MOD, ZEND_POW, ZEND_SL, ZEND_SR, ZEND_BW_OR, ZEND_BW_AND, ZEND_BW_XOR, ZEND_BW_NOT, ZEND_CONCAT, ZEND_BOOL_XOR, ZEND_BOOL_NOT |

COMPARE has no request member. The producer's source mapping is specified
in [Handler Extraction](../../externs-phpstubs/docs/spec-extraction-handlers.md).
`StringDispatch` has NONE, METHOD_ONLY, METHOD_FIRST and NATIVE_FIRST;
their meaning is owned by the handler model. It is never a success flag.

## Exception / Error Types

- `IllegalArgumentException`: malformed ids, symbols, slot/request pairs,
  dispatch, collections, duplicate table ids or inconsistent table facts.
  Construction and YAML violations use the existing load-boundary contract.

## Open Gaps

- The handler types, fields and validation above are not implemented.

## Edge Cases

Deferred initializers and ordinary runtime syntax share the consumed protocol:

```php
<?php
function delayedNot($value = !new GMP(0)): void {
    var_dump($value);
}
function delayedBitwiseNot($value = ~new GMP(0)): void {
    var_dump(get_debug_type($value), (string) $value);
}
var_dump(!new GMP(0));
delayedNot();
delayedBitwiseNot();
```

```text
bool(true)
bool(true)
string(3) "GMP"
string(2) "-1"
```

Standard mixed comparison can consume ARRAY and RESOURCE requests:

```php
<?php
$object = new stdClass;
$resource = fopen('php://memory', 'r+');
var_dump($object == []);
var_dump($object <=> []);
var_dump($object == $resource);
var_dump($object <=> $resource);
fclose($resource);
```

```text
bool(false)
int(1)
bool(false)
int(1)
```
