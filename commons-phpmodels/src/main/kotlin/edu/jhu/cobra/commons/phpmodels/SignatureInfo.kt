package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

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
    @ConsistentCopyVisibility
    public data class ClassSignature private constructor(
        val classifier: Classifier,
        val parent: String?,
        val interfaces: List<String>,
    ) : SignatureInfo {
        init {
            require(parent == null || parent.isNotBlank()) { "Class signature declares a blank parent" }
            require(interfaces.none { it.isBlank() }) { "Class signature declares a blank interface" }
        }

        public companion object {
            /** Folds the inheritance edges into the stored, compared form. */
            @JvmStatic
            @JsonCreator
            public operator fun invoke(
                classifier: Classifier,
                parent: String? = null,
                interfaces: List<String> = emptyList(),
            ): ClassSignature = ClassSignature(classifier, parent?.lowercase(), interfaces.map(String::lowercase))
        }
    }

    /**
     * A typed value declaration: declared type and, when the declaring source
     * states one, the spelled literal value (constant and class-constant
     * subjects).
     */
    public data class TypedSignature(
        val type: DeclaredType,
        val value: String? = null,
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
        require(raw.lowercase() in KEYWORD_RETURN_KINDS || CLASS_NAME_SPELLING.matches(raw)) {
            "Declared type is neither a keyword type nor a class name: '$raw'"
        }
    }

    /** The returns classification this declared type derives to. */
    public fun toReturnKind(): ReturnKind =
        // A class name is not in the keyword map; it derives to ANY by design.
        KEYWORD_RETURN_KINDS[raw.lowercase()] ?: ReturnKind.ANY

    /** True when the declared type is `void`: the callable produces no result. */
    public val isVoid: Boolean
        get() = raw.equals("void", ignoreCase = true)

    override fun toString(): String = raw

    private companion object {
        // The keyword-type vocabulary is fixed by the PHP type system, not
        // configuration. Each keyword carries the returns classification it
        // derives to; non-scalar keywords derive to ANY.
        private val KEYWORD_RETURN_KINDS: Map<String, ReturnKind> =
            mapOf(
                "string" to ReturnKind.STR,
                "int" to ReturnKind.NUM,
                "float" to ReturnKind.NUM,
                "bool" to ReturnKind.BOOL,
                "array" to ReturnKind.ANY,
                "object" to ReturnKind.ANY,
                "callable" to ReturnKind.ANY,
                "resource" to ReturnKind.ANY,
                "mixed" to ReturnKind.ANY,
                "void" to ReturnKind.ANY,
                "null" to ReturnKind.ANY,
                "iterable" to ReturnKind.ANY,
            )
        private val CLASS_NAME_SPELLING = Regex("""\\?[A-Za-z_][A-Za-z0-9_]*(\\[A-Za-z_][A-Za-z0-9_]*)*""")
    }
}

/** The closed set of class-like declaration kinds. */
public enum class Classifier { CLASS, INTERFACE, TRAIT, ENUM }

/** The closed set of member visibilities. */
public enum class Visibility { PUBLIC, PROTECTED, PRIVATE }
