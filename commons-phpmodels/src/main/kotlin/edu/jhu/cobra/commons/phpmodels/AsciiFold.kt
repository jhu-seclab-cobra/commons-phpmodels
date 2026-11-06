package edu.jhu.cobra.commons.phpmodels

/**
 * ASCII case folding, matching PHP's name folding (`zend_str_tolower`).
 *
 * PHP folds class, function, method and constant-keyword names byte-wise over `A`-`Z` and leaves every byte at or
 * above `0x80` alone, so names differing only in non-ASCII case stay distinct. Folding per character is equivalent: a
 * non-ASCII code point has no UTF-16 unit in `A`..`Z`. `lowercase()` would apply Unicode mappings PHP does not.
 */
internal fun String.asciiFold(): String =
    if (none { it in 'A'..'Z' }) this
    else
        buildString(length) {
            for (char in this@asciiFold) append(if (char in 'A'..'Z') char + ('a' - 'A') else char)
        }
