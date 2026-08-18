package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * The closed set of subject kinds a generator can find. A generator names the
 * kind as data because it matches many subjects instead of containing one.
 * Generators find callable and variable subjects; the four declaration-only
 * kinds are matched explicitly, never by pattern.
 */
public enum class SubjectKind { FUNCTION, METHOD, VARIABLE }

/**
 * One condition a found subject must satisfy. The `constraint` discriminator
 * names the identity field the pattern applies to.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "constraint")
@JsonSubTypes(
    JsonSubTypes.Type(value = NameConstraint::class, name = "name"),
    JsonSubTypes.Type(value = ClassConstraint::class, name = "class"),
)
public sealed interface SubjectConstraint {
    /** The compiled pattern; construction fails on an invalid pattern. */
    public val regex: Regex

    /** True when the entire case-folded identity [field] matches the pattern. */
    public fun matches(field: String): Boolean = regex.matches(field)
}

/**
 * Shared body of the constraint kinds: the declared pattern, its compilation,
 * and value equality over the pattern within one concrete kind.
 *
 * @property pattern The declared pattern source.
 * @throws java.util.regex.PatternSyntaxException If [pattern] does not compile.
 */
public sealed class PatternConstraint(
    public val pattern: String,
) : SubjectConstraint {
    // Identities are case-folded at the load boundary; matching the pattern
    // case-insensitively keeps an uppercase literal from never matching.
    final override val regex: Regex = Regex(pattern, RegexOption.IGNORE_CASE)

    final override fun equals(other: Any?): Boolean =
        other is PatternConstraint && other::class == this::class && other.pattern == pattern

    final override fun hashCode(): Int = pattern.hashCode()

    final override fun toString(): String = "${this::class.simpleName}(pattern=$pattern)"
}

/** A constraint over the subject's own name. */
public class NameConstraint(
    pattern: String,
) : PatternConstraint(pattern)

/** A constraint over a method subject's owning class. */
public class ClassConstraint(
    pattern: String,
) : PatternConstraint(pattern)

/**
 * One decoded generator entry: its unique name, the subject kind to find, the
 * constraints to satisfy, and the model body to attach to every satisfying
 * subject. The only entry form with a `model:` wrapper — it separates the
 * selection fields from the assertion; the name is the identity every
 * generated declaration traces back to. Name uniqueness spans the whole load,
 * so the loading consumer checks it, not this class.
 *
 * @property name Load-unique generator name.
 * @property find The subject kind the generator applies to.
 * @property where Constraints a subject of the [find] kind must satisfy.
 * @property model The body attached to every matching subject.
 * @throws IllegalArgumentException If the name is blank, no name constraint is
 *   declared, a class constraint accompanies a non-method find, the body is
 *   empty, or a variable find declares a section besides sources.
 */
public data class ModelGenerator(
    val name: String,
    val find: SubjectKind,
    val where: List<SubjectConstraint>,
    val model: ModelBody,
) : ModelEntry {
    init {
        require(name.isNotBlank()) { "Model generator declares a blank name" }
        require(where.any { it is NameConstraint }) {
            "Model generator '$name' declares no name constraint"
        }
        require(find == SubjectKind.METHOD || where.none { it is ClassConstraint }) {
            "Model generator '$name' constrains a class but finds $find subjects"
        }
        require(!model.isEmpty) {
            "Model generator '$name' declares an empty body: a generator attaches no signature, so it asserts nothing"
        }
        require(find != SubjectKind.VARIABLE || model.declaresOnlySources) {
            "Model generator '$name' finds variables but declares a section besides sources"
        }
        require(find == SubjectKind.METHOD || !model.namesReceiverPort) {
            "Model generator '$name' names the receiver port but finds $find subjects"
        }
        require(find != SubjectKind.VARIABLE || !model.declaresExplicitSourceSite) {
            "Model generator '$name' declares an explicit source site but finds $find subjects"
        }
    }

    /** True when [subject] is of the [find] kind and satisfies every constraint. */
    public fun matches(subject: ModelSubject): Boolean = subject.kind == find && where.all { it.satisfiedBy(subject) }
}

// The four declaration-only kinds map to no findable kind: a generator never
// matches them, so their models are always explicit.
private val ModelSubject.kind: SubjectKind?
    get() =
        when (this) {
            is FunctionSubject -> SubjectKind.FUNCTION
            is MethodSubject -> SubjectKind.METHOD
            is VariableSubject -> SubjectKind.VARIABLE
            is ClassSubject, is ClassConstantSubject, is ConstantSubject, is PropertySubject -> null
        }

private fun SubjectConstraint.satisfiedBy(subject: ModelSubject): Boolean =
    when (this) {
        is NameConstraint -> matches(subject.name)
        is ClassConstraint -> subject is MethodSubject && matches(subject.owner)
    }
