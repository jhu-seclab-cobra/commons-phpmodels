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
 * @throws IllegalArgumentException If the declared name is blank.
 */
public sealed class NamedSubject(
    private val kind: String,
    final override val name: String,
    private val spelledPrefix: String = "",
) : ModelSubject {
    init {
        require(name.isNotBlank()) { "${kind.replaceFirstChar(Char::uppercaseChar)} subject declares a blank name" }
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
 * @throws IllegalArgumentException If either declared name is blank.
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
        public fun parse(raw: String): FunctionSubject = FunctionSubject(simpleName(KIND, raw))
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
        public fun parse(raw: String): ClassSubject = ClassSubject(simpleName(KIND, raw))
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
            val (owner, name) = memberPieces(KIND, raw)
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
            val (owner, name) = memberPieces(KIND, raw)
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
        public fun parse(raw: String): ConstantSubject = ConstantSubject(simpleName(KIND, raw))
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
            val name = raw.drop(1)
            require('$' !in name && "::" !in name) { "Variable subject declares a malformed name: '$raw'" }
            return VariableSubject(name)
        }
    }
}

// The single authority for the subject spelling grammar: a leading namespace
// slash is stripped; `$` is mandatory for property names and forbidden
// elsewhere; a member spelling has exactly one `::` with both sides non-empty.

private fun simpleName(
    kind: String,
    raw: String,
): String {
    val name = raw.removePrefix("\\")
    require("::" !in name) { "A $kind subject takes a plain name, got member spelling '$raw'" }
    require('$' !in name) { "A $kind subject name must not contain '\$': '$raw'" }
    return name
}

private fun memberPieces(
    kind: String,
    raw: String,
): Pair<String, String> {
    val (owner, member) = ownerAndMember(kind, raw)
    require('$' !in member) { "A $kind subject member must not contain '\$': '$raw'" }
    return owner to member
}

private fun propertyPieces(
    kind: String,
    raw: String,
): Pair<String, String> {
    val (owner, member) = ownerAndMember(kind, raw)
    require(member.startsWith('$')) { "A $kind subject member must be spelled '\$name', got '$raw'" }
    val name = member.drop(1)
    require('$' !in name) { "A $kind subject member declares a malformed name: '$raw'" }
    return owner to name
}

private fun ownerAndMember(
    kind: String,
    raw: String,
): Pair<String, String> {
    val spelling = raw.removePrefix("\\")
    val separator = spelling.indexOf("::")
    require(separator >= 0) { "A $kind subject must be spelled 'class::member', got '$raw'" }
    val owner = spelling.substring(0, separator)
    val member = spelling.substring(separator + 2)
    require("::" !in member) { "A $kind subject spelling has more than one '::': '$raw'" }
    require('$' !in owner) { "A $kind subject class must not contain '\$': '$raw'" }
    return owner to member
}
