package mozilla.appservices.megazord

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
open class MegazordPlugin : Plugin<Project> {
    internal lateinit var megazordExtension: MegazordExtension

    override fun apply(project: Project) {
        with(project) {
            megazordExtension = extensions.create("megazord", MegazordExtension::class.java)
        }
    }
}
