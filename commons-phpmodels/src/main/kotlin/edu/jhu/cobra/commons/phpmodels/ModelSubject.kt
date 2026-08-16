package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * The PHP declaration a model identifies. One subtype per declaration kind,
 * holding exactly that kind's identity fields.
 *
 * The YAML form is a one-key mapping — the key names the kind, the value is
 * the PHP-native spelling string (`function: strlen`, `method: mysqli::query`).
 * The spelling encodes identity only, never kind.
 *
 * Identity is case-folded per kind, following PHP's own case rules: function,
 * class, and method names fold; constant and property names stay sensitive
 * with their owning class folded. Each subtype folds its identity fields
 * itself rather than trusting its callers, and defines equality over the
 * folded form so a subject is usable as a lookup key.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes(
    JsonSubTypes.Type(value = FunctionSubject::class, name = "function"),
    JsonSubTypes.Type(value = ClassSubject::class, name = "class"),
    JsonSubTypes.Type(value = MethodSubject::class, name = "method"),
    JsonSubTypes.Type(value = ClassConstantSubject::class, name = "class_constant"),
    JsonSubTypes.Type(value = PropertySubject::class, name = "property"),
    JsonSubTypes.Type(value = ConstantSubject::class, name = "constant"),
    JsonSubTypes.Type(value = VariableSubject::class, name = "variable"),
)
public sealed interface ModelSubject {
    /** The subject's own declared name, folded or sensitive per its kind. */
    public val name: String
}

/**
 * Shared identity of the kinds a name alone identifies: the blank check,
 * value equality within one concrete kind, the name-based hash, and the
 * `kind name` spelling. Subtypes fold [name] before delegating here when
 * their kind is case-insensitive.
 *
 * @property name The declared name, exactly as the subtype passed it.
 * @throws IllegalArgumentException If the declared name is blank or carries a
 *   spelling-grammar character.
 */
public sealed class NamedSubject(
    private val kind: String,
    final override val name: String,
    private val spelledPrefix: String = "",
) : ModelSubject {
    init {
        val label = kind.replaceFirstChar(Char::uppercaseChar)
        require(name.isNotBlank()) { "$label subject declares a blank name" }
        requirePlainIdentity(label, "name", name)
    }

    final override fun equals(other: Any?): Boolean =
        other is NamedSubject && other::class == this::class && other.name == name

    final override fun hashCode(): Int = name.hashCode()

    final override fun toString(): String = "$kind $spelledPrefix$name"
}

/**
 * Shared identity of the kinds an owning class plus a member name identify:
 * the folded owner, the blank checks, value equality within one concrete
 * kind, the owner-and-name hash, and the `kind owner::name` spelling.
 * Subtypes fold [name] before delegating here when their kind is
 * case-insensitive.
 *
 * @property owner Case-folded name of the owning class.
 * @property name The declared member name, exactly as the subtype passed it.
 * @throws IllegalArgumentException If either declared name is blank or
 *   carries a spelling-grammar character.
 */
public sealed class MemberSubject(
    private val kind: String,
    owner: String,
    final override val name: String,
    private val spelledPrefix: String = "",
) : ModelSubject {
    public val owner: String = owner.lowercase()

    init {
        val label = kind.replaceFirstChar(Char::uppercaseChar)
        require(this.owner.isNotBlank()) { "$label subject declares a blank class" }
        require(name.isNotBlank()) { "$label subject '${this.owner}' declares a blank name" }
        requirePlainIdentity(label, "class", this.owner)
        requirePlainIdentity(label, "name", name)
    }

    final override fun equals(other: Any?): Boolean =
        other is MemberSubject && other::class == this::class && other.owner == owner && other.name == name

    final override fun hashCode(): Int = 31 * owner.hashCode() + name.hashCode()

    final override fun toString(): String = "$kind $owner::$spelledPrefix$name"
}

