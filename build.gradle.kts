// Top-level build file. Module configuration lives in the convention plugins under
// build-logic/, so individual modules only declare what is genuinely specific to them.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

// Stated here rather than relying on .editorconfig discovery, which Spotless's ktlint step
// does not reliably pick up. .editorconfig still exists so the IDE agrees with the build.
val ktlintRules = mapOf(
    "indent_size" to "4",
    "max_line_length" to "120",
    "ij_kotlin_allow_trailing_comma" to "true",
    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    // Composables are PascalCase and test names are backticked sentences.
    "ktlint_standard_function-naming" to "disabled",
    "ktlint_standard_property-naming" to "disabled",
)

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**/*.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
}
