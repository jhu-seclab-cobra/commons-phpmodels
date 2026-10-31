# PHP Models — Class Names and Class Aliases

The class name and class aliases a class signature states, one concern of the
class entry of [model-declarations.md](model-declarations.md).

## Entities

- **Class name** — the name of the class in the letter case its declaration
  spells, namespace included, without a leading namespace separator. Null when
  the declaring source states none.
- **Class alias** — a further name the engine registers for the class, stored
  case-folded and without a leading namespace separator.

## Relations

| From | To | Relation | Cardinality | Meaning |
|------|----|----------|-------------|---------|
| Class Signature | class alias | states | 1:0..N | A class signature can state further names the engine registers the class under |

## Invariants

- A class alias is folded as a class subject and differs from the entry's
  subject and from every other class alias of the same signature.
- A signature that states class aliases states the class name, and the class
  name folds to the entry's subject.
