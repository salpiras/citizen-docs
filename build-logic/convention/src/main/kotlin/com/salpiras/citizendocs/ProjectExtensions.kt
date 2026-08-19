package com.salpiras.citizendocs

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Gives the convention plugins access to the same `libs` catalog the modules use. */
val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionInt(name: String): Int = findVersion(name).get().requiredVersion.toInt()
