package com.rapture.gradle.bootstrap.plugin

import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Used to set the version on a plugin manifest file plugin.txt
 */
class ApplyPluginManifestVersionTask extends DefaultTask {

    /**
     * Task to set the version on a plugin's manifest file 'plugin.txt'.  If
     * the version attribute is already defined, it will replace it's value.  Else,
     * it will add a new version attribute to the end of the json document.
     */
    @TaskAction
    void applyPluginManifestVersion() {
        println "Applying version to plugin manifests: ${project.version}"
        def filePattern = ~/plugin\.txt/
        def pluginTextClosure = {
            if (filePattern.matcher(it.name).find()) {
                println "Processing: ${it.parent}/${it.name}"
                def pluginJson = new JsonSlurper().parseText(it.text) 
                def (major, minor, release, timestamp) = project.version.tokenize('.')
                pluginJson['version'] = ['major':major.toInteger(), 'minor':minor.toInteger(), 'release':release.toInteger(), 'timestamp':timestamp.toLong()]
                def contents = new JsonBuilder(pluginJson).toString()
                File pdir = new File("${project.buildDir}/${it.parent}");
                pdir.mkdirs()
                File feat = new File(pdir, "plugin.txt")
                feat.createNewFile()
                feat.write(contents)
            }
        }
        new File('.').listFiles().each() { file ->
            if (file.isDirectory()) {
                file.eachFile(pluginTextClosure)
            }
        }
    }
}
