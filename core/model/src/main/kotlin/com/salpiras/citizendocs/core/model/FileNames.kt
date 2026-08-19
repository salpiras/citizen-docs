package com.salpiras.citizendocs.core.model

private val UNSAFE = Regex("""[^A-Za-z0-9\-_ ]""")
private val WHITESPACE = Regex("""\s+""")

private const val MAX_STEM_LENGTH = 60
private const val FALLBACK_STEM = "document"

/**
 * Turns a user-supplied title into a filesystem-safe stem.
 *
 * The old implementation was `title.replace(" ", "_")`, which let `/`, `.` and `..` through
 * untouched — a title of `../../evil` would have escaped the documents directory. Everything
 * outside a conservative allowlist is dropped, and the result is never "", "." or "..".
 */
fun slugify(title: String): String = title
    .trim()
    .replace(UNSAFE, "")
    .replace(WHITESPACE, "_")
    .trim('_', '.')
    .take(MAX_STEM_LENGTH)
    .ifEmpty { FALLBACK_STEM }
