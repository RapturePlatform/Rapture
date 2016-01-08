package com.rapture.gradle.bootstrap.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class AdjustVersionTask extends DefaultTask {

    /**
     * This is an explicit task to adjust the version after it is defined.  Versions are
     * defined in subprojects which occur during the configuration phase in Gradle.
     * However, since the bootstrap plugin is applied in the root build.gradle in subprojects {...}, at that time
     * the version of the subproject has yet to be defined.  So we need an explicit call after the 'version' variable
     * is defined in the subproject.
     */
    @TaskAction
    void adjustVersion() {
        new RaptureVersioning().adjustVersion(project)
    }

}
