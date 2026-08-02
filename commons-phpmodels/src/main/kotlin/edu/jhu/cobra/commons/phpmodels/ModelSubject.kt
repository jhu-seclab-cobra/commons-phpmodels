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
public sealed interface ModelSubject

/**
 * A function declaration, identified by its name alone.
 *
 * @property name Case-folded function name.
 * @throws IllegalArgumentException If the declared name is blank.
 */
public class FunctionSubject(
    name: String,
) : ModelSubject {
    public val name: String = name.lowercase()

    init {
        require(this.name.isNotBlank()) { "Function subject declares a blank name" }
    }

    override fun equals(other: Any?): Boolean = other is FunctionSubject && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "function $name"

    public companion object {
        /** @throws IllegalArgumentException If [raw] is not a plain function name. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): FunctionSubject = FunctionSubject(simpleName("function", raw))
    }
}

/**
 * A class-like declaration (class, interface, trait, or enum), identified by
 * its name alone.
 *
 * @property name Case-folded class name.
 * @throws IllegalArgumentException If the declared name is blank.
 */
public class ClassSubject(
    name: String,
) : ModelSubject {
    public val name: String = name.lowercase()

    init {
        require(this.name.isNotBlank()) { "Class subject declares a blank name" }
    }

    override fun equals(other: Any?): Boolean = other is ClassSubject && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "class $name"

    public companion object {
        /** @throws IllegalArgumentException If [raw] is not a plain class name. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): ClassSubject = ClassSubject(simpleName("class", raw))
    }
}

/**
 * A method declaration, identified by its owning class together with its own
 * name; spelled `class::name`.
 *
 * @property owner Case-folded name of the owning class.
 * @property name Case-folded method name.
 * @throws IllegalArgumentException If either declared name is blank.
 */
public class MethodSubject(
    owner: String,
    name: String,
) : ModelSubject {
    public val owner: String = owner.lowercase()
    public val name: String = name.lowercase()

    init {
        require(this.owner.isNotBlank()) { "Method subject declares a blank class" }
        require(this.name.isNotBlank()) { "Method subject '${this.owner}' declares a blank name" }
    }

    override fun equals(other: Any?): Boolean = other is MethodSubject && other.owner == owner && other.name == name

    override fun hashCode(): Int = 31 * owner.hashCode() + name.hashCode()

    override fun toString(): String = "method $owner::$name"

    public companion object {
        /** @throws IllegalArgumentException If [raw] is not a `class::name` spelling. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): MethodSubject {
            val (owner, name) = memberPieces("method", raw)
            return MethodSubject(owner, name)
        }
    }
}

/**
 * A class-constant declaration, identified by its owning class together with
 * its own name; spelled `class::NAME`.
 *
 * @property owner Case-folded name of the owning class.
 * @property name Case-sensitive constant name.
 * @throws IllegalArgumentException If either declared name is blank.
 */
public class ClassConstantSubject(
    owner: String,
    name: String,
) : ModelSubject {
    public val owner: String = owner.lowercase()
    public val name: String = name

    init {
        require(this.owner.isNotBlank()) { "Class-constant subject declares a blank class" }
        require(this.name.isNotBlank()) { "Class-constant subject '${this.owner}' declares a blank name" }
    }

    override fun equals(other: Any?): Boolean =
        other is ClassConstantSubject && other.owner == owner && other.name == name

    override fun hashCode(): Int = 31 * owner.hashCode() + name.hashCode()

    override fun toString(): String = "class constant $owner::$name"

    public companion object {
        /** @throws IllegalArgumentException If [raw] is not a `class::NAME` spelling. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): ClassConstantSubject {
            val (owner, name) = memberPieces("class constant", raw)
            return ClassConstantSubject(owner, name)
        }
    }
}

/**
 * A property declaration, identified by its owning class together with its own
 * name; spelled `class::$name`, stored without the `$`.
 *
 * @property owner Case-folded name of the owning class.
 * @property name Case-sensitive property name, without the leading `$`.
 * @throws IllegalArgumentException If either declared name is blank.
 */
public class PropertySubject(
    owner: String,
    name: String,
) : ModelSubject {
    public val owner: String = owner.lowercase()
    public val name: String = name

    init {
        require(this.owner.isNotBlank()) { "Property subject declares a blank class" }
        require(this.name.isNotBlank()) { "Property subject '${this.owner}' declares a blank name" }
    }

    override fun equals(other: Any?): Boolean = other is PropertySubject && other.owner == owner && other.name == name

    override fun hashCode(): Int = 31 * owner.hashCode() + name.hashCode()

    override fun toString(): String = "property $owner::\$$name"

    public companion object {
        /** @throws IllegalArgumentException If [raw] is not a `class::${'$'}name` spelling. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): PropertySubject {
            val (owner, dollarName) = memberPieces("property", raw, dollarName = true)
            return PropertySubject(owner, dollarName)
        }
    }
}

/**
 * A global constant declaration, identified by its name alone.
 *
 * @property name Case-sensitive constant name.
 * @throws IllegalArgumentException If the declared name is blank.
 */
public class ConstantSubject(
    name: String,
) : ModelSubject {
    public val name: String = name

    init {
        require(this.name.isNotBlank()) { "Constant subject declares a blank name" }
    }

    override fun equals(other: Any?): Boolean = other is ConstantSubject && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "constant $name"

    public companion object {
        /** @throws IllegalArgumentException If [raw] is not a plain constant name. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): ConstantSubject = ConstantSubject(simpleName("constant", raw))
    }
}

/**
 * A language-predefined variable, such as a superglobal array; spelled
 * `${'$'}name`, stored without the `$`.
 *
 * @property name Case-folded variable name, without the leading `$`.
 * @throws IllegalArgumentException If the declared name is blank.
 */
public class VariableSubject(
    name: String,
) : ModelSubject {
    public val name: String = name.lowercase()

    init {
        require(this.name.isNotBlank()) { "Variable subject declares a blank name" }
    }

    override fun equals(other: Any?): Boolean = other is VariableSubject && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "variable \$$name"

    public companion object {
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
    dollarName: Boolean = false,
): Pair<String, String> {
    val spelling = raw.removePrefix("\\")
    val separator = spelling.indexOf("::")
    require(separator >= 0) { "A $kind subject must be spelled 'class::member', got '$raw'" }
    val owner = spelling.substring(0, separator)
    val member = spelling.substring(separator + 2)
    require("::" !in member) { "A $kind subject spelling has more than one '::': '$raw'" }
    require('$' !in owner) { "A $kind subject class must not contain '\$': '$raw'" }
    val name =
        if (dollarName) {
            require(member.startsWith('$')) { "A $kind subject member must be spelled '\$name', got '$raw'" }
            member.drop(1)
        } else {
            require('$' !in member) { "A $kind subject member must not contain '\$': '$raw'" }
            member
        }
    require('$' !in name) { "A $kind subject member declares a malformed name: '$raw'" }
    return owner to name
}
