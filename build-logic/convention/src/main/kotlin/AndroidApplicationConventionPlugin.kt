import com.android.build.api.dsl.ApplicationExtension
import com.salpiras.citizendocs.configureKotlinAndroid
import com.salpiras.citizendocs.libs
import com.salpiras.citizendocs.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // See AndroidLibraryConventionPlugin: built-in Kotlin, so no kotlin-android plugin.
            apply(plugin = "com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.versionInt("targetSdk")
                testOptions.animationsDisabled = true
            }
        }
    }
}
