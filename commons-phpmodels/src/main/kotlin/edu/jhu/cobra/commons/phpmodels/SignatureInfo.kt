package edu.jhu.cobra.commons.phpmodels

/**
 * The decoded signature section of a declaration entry: what the declaration
 * looks like, never what an analysis believes about it. One subtype per
 * describable subject kind; the subtype is selected at the entry level from
 * the entry's subject kind — the signature mapping carries no discriminator
 * of its own.
 */
public sealed interface SignatureInfo {
    /**
     * A callable declaration: parameter list and declared return type
     * (function and method subjects).
     *
     * @property params Declared parameters; position is list order.
     * @property returnType Declared return type; the returns classification is
     *   derived from it at load and never stored beside it.
     */
    public data class CallableSignature(
        val params: List<ParameterInfo> = emptyList(),
        val returnType: DeclaredType,
    ) : SignatureInfo

    /**
     * A class-like declaration: its classifier and inheritance edges
     * (class subjects).
     *
     * @property classifier The declaration kind.
     * @property parent Case-folded parent class name, or null when none.
     * @property interfaces Case-folded implemented interface names.
     * @throws IllegalArgumentException If the parent or an interface name is blank.
     */
    public class ClassSignature(
        public val classifier: Classifier,
        parent: String? = null,
        interfaces: List<String> = emptyList(),
    ) : SignatureInfo {
        public val parent: String? = parent?.lowercase()
        public val interfaces: List<String> = interfaces.map { it.lowercase() }

        init {
            require(this.parent == null || this.parent.isNotBlank()) { "Class signature declares a blank parent" }
            require(this.interfaces.none { it.isBlank() }) { "Class signature declares a blank interface" }
        }

        override fun equals(other: Any?): Boolean =
            other is ClassSignature &&
                other.classifier == classifier &&
                other.parent == parent &&
                other.interfaces == interfaces

        override fun hashCode(): Int = 31 * (31 * classifier.hashCode() + parent.hashCode()) + interfaces.hashCode()

        override fun toString(): String =
            "ClassSignature(classifier=$classifier, parent=$parent, interfaces=$interfaces)"
    }

    /**
     * A typed value declaration: the declared type alone (constant and
     * class-constant subjects).
     */
    public data class TypedSignature(
        val type: DeclaredType,
    ) : SignatureInfo

    /**
     * A property declaration: declared type, visibility, and staticness
     * (property subjects).
     */
    public data class PropertySignature(
        val type: DeclaredType,
        val visibility: Visibility,
        val static: Boolean = false,
    ) : SignatureInfo
}

/**
 * One declared parameter of a callable. A pure data holder; position is list
 * order.
 *
 * @property name Declared parameter name, without the leading `$`.
 * @property type Declared parameter type.
 * @property optional True when the parameter has a default value.
 * @property byRef True when the parameter is taken by reference.
 * @property variadic True when the parameter collects the variadic tail.
 */
public data class ParameterInfo(
    val name: String,
    val type: DeclaredType,
    val optional: Boolean = false,
    val byRef: Boolean = false,
    val variadic: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Parameter declares a blank name" }
    }
}

/**
 * A declared PHP type name: one of the closed keyword-type vocabulary or a
 * class name. Stored losslessly; the four-kind returns classification is
 * derived by [toReturnKind] and never stored beside the declared type.
 *
 * @property raw The type name as declared.
 * @throws IllegalArgumentException If [raw] is neither a keyword type nor a
 *   class-name spelling.
 */
@JvmInline
public value class DeclaredType(
    public val raw: String,
) {
    init {
        require(raw.lowercase() in KEYWORD_TYPES || CLASS_NAME_SPELLING.matches(raw)) {
            "Declared type is neither a keyword type nor a class name: '$raw'"
        }
    }

    /** The returns classification this declared type derives to. */
    public fun toReturnKind(): ReturnKind =
        when (raw.lowercase()) {
            "string" -> ReturnKind.STR
            "int", "float" -> ReturnKind.NUM
            "bool" -> ReturnKind.BOOL
            else -> ReturnKind.ANY
        }

    override fun toString(): String = raw

    private companion object {
        // Fixed by the PHP type system, not configuration.
        private val KEYWORD_TYPES: Set<String> =
            setOf(
                "string",
                "int",
                "float",
                "bool",
                "array",
                "object",
                "callable",
                "resource",
                "mixed",
                "void",
                "null",
                "iterable",
            )
        private val CLASS_NAME_SPELLING = Regex("""\\?[A-Za-z_][A-Za-z0-9_]*(\\[A-Za-z_][A-Za-z0-9_]*)*""")
    }
}

/** The closed set of class-like declaration kinds. */
public enum class Classifier { CLASS, INTERFACE, TRAIT, ENUM }

/** The closed set of member visibilities. */
public enum class Visibility { PUBLIC, PROTECTED, PRIVATE }
