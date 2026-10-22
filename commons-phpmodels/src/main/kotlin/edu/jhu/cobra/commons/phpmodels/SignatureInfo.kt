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
     * @property opcodeArities The argument counts at which the engine compiles
     *   a call of this callable to one opcode whose operands are the
     *   arguments; empty when no count does. An engine fact read off the
     *   upstream compiler, stored with the declaration it describes.
     * @throws IllegalArgumentException If a count lies below the number of
     *   required parameters, or above the parameter count of a signature
     *   without a variadic parameter.
     */
    public data class CallableSignature(
        val params: List<ParameterInfo> = emptyList(),
        val returnType: DeclaredType,
        val opcodeArities: Set<Int> = emptySet(),
    ) : SignatureInfo {
        /** The most arguments a call may pass: the parameter count, unbounded with a variadic last parameter. */
        val maxArguments: Int
            get() = if (params.lastOrNull()?.variadic == true) Int.MAX_VALUE else params.size

        init {
            val required = params.count { !it.optional }
            val outside = opcodeArities.firstOrNull { it !in required..maxArguments }
            require(outside == null) {
                "Opcode arity $outside lies outside the $required..$maxArguments arguments the signature admits"
            }
        }
    }

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

/** The closed set of class-like declaration kinds. */
public enum class Classifier { CLASS, INTERFACE, TRAIT, ENUM }

/** The closed set of member visibilities. */
public enum class Visibility { PUBLIC, PROTECTED, PRIVATE }
