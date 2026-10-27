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
     * @property framelessArities The argument counts at which php-src selects
     *   a frameless function for a call of this callable; each count is at
     *   most [MAX_FRAMELESS_ARITY]. Empty when none.
     * @property specialArities The argument counts, each 2 or more, at which
     *   php-src always compiles a call bound when compiled to one opcode
     *   reading its arguments. Empty when none.
     * @property ropeFormat The parameter whose constant value php-src compiles
     *   into a rope with the arguments after it; null when none.
     * @throws IllegalArgumentException If a count lies below the number of
     *   required parameters, or above the parameter count of a signature
     *   without a variadic parameter; a frameless count exceeds
     *   [MAX_FRAMELESS_ARITY]; a special count is below 2; or the rope format
     *   names no declared parameter, or one passed by reference or variadic.
     */
    public data class CallableSignature(
        val params: List<ParameterInfo> = emptyList(),
        val returnType: DeclaredType,
        val framelessArities: Set<Int> = emptySet(),
        val specialArities: Set<Int> = emptySet(),
        val ropeFormat: String? = null,
    ) : SignatureInfo {
        /** The most arguments a call may pass: the parameter count, unbounded with a variadic last parameter. */
        val maxArguments: Int
            get() = if (params.lastOrNull()?.variadic == true) Int.MAX_VALUE else params.size

        /** True when the signature states any one-opcode fact. */
        val declaresOpcodeFacts: Boolean
            get() = framelessArities.isNotEmpty() || specialArities.isNotEmpty() || ropeFormat != null

        init {
            val required = params.count { !it.optional }
            val outside = (framelessArities + specialArities).firstOrNull { it !in required..maxArguments }
            require(outside == null) {
                "Arity $outside lies outside the $required..$maxArguments arguments the signature admits"
            }
            val tooMany = framelessArities.firstOrNull { it > MAX_FRAMELESS_ARITY }
            require(tooMany == null) { "Frameless arity $tooMany exceeds $MAX_FRAMELESS_ARITY" }
            val tooFew = specialArities.firstOrNull { it < MIN_SPECIAL_ARITY }
            require(tooFew == null) { "Special arity $tooFew is below $MIN_SPECIAL_ARITY" }
            if (ropeFormat != null) {
                val format = params.firstOrNull { it.name == ropeFormat }
                require(format != null && !format.byRef && !format.variadic) {
                    "Rope format $ropeFormat names no plain declared parameter"
                }
            }
        }

        private companion object {
            /** php-src `find_frameless_function_info` rejects a call of more arguments. */
            const val MAX_FRAMELESS_ARITY = 3

            /** A one-operand compile has no operand order. */
            const val MIN_SPECIAL_ARITY = 2
        }
    }

    /**
     * A class-like declaration: its classifier and inheritance edges
     * (class subjects).
     *
     * @property classifier The declaration kind.
     * @property parent Case-folded parent class name, or null when none.
     * @property interfaces Case-folded implemented interface names.
     * @property isFinal The registered declaration's FINAL flag, local to this
     *   declaration, independent of the classifier and never inherited; null
     *   when unestablished, which proves neither value.
     * @property handlers The effective object-handler table alternatives of a
     *   builtin class; empty when none is established.
     * @throws IllegalArgumentException If the parent or an interface name is
     *   blank, a table is null, or two tables share an id.
     */
    @ConsistentCopyVisibility
    public data class ClassSignature private constructor(
        val classifier: Classifier,
        val parent: String?,
        val interfaces: List<String>,
        val isFinal: Boolean?,
        val handlers: List<HandlerTable>,
    ) : SignatureInfo {
        init {
            require(parent == null || parent.isNotBlank()) { "Class signature declares a blank parent" }
            require(interfaces.none { it.isBlank() }) { "Class signature declares a blank interface" }
            require(handlers.none { (it as HandlerTable?) == null }) { "Class signature holds a null handler table" }
            val repeated = handlers.groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }
            require(repeated == null) { "Class signature repeats handler table id ${repeated?.key}" }
        }

        public companion object {
            /** Folds the inheritance edges into the stored, compared form. */
            @JvmStatic
            @JsonCreator
            public operator fun invoke(
                classifier: Classifier,
                parent: String? = null,
                interfaces: List<String> = emptyList(),
                isFinal: Boolean? = null,
                handlers: List<HandlerTable> = emptyList(),
            ): ClassSignature =
                ClassSignature(classifier, parent?.lowercase(), interfaces.map(String::lowercase), isFinal, handlers)
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
