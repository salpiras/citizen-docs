import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.salpiras.citizendocs.buildlogic"

// The build-logic plugins target the JDK used to run Gradle, not the one used on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id =
                libs.plugins.citizendocs.android.application
                    .asProvider()
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id =
                libs.plugins.citizendocs.android.application.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id =
                libs.plugins.citizendocs.android.library
                    .asProvider()
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id =
                libs.plugins.citizendocs.android.library.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id =
                libs.plugins.citizendocs.android.feature
                    .get()
                    .pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id =
                libs.plugins.citizendocs.android.room
                    .get()
                    .pluginId
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("androidScreenshot") {
            id =
                libs.plugins.citizendocs.android.screenshot
                    .get()
                    .pluginId
            implementationClass = "AndroidScreenshotConventionPlugin"
        }
        register("hilt") {
            id =
                libs.plugins.citizendocs.hilt
                    .get()
                    .pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id =
                libs.plugins.citizendocs.jvm.library
                    .get()
                    .pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
