package com.rapture.gradle.bootstrap.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * This will cause Rapture projects to publish JavaDoc and source JAR files to Maven in addition to the compiled files. 
 * @author bardhi
 *
 */
class ArchivePublisher {

    void apply(Project project) {
        boolean isDisabled = project.hasProperty("raptureDisableSources") && project.reptureDisableSources;
        if (project.hasProperty("classes") && !isDisabled) {
            def sourcesJar = project.task("sourcesJar", type : Jar) {
                classifier = 'sources'
                from project.sourceSets.main.allSource
            }
            project.artifacts { archives sourcesJar }

            def javadocJar = project.task("javadocJar", type : Jar) {
                classifier = 'javadoc'
                from project.sourceSets.main.allSource
            }
            project.artifacts { archives javadocJar }
        }
    }
}
