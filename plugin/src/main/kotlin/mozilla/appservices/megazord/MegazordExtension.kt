/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.appservices.megazord

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier

// `MegazordExtension` is documented in README.md.
open class MegazordExtension(project: Project) {
    val megazords: NamedDomainObjectContainer<MegazordDefinition> = project.container(MegazordDefinition::class.java)

    fun megazords(configureClosure: Closure<*>): NamedDomainObjectContainer<MegazordDefinition> {
        return megazords.configure(configureClosure)
    }

    fun setMozillaMegazords() {
        megazords.clear()

        megazords.add(MegazordDefinition("lockbox",
                DefaultModuleIdentifier.newId("org.mozilla.appservices.composites", "lockbox"),
                setOf(
                        DefaultModuleIdentifier.newId("org.mozilla.fxaclient", "fxaclient"),
                        DefaultModuleIdentifier.newId("org.mozilla.sync15", "logins")
                )))
        megazords.add(MegazordDefinition("reference-browser",
                DefaultModuleIdentifier.newId("org.mozilla.appservices.composites", "reference-browser"),
                setOf(
                        DefaultModuleIdentifier.newId("org.mozilla.fxaclient", "fxaclient"),
                        DefaultModuleIdentifier.newId("org.mozilla.sync15", "logins"),
                        DefaultModuleIdentifier.newId("org.mozilla.places", "places")
                )))
    }

    // See https://stackoverflow.com/a/51911444.
    // https://discuss.gradle.org/t/gradle-plugin-with-property-boolean-in-kotlin-does-not-work/25844/4.
    val failIfMegazordIsStrictSuperset = project.objects.property(Boolean::class.javaObjectType)

    init {
        failIfMegazordIsStrictSuperset.set(true)
        setMozillaMegazords()
    }
}
