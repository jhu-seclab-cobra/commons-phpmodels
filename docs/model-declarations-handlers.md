# PHP Models — Effective Handler Facts

The handler facts of the class signature in a declaration entry
([model-declarations.md](model-declarations.md)). Software structure:
[design-declarations-handlers.md](design-declarations-handlers.md).

The class signature states how a builtin class's native object handlers
behave, for a consumer that must decide what a conversion or comparison of
an object of that class yields. The signature groups facts by effective
object-handler table alternative. Each fact describes one slot of that
table (cast, comparison, arithmetic operation) for one request: the cast
target type, or the opcode of the operation.

- Each target alternative keeps its source identity, established normal-result
  kinds and string dispatch together. Result kinds use
  [Return Classification](model-declarations.md#return-classification).
  Every possible table selection is retained within the signature; no
  separate runtime table or selected-state property exists.
- A slot that is NULL in one effective table is stated as positively absent.
  A present target with no normal return for a request has an empty set of
  normal-result kinds. Missing table or request coverage stays unestablished
  and is never read as standard behavior. An absent whole slot applies to
  every request of that slot; presence in a different table is independent.
- An entirely unestablished class states no table coverage. A reachable
  unresolved table remains an alternative with no established facts. Missing
  coverage beside a known sibling cannot be discarded. A consumer without
  native-state evaluation retains every table and applies the requested
  dispatch and its PHP failure continuation before joining outcomes.
- String dispatch is one of: no effective-method call; only that method;
  attempt the method before native conversion; or native conversion before
  the method. A later stage supplies a normal result only after an earlier
  failure with no pending exception; this does not assert that C skips later
  instructions while an exception is pending. Native-first fallback is controlled by
  conversion failure, not failure of an earlier native invocation.
  The ordinary effective-method call supplies its result and effects;
  an ancestor's native bounds never replace a selected user method result.
- Normal-result kinds bound possible normal values, without asserting a
  literal value, success or absence of exceptions. The kinds of a native
  or default path do not independently establish that the path is reached.
  Unknown native success retains feasible native results and method calls.
  Empty native results do not erase a language-level failure continuation.
- A consumer finds the class candidates of an object through its LINK_WITH
  and INSTANCE_OF relations, reads the declaration kind through DEFINED_BY,
  and reaches the facts of an ancestor through the INHERIT_FROM lookup over
  class parents only; interfaces are excluded. An enum has its own language
  rule and takes no default of an ordinary user class.
- An inherited user `__toString` decides a string conversion only when the
  native or standard dispatcher of the class uses it. SimpleXML does; tidy
  does not; COM falls back to it under a condition. A string method never
  overrides a native boolean, numeric or comparison slot.

## Edge Cases

```php
<?php
class XMLParent extends SimpleXMLElement {
    public function __toString(): string { return 'override'; }
}
class XMLGrandchild extends XMLParent {}
$object = new XMLGrandchild('<r/>');
var_dump((string) $object);
var_dump((bool) $object);
```

```text
string(8) "override"
bool(false)
```

The string cast of the grandchild uses the inherited user method; its
boolean cast keeps the native SimpleXML slot.

```php
<?php
class TidyText extends tidy {
    public function __toString(): string { return 'USER_OVERRIDE'; }
}
$object = new TidyText;
$object->parseString('<p>native</p>');
var_dump(str_contains((string) $object, 'USER_OVERRIDE'));
var_dump(str_contains((string) $object, 'native'));
```

```text
bool(false)
bool(true)
```

The string cast of a tidy object is the native string, whatever the user
class declares.

An object of `stdClass` has a present string cast with no normal result, a
boolean cast that yields true, and a NULL arithmetic slot:

```php
<?php
try {
    var_dump((string) new stdClass);
} catch (Error $error) {
    echo get_class($error), ': ', $error->getMessage(), "\n";
}
var_dump((bool) new stdClass);
try {
    var_dump((new stdClass) + 1);
} catch (TypeError $error) {
    echo get_class($error), ': ', $error->getMessage(), "\n";
}
```

```text
Error: Object of class stdClass could not be converted to string
bool(true)
TypeError: Unsupported operand types: stdClass + int
```

A standard-dispatched descendant method is retained despite its anchor's empty string bound:

```php
<?php
class NativeBaseChild extends stdClass {
    public function __toString(): string { return 'child'; }
}
final class FinalNativeGrandchild extends NativeBaseChild {}
$object = new FinalNativeGrandchild;
var_dump((string) $object, (bool) $object);
try {
    var_dump((string) new stdClass);
} catch (Throwable $error) {
    echo get_class($error), ': ', $error->getMessage(), "\n";
}
```

```text
string(5) "child"
bool(true)
Error: Object of class stdClass could not be converted to string
```

A throwing method supplies no normal native recovery; tidy casting bypasses it:

```php
<?php
class ThrowingXml extends SimpleXMLElement {
    public function __toString(): string {
        echo "xml-method\n";
        throw new RuntimeException('xml-stop');
    }
}
class ThrowingTidy extends tidy {
    public function __toString(): string {
        echo "tidy-method\n";
        throw new RuntimeException('tidy-stop');
    }
}
try {
    var_dump((string) new ThrowingXml('<r>native</r>'));
} catch (RuntimeException $error) {
    echo $error->getMessage(), "\n";
}
$document = new ThrowingTidy;
$document->parseString('<p>native</p>');
var_dump(str_contains((string) $document, 'native'));
try {
    $document->__toString();
} catch (RuntimeException $error) {
    echo $error->getMessage(), "\n";
}
```

```text
xml-method
xml-stop
bool(true)
tidy-method
tidy-stop
```

The same native class can have both present and NULL operation alternatives:

```php
<?php
$ffi = FFI::cdef('typedef int I;');
$scalar = $ffi->new('int');
$pointer = FFI::addr($scalar);
try {
    var_dump($scalar + 1);
} catch (TypeError $error) {
    echo get_class($error), ': ', $error->getMessage(), "\n";
}
var_dump(get_debug_type($pointer + 1));
```

```text
TypeError: Unsupported operand types: FFI\CData + int
string(9) "FFI\CData"
```
