package com.rapture.gradle.bootstrap.plugin

import org.ajoberstar.gradle.git.tasks.GitTag
import org.gradle.api.Project

/**
 * Handles tagging and versioning for Rapture gradle projects
 */
class RaptureVersioning {

    public static final String DEPENDENCIES_FILE = 'dependencies.properties' 
    public static final String TIMESTAMP_FILE = 'timestamp.txt' 

    void apply(Project project) {
        setupTimestamp(project)
        setupTasks(project)

        String version
        if (project.hasProperty('version')) {
            if (project.version == null || project.version.equals(Project.DEFAULT_VERSION)) {
                version = modifyVersion(project, project.name)
            }
            else {
                version = adjustVersion(project)
            }
        }

        // projects with wildcard dependencies use the timestamp for their tags
        // this allows the various subprojects to each have their own versions
        if (hasWildcardDependencies(project)) {
            overrideWildcardDependencies(project)
        }

        // we only need to tag a repo once at the root level
        if (isTagEnabled(project) && !isSubproject(project)) {
            tag(project, version)
        }
    }

    /**
     * set a timestamp that will become part of the version for tagged builds.
     * We re-use the root projects original timestamp so that all timestamps are the 
     * same for a given repository.
     * If a timestamp file is provided, we use that as a first preference.  This is so that
     * all artifacts that are part of a larger build process can all share the same timestamp.
     */
    void setupTimestamp(Project project) {
           File timestamp = new File(TIMESTAMP_FILE);
           if (timestamp.exists()) {
               project.ext.timestamp = timestamp.text.trim()
               println "Using timestamp from file: [${project.ext.timestamp}]"
           }
           else if (isSubproject(project)) {
            if (project.parent.ext.hasProperty("timestamp")) {
                project.ext.timestamp = project.parent.ext.timestamp
            } else {
              project.ext.timestamp = new Date().format('yyyyMMddHHmmss')
            }
           } else {
            project.ext.timestamp = new Date().format('yyyyMMddHHmmss')
           }
    }
    
    void setupTasks(Project project) {
        project.task('tagVersion', type: GitTag) {
            tagName = "${project.version}"
            message = "Release of ${project.name} ${project.version}"
        }
        project.task('adjustVersion', type: AdjustVersionTask)
        project.task('applyPluginManifestVersion', type: ApplyPluginManifestVersionTask)
    }

    void tag(Project project, String version) {
        println "Preparing ${project.name} tag: $version"
        project.tagVersion.tagName = version
        project.tagVersion.message = "Release of ${project.name} $version"
        if (isTagRepoPathEnabled(project)) {
            println "Setting root repo path for tagging to: ${project.tagRepoPath}"
            project.tagVersion.repoPath = project.tagRepoPath
        } 
        project.tagVersion.execute()
    }

    boolean isSubproject(Project project) {
       return project.parent != null
    } 

    /**
     * Used to modify a project's version property.  May be explicitly called in later
     * stages of the configuration phase after the plugin has been applied, since at the time of
     * plugin application, usually the version hasn't been defined yet.  Subprojects are an example
     * of this.
     */
    String adjustVersion(Project project) {
        project.version = modifyVersion(project, project.version)
        return project.version
    }
 
    /**
     * non-tagged builds get the .99999999999999 version which will always be preferred
     * for ad-hoc builds when placed in a local maven repository.  Tagged builds
     * get the timestamp (which is shared for the entire repository)
     */
    String modifyVersion(Project project, String version) {
        if (isTagEnabled(project)) {
            version += ".${project.ext.timestamp}"
        }
        else {
            version += '.99999999999999'
        }
        return version
    }

    void overrideWildcardDependencies(Project project) {
        def props = new Properties()
        new File(DEPENDENCIES_FILE).withInputStream { s ->
            props.load(s) 
        }
        props.each { k, v ->
            if (project.hasProperty(k)) {
                project.setProperty(k, v)
                println "Using overriden dependency version [$k : $v]"
            }
        }
    }

    boolean isTagRepoPathEnabled(Project project) {
        return project.hasProperty('tagRepoPath')
    }

    boolean isTagEnabled(Project project) {
        return project.hasProperty('tag') && project.tag.equals('true')
    }

    boolean hasWildcardDependencies(Project project) {
        return !isSubproject(project) && new File(DEPENDENCIES_FILE).exists()
    }

}
