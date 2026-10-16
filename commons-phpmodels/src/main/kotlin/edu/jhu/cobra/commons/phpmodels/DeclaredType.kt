package edu.jhu.cobra.commons.phpmodels

/**
 * A PHP type declaration as spelled: a single type, a nullable type (`?T`), a
 * union (`A|B`), an intersection of class names (`A&B`), or a union whose
 * members are single types or parenthesized intersections (`(A&B)|null`).
 * Stored losslessly; the four-kind returns classification is derived by
 * [toReturnKind] and never stored beside the declared type.
 *
 * @property raw The declaration as spelled, without whitespace.
 * @throws IllegalArgumentException If [raw] is outside the declared-type
 *   grammar or breaks a PHP composition rule.
 */
@JvmInline
public value class DeclaredType(
    public val raw: String,
) {
    init {
        parseDeclaredType(raw)
    }

    /**
     * The union read of the declaration: one set per alternative, holding the
     * single types it intersects. A keyword is lowercased; a class name is
     * folded and stripped of its leading namespace slash. `?T` reads as
     * `[{t}, {null}]`.
     */
    public val alternatives: List<Set<String>>
        get() = parseDeclaredType(raw)

    /** The returns classification: each alternative classified, joined with [ReturnKind.join]. */
    public fun toReturnKind(): ReturnKind = alternatives.map(::classifyAlternative).reduce(ReturnKind::join)

    /** True when the declaration is the `void` keyword: the callable produces no result. */
    public val isVoid: Boolean
        get() = raw.equals(VOID, ignoreCase = true)

    override fun toString(): String = raw
}

// The keyword naming the absent result type, fixed by the PHP type system.
private const val VOID = "void"

// The keyword naming the null type, the second alternative of `?T`.
private const val NULL = "null"

// The keyword admitting every value; no `?` applies to it.
private const val MIXED = "mixed"

// The separators of the declared-type grammar, fixed by the PHP grammar.
private const val NULLABLE_PREFIX = '?'
private const val UNION_SEPARATOR = '|'
private const val INTERSECTION_SEPARATOR = '&'
private const val GROUP_OPEN = '('
private const val GROUP_CLOSE = ')'
private const val NAMESPACE_SEPARATOR = "\\"

// The keyword types of the PHP type system, not configuration.
private val KEYWORD_TYPES: Set<String> =
    setOf(
        "string",
        "int",
        "float",
        "bool",
        "true",
        "false",
        "array",
        "object",
        "callable",
        "iterable",
        "resource",
        MIXED,
        VOID,
        "never",
        NULL,
    )

// The keywords PHP admits only as the whole declaration.
private val STANDALONE_KEYWORDS: Set<String> = setOf(MIXED, VOID, "never")

// The classification table of a one-type alternative; every other single type
// and every intersection classifies as ANY by that table.
private val SCALAR_RETURN_KINDS: Map<String, ReturnKind> =
    mapOf(
        "string" to ReturnKind.STR,
        "int" to ReturnKind.NUM,
        "float" to ReturnKind.NUM,
        "bool" to ReturnKind.BOOL,
        "true" to ReturnKind.BOOL,
        "false" to ReturnKind.BOOL,
    )

// A class name, optionally namespace-qualified with a leading slash.
private val CLASS_NAME_SPELLING = Regex("""\\?[A-Za-z_][A-Za-z0-9_]*(\\[A-Za-z_][A-Za-z0-9_]*)*""")

private fun classifyAlternative(alternative: Set<String>): ReturnKind =
    alternative.singleOrNull()?.let(SCALAR_RETURN_KINDS::get) ?: ReturnKind.ANY

// The grammar's parser: the alternatives of a valid declaration, or IllegalArgumentException.
private fun parseDeclaredType(raw: String): List<Set<String>> {
    require(raw.isNotEmpty() && raw.none(Char::isWhitespace)) { "Declared type is blank or holds whitespace: '$raw'" }
    val members = raw.split(UNION_SEPARATOR)
    return when {
        raw.first() == NULLABLE_PREFIX -> listOf(setOf(nullableTarget(raw, raw.drop(1))), setOf(NULL))
        members.size == 1 -> listOf(standaloneMember(raw))
        else -> union(raw, members)
    }
}

// A union of two or more members; a type that stands alone joins none.
private fun union(
    raw: String,
    members: List<String>,
): List<Set<String>> {
    val alternatives = members.map { unionMember(raw, it) }
    require(alternatives.none { it.any(STANDALONE_KEYWORDS::contains) }) {
        "Declared type unites a type that stands alone: '$raw'"
    }
    return alternatives
}

// `?T`: T is one single type other than null and the standalone keywords.
private fun nullableTarget(
    raw: String,
    target: String,
): String {
    val type = singleType(raw, target)
    require(type != NULL && type !in STANDALONE_KEYWORDS) { "Declared type makes '$type' nullable: '$raw'" }
    return type
}

// The whole declaration without `|`: a single type or a bare intersection.
private fun standaloneMember(raw: String): Set<String> =
    if (INTERSECTION_SEPARATOR in raw) intersection(raw, raw) else setOf(singleType(raw, raw))

// A union member: a single type or a parenthesized intersection.
private fun unionMember(
    raw: String,
    member: String,
): Set<String> {
    val grouped = member.length > 2 && member.first() == GROUP_OPEN && member.last() == GROUP_CLOSE
    val inner = if (grouped) member.substring(1, member.length - 1) else member
    require(grouped == (INTERSECTION_SEPARATOR in inner)) {
        "Declared type holds a union member that is neither a type nor a parenthesized intersection: '$raw'"
    }
    return if (grouped) intersection(raw, inner) else setOf(singleType(raw, inner))
}

// An intersection joins class names only.
private fun intersection(
    raw: String,
    spelled: String,
): Set<String> {
    val types = spelled.split(INTERSECTION_SEPARATOR).map { singleType(raw, it) }
    require(types.none(KEYWORD_TYPES::contains)) { "Declared type intersects a keyword type: '$raw'" }
    return types.toSet()
}

// One keyword type, unqualified, or one class name, folded.
private fun singleType(
    raw: String,
    spelled: String,
): String {
    val folded = spelled.lowercase()
    if (folded in KEYWORD_TYPES) return folded
    require(CLASS_NAME_SPELLING.matches(spelled)) {
        "Declared type holds '$spelled', neither a keyword nor a class name: '$raw'"
    }
    val name = folded.removePrefix(NAMESPACE_SEPARATOR)
    require(name !in KEYWORD_TYPES) { "Declared type qualifies the keyword type '$name': '$raw'" }
    return name
}
