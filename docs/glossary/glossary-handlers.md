# Glossary — Object Handlers

## Handler Coverage

### `HandlerTable`
- **Definition:** One possible effective object-handler table of a class, carrying the established facts for its slots.
- **Example:** a class with two possible native tables → two `HandlerTable` alternatives.
- **Source:** `HandlerFact.kt` `HandlerTable`

### `HandlerFact`
- **Definition:** The established absence or possible targets of one slot and request in a `HandlerTable`.
- **Example:** an absent arithmetic slot → a `HandlerFact` with no targets.
- **Source:** `HandlerFact.kt` `HandlerFact`

### `HandlerTarget`
- **Definition:** One source-qualified native function target of an established slot/request fact, with its normal-result classifications and string-method invocation order.
- **Example:** a string cast target → its source identity, result classifications and dispatch kept together.
- **Source:** `HandlerFact.kt` `HandlerTarget`

### `HandlerSlot`
- **Definition:** The category of object operation queried in a `HandlerFact`.
- **Example:** `CAST` → the conversion slot.
- **Source:** `HandlerFact.kt` `HandlerSlot`

| Member | Definition | Reference | Literature | Tools |
|--------|------------|-----------|------------|-------|
| `CAST` | The slot performs a conversion. | none | none | none |
| `COMPARE` | The slot compares objects or an object and another value. | none | none | none |
| `OPERATION` | The slot performs an arithmetic, bitwise or boolean operation. | none | none | none |

### `HandlerRequest`
- **Definition:** The requested conversion type or opcode associated with one object-operation slot.
- **Example:** `IS_STRING` → a CAST request for string conversion.
- **Source:** `HandlerRequest.kt` `HandlerRequest`

| Member | Definition | Reference | Literature | Tools |
|--------|------------|-----------|------------|-------|
| `IS_STRING` | The request selects string conversion. | none | none | none |
| `IS_LONG` | The request selects integer conversion. | none | none | none |
| `IS_DOUBLE` | The request selects floating-point conversion. | none | none | none |
| `_IS_BOOL` | The request selects boolean conversion. | none | none | none |
| `_IS_NUMBER` | The request selects numeric conversion. | none | none | none |
| `IS_ARRAY` | The request selects array conversion. | none | none | none |
| `IS_RESOURCE` | The request selects resource conversion. | none | none | none |
| `ZEND_ADD` | The request selects addition. | none | none | none |
| `ZEND_SUB` | The request selects subtraction. | none | none | none |
| `ZEND_MUL` | The request selects multiplication. | none | none | none |
| `ZEND_DIV` | The request selects division. | none | none | none |
| `ZEND_MOD` | The request selects remainder. | none | none | none |
| `ZEND_POW` | The request selects exponentiation. | none | none | none |
| `ZEND_SL` | The request selects left shift. | none | none | none |
| `ZEND_SR` | The request selects right shift. | none | none | none |
| `ZEND_BW_OR` | The request selects bitwise OR. | none | none | none |
| `ZEND_BW_AND` | The request selects bitwise AND. | none | none | none |
| `ZEND_BW_XOR` | The request selects bitwise XOR. | none | none | none |
| `ZEND_BW_NOT` | The request selects bitwise NOT. | none | none | none |
| `ZEND_CONCAT` | The request selects string concatenation. | none | none | none |
| `ZEND_BOOL_XOR` | The request selects boolean XOR. | none | none | none |
| `ZEND_BOOL_NOT` | The request selects boolean NOT. | none | none | none |

### `StringDispatch`
- **Definition:** The order in which native conversion and the effective string method may provide a normal string result.
- **Example:** `METHOD_ONLY` → invocation of the effective string method.
- **Source:** `HandlerFact.kt` `StringDispatch`

| Member | Definition | Reference | Literature | Tools |
|--------|------------|-----------|------------|-------|
| `NONE` | No effective string method is invoked. | none | none | none |
| `METHOD_ONLY` | Only the effective string method is invoked. | none | none | none |
| `METHOD_FIRST` | The effective string method precedes native conversion. | none | none | none |
| `NATIVE_FIRST` | Native conversion precedes the effective string method. | none | none | none |
