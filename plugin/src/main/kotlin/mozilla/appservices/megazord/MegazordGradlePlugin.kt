/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.appservices.megazord

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.net.URI

open class MegazordPlugin : Plugin<Project> {
    internal lateinit var megazordExtension: MegazordExtension

    /**
     * Add substitutions for each component in the megazord.
     */
    private fun DependencySubstitutions.substituteMegazord(megazord: MegazordDefinition, logger: Logger?) {
        this.all { dependency ->
            val requested = dependency.requested as? ModuleComponentSelector
            if (requested == null) {
                return@all
            }

            val identifier = megazord.components.find { it == requested.moduleIdentifier }
            if (identifier == null) {
                logger?.debug("substitution for '${requested.group}:${requested.module}' not found")
                return@all
            }

            val substitution = "${megazord.moduleIdentifier.group}:${megazord.moduleIdentifier.name}:${requested.version}"
            logger?.info("substituting megazord module '${substitution}' for '${requested.group}:${requested.module}:${requested.version}'")
            dependency.useTarget(substitution)
        }
    }

    companion object {
        /**
         * Find the megazord definition that best covers the given modules.
         */
        private fun findMegazordsForConfiguration(megazords: Collection<MegazordDefinition>, modules: Collection<ModuleComponentIdentifier>, logger: Logger?): List<MegazordDefinition> {
            val interesting: Set<ModuleIdentifier> = megazords.map { megazord -> megazord.components }.fold(setOf()) { u, us -> u.union(us) };

            val matching = modules.filter { interesting.contains(it.moduleIdentifier) }
            if (matching.isEmpty()) {
                return emptyList()
            }

            val versions = matching.map { dep -> dep.version }.toSortedSet()
            if (versions.size > 1) {
                throw GradleException("megazord component modules did not all have the same version: ${matching} components had versions ${versions}")
            }

            val candidates = megazords.filter { megazord -> megazord.components.containsAll(matching.map { id -> id.moduleIdentifier }) }
            logger?.info("components ${matching} match candidate megazords: ${candidates}")

            return candidates
        }
    }

    override fun apply(project: Project) {
        with(project) {
            megazordExtension = extensions.create("megazord", MegazordExtension::class.java, project)

            // This is somewhat temporary: https://github.com/mozilla/application-services isn't publishing to
            // maven.mozilla.org yet, so we need a non-standard URL.  But we probably want to force
            // https://maven.mozilla.org/maven2 in the future anyway... it's probably not worth making this
            // configurable.
            project.repositories.maven {
                it.url = URI.create("https://dl.bintray.com/ncalexander/application-services")
            }

            project.afterEvaluate {
                val logger = Logging.getLogger("megazord-gradle")

                project.configurations.all { configuration ->
                    if (!configuration.isCanBeResolved()) {
                        logger.debug("configuration ${configuration.name} cannot be resolved after project evaluation; not trying to substitute megazords")
                        return@all
                    }

                    val modules: List<ModuleComponentIdentifier> = configuration.copyRecursive().incoming.resolutionResult.allComponents.mapNotNull { it.id as? ModuleComponentIdentifier }
                    if (modules.isEmpty()) {
                        return@all
                    }

                    logger.info("configuration ${configuration.name} resolves to ${modules}; looking for megazords")

                    val candidates = findMegazordsForConfiguration(megazordExtension.megazords, modules, logger).sortedBy({ it.components.size })

                    val minimum = candidates.firstOrNull()
                    if (minimum == null) {
                        // Equivalent to candidates.isEmpty().
                        logger.debug("no megazords found for configuration ${configuration.name}; skipping")
                        return@all
                    }

                    // It's not okay to have megazords {A, B, C} and {A, B, D} for components {A, B}.
                    val minimums = candidates.filter { megazord -> megazord.components.size == minimum.components.size }
                    if (minimums.size > 1) {
                        throw GradleException("multiple minimum megazords found for configuration ${configuration.name}: ${minimums.map { it.name }}")
                    }

                    val leftOvers = minimum.components.minus(modules.map { it.moduleIdentifier })
                    if (megazordExtension.failIfMegazordIsStrictSuperset.get() && leftOvers.isNotEmpty()) {
                        throw GradleException("minimum megazord ${minimum.name} contains modules not in transitive dependencies of configuration ${configuration.name}: ${leftOvers}")
                    }

                    logger.info("megazord found for configuration ${configuration.name}: ${minimum.name}")

                    configuration.resolutionStrategy.dependencySubstitution.substituteMegazord(minimum, logger)
                }
            }
        }
    }
}
