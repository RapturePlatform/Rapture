package com.incapture.rapgen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenApi extends DefaultTask {

    String mainApiFile
    String language

    def mainClassName = "com.incapture.rapgen.GenApi"

    @Optional @InputDirectory
    File codeSamplesJava

    @Optional @OutputDirectory
    File kernelDestDir
    @OutputDirectory
    File apiDestDir

    public GenApi() {
        inputs.dir project.sourceSets.main.runtimeClasspath
        inputs.dir project.file("src/main/resources")
    }

    @TaskAction
    void genApi() {
        if (mainApiFile == null) {
            throw new RuntimeException("mainApiFile must be defined")
        }
        def kernelDestPath;
        def apiDestPath;
        if (kernelDestDir != null) {
            kernelDestPath = kernelDestDir.absolutePath;
            apiDestPath = apiDestDir.absolutePath;
        } else {
            kernelDestPath = new File(apiDestDir, language).absolutePath;
            apiDestPath = new File(apiDestDir, language).absolutePath;
        }

        println(String.format("kernelDestPath=%s, apiDestPath=%s", kernelDestPath, apiDestPath))

        def javaArgs = [
                '-mainApiFile',
                mainApiFile,
                '-l',
                language,
                '-o',
                kernelDestPath,
                '-a',
                apiDestPath,
                '-g',
                getGenType()
        ]

        if (codeSamplesJava != null) {
            javaArgs.add('--codeSamplesJava')
            javaArgs.add(codeSamplesJava)
        }

        def execOptions = {
            description "Generate API for ${language}"
            main = mainClassName
            classpath = project.sourceSets.main.runtimeClasspath
            args = javaArgs
        }

        project.javaexec(execOptions)
    }

    String getGenType() {
        return 'API';
    }
}