/** A function declaration, identified by its case-folded name alone. */
public class FunctionSubject(
    name: String,
) : NamedSubject(KIND, name.lowercase()) {
    public companion object {
        private const val KIND = "function"

        /** @throws IllegalArgumentException If [raw] is not a plain function name. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): FunctionSubject = FunctionSubject(simpleName(raw))
    }
}

/**
 * A class-like declaration (class, interface, trait, or enum), identified by
 * its case-folded name alone.
 */
public class ClassSubject(
    name: String,
) : NamedSubject(KIND, name.lowercase()) {
    public companion object {
        private const val KIND = "class"

        /** @throws IllegalArgumentException If [raw] is not a plain class name. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): ClassSubject = ClassSubject(simpleName(raw))
    }
}

/**
 * A method declaration, identified by its owning class together with its own
 * name, both case-folded; spelled `class::name`.
 */
public class MethodSubject(
    owner: String,
    name: String,
) : MemberSubject(KIND, owner, name.lowercase()) {
    public companion object {
        private const val KIND = "method"

        /** @throws IllegalArgumentException If [raw] is not a `class::name` spelling. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): MethodSubject {
            val (owner, name) = ownerAndMember(KIND, raw)
            return MethodSubject(owner, name)
        }
    }
}

/**
 * A class-constant declaration, identified by its case-folded owning class
 * together with its case-sensitive name; spelled `class::NAME`.
 */
public class ClassConstantSubject(
    owner: String,
    name: String,
) : MemberSubject(KIND, owner, name) {
    public companion object {
        private const val KIND = "class constant"

        /** @throws IllegalArgumentException If [raw] is not a `class::NAME` spelling. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): ClassConstantSubject {
            val (owner, name) = ownerAndMember(KIND, raw)
            return ClassConstantSubject(owner, name)
        }
    }
}

/**
 * A property declaration, identified by its case-folded owning class together
 * with its case-sensitive name; spelled `class::$name`, stored without the
 * `$`.
 */
public class PropertySubject(
    owner: String,
    name: String,
) : MemberSubject(KIND, owner, name, spelledPrefix = "$") {
    public companion object {
        private const val KIND = "property"

        /** @throws IllegalArgumentException If [raw] is not a `class::${'$'}name` spelling. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): PropertySubject {
            val (owner, name) = propertyPieces(KIND, raw)
            return PropertySubject(owner, name)
        }
    }
}

/** A global constant declaration, identified by its case-sensitive name alone. */
public class ConstantSubject(
    name: String,
) : NamedSubject(KIND, name) {
    public companion object {
        private const val KIND = "constant"

        /** @throws IllegalArgumentException If [raw] is not a plain constant name. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): ConstantSubject = ConstantSubject(simpleName(raw))
    }
}

/**
 * A language-predefined variable, such as a superglobal array, identified by
 * its case-folded name; spelled `${'$'}name`, stored without the `$`.
 */
public class VariableSubject(
    name: String,
) : NamedSubject(KIND, name.lowercase(), spelledPrefix = "$") {
    public companion object {
        private const val KIND = "variable"

        /** @throws IllegalArgumentException If [raw] is not a `${'$'}name` spelling. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): VariableSubject {
            require(raw.startsWith('$')) { "Variable subject must be spelled '\$name', got '$raw'" }
            return VariableSubject(raw.drop(1))
        }
    }
}

// The subject spelling grammar: a leading namespace slash is stripped, `$` is
// mandatory for property and variable names, and a member spelling splits at
// the first `::`. Residual grammar characters inside the split pieces are
// rejected by the base-class identity invariant, so the spelling creators and
// direct construction yield one identity.

private fun requirePlainIdentity(
    label: String,
    field: String,
    value: String,
) {
    require("::" !in value) { "$label subject $field contains '::': '$value'" }
    require('$' !in value) { "$label subject $field contains '\$': '$value'" }
    require(!value.startsWith('\\')) { "$label subject $field starts with '\\': '$value'" }
}

private fun simpleName(raw: String): String = raw.removePrefix("\\")

private fun propertyPieces(
    kind: String,
    raw: String,
): Pair<String, String> {
    val (owner, member) = ownerAndMember(kind, raw)
    require(member.startsWith('$')) { "A $kind subject member must be spelled '\$name', got '$raw'" }
    return owner to member.drop(1)
}

private fun ownerAndMember(
    kind: String,
    raw: String,
): Pair<String, String> {
    val spelling = raw.removePrefix("\\")
    val separator = spelling.indexOf("::")
    require(separator >= 0) { "A $kind subject must be spelled 'class::member', got '$raw'" }
    return spelling.substring(0, separator) to spelling.substring(separator + 2)
}
