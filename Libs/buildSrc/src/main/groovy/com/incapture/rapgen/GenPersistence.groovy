package com.incapture.rapgen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenPersistence extends DefaultTask {

    def mainApiFile

    @OutputDirectory
    File destFilesDir

    public GenPersistence() {
        inputs.dir project.sourceSets.main.runtimeClasspath
    }

    @TaskAction
    void genPersistence() {

        if (mainApiFile == null) {
            throw new RuntimeException("mainApiFile must be defined")
        }

        def destFilesPath = destFilesDir.absolutePath;


        def execOptions = {
            description "Generate persistence library for Storables"
            main = "com.incapture.rapgen.persistence.GenPersistence"
            classpath = project.sourceSets.main.runtimeClasspath

            args = [
                    '-mainApiFile',
                    mainApiFile,
                    '-l',
                    'Java',
                    '-o',
                    "$destFilesPath",
                    '-g',
                    getGenType()
            ]
        }
        project.javaexec(execOptions)
    }

    String getGenType() {
        return 'API';
    }
}
