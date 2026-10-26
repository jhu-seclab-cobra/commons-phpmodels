# PHP Models — Effective Handler Facts

The handler facts of the class signature in a declaration entry
([model-declarations.md](model-declarations.md)). Software structure:
[design-declarations.md](design-declarations.md).

The class signature states how a builtin class's native object handlers
behave, for a consumer that must decide what a conversion or comparison of
an object of that class yields. Each fact describes one handler slot of the
class (cast, comparison, arithmetic operation) for one request: the cast
target type, or the opcode of the operation.

- A fact states the effective target of the slot, or the alternatives of a
  table that selects among targets by the request, and the kinds of the
  normal result the slot is established to produce (the classification of
  Return Classification in [model-declarations.md](model-declarations.md#return-classification)).
- A slot that is NULL in the class is stated as positively absent. That
  differs from a slot with no fact: missing coverage stays unestablished and
  is never read as the standard behavior.
- A consumer finds the class candidates of an object through its LINK_WITH
  and INSTANCE_OF relations, reads the declaration kind through DEFINED_BY,
  and reaches the facts of an ancestor through the INHERIT_FROM lookup over
  class parents only; interfaces are excluded. An enum has its own language
  rule and takes no default of an ordinary user class.
- An inherited user `__toString` decides a string conversion only when the
  native or standard dispatcher of the class uses it. SimpleXML does; tidy
  does not; COM falls back to it under a condition. A string method never
  overrides a native boolean, numeric or comparison slot.

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
